/**
 * The BSD License
 *
 * Copyright (c) 2010-2018 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package mx.nic.lab.rpki.api.validation;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.io.Files;

import mx.nic.lab.rpki.api.util.RsyncUtils;
import mx.nic.lab.rpki.api.util.TrustAnchorExtractorException;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.ErrorCodes;
import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.pojo.RpkiObject;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.ValidationCheck;
import mx.nic.lab.rpki.db.pojo.ValidationRun;
import net.ripe.rpki.commons.crypto.CertificateRepositoryObject;
import net.ripe.rpki.commons.crypto.util.CertificateRepositoryObjectFactory;
import net.ripe.rpki.commons.crypto.x509cert.X509CertificateUtil;
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificate;
import net.ripe.rpki.commons.validation.ValidationLocation;
import net.ripe.rpki.commons.validation.ValidationResult;
import net.ripe.rpki.commons.validation.objectvalidators.CertificateRepositoryObjectValidationContext;

public class TrustAnchorValidationService extends ValidationService {

	private static final Logger logger = Logger.getLogger(TrustAnchorValidationService.class.getName());

	private TrustAnchorValidationService() {
		// No code
	}

	/**
	 * Run the first validation of the TALs
	 * 
	 * @throws InitializationException
	 */
	public static void runFirstValidation() throws InitializationException {
		ValidationResult validationResult = loadAndValidateTals(false);
		if (validationResult.hasFailures()) {
			throw new InitializationException(
					"Initial validation of TALs threw some errors: " + validationResult.toString());
		}
	}

	/**
	 * Run the TAL validation, this must used by recurrent calls, when the Jobs are
	 * already configured
	 */
	public static void validate() {
		ValidationRun validationRun = new ValidationRun(ValidationRun.Type.TRUST_ANCHOR);
		// Create validation run with initial status (running)
		try {
			validationRun.setId(getValidationRunDAO().create(validationRun));
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "Error persisting validation run " + validationRun.toString(), e);
			return;
		}
		ValidationResult validationResult = null;
		try {
			validationResult = loadAndValidateTals(true);
		} catch (InitializationException e) {
			logger.log(Level.WARNING, "Trust anchor validation failed", e);
			validationRun.addCheck(new ValidationCheck(validationRun, validationRun.getTalCertificateURI(),
					ValidationCheck.Status.ERROR, ErrorCodes.UNHANDLED_EXCEPTION, e.toString()));
			validationRun.setFailed();
		}
		validationRun.completeWith(validationResult);
		try {
			getValidationRunDAO().completeValidation(validationRun);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "There was an error updating the TAL validation run " + validationRun.toString(),
					e);
		}

	}

	/**
	 * Loads and validate the configured TALs from the source directory, also check
	 * if the files and the DB are synchronized to take actions (add, delete, update
	 * the TAL).
	 * 
	 * @param jobsLoaded
	 *            To indicate if the main Jobs are allready configured, its expected
	 *            that when the application is loaded this flag has a
	 *            <code>false</code> value
	 * @return {@link ValidationResult} of the task
	 * @throws InitializationException
	 */
	private static ValidationResult loadAndValidateTals(boolean jobsLoaded) throws InitializationException {
		ValidationResult validationResult = ValidationResult.withLocation(getTalsLocation().getPath());
		File talsLocation = getTalsLocation();
		Map<Long, Tal> talsToDelete = new HashMap<>();
		try {
			List<Tal> tals = getTalDAO().getAll(null);
			tals.forEach((tal) -> talsToDelete.put(tal.getId(), tal));
		} catch (ApiDataAccessException e) {
			throw new InitializationException("Error getting existent tals data", e);
		}
		for (File talFile : talsLocation.listFiles()) {
			if (!talFile.isFile() && !talFile.getName().endsWith(".tal")) {
				logger.log(Level.WARNING, talFile + " isn't a file nor has the extension .tal, it will be ommitted");
				continue;
			}
			try {
				boolean created = false;
				Tal loadedTal = Util.loadTalfromFile(talFile);
				Tal foundTal = getTalDAO().getExistentTal(loadedTal);
				if (foundTal == null) {
					// Doesn't exists, create
					loadedTal.setId(getTalDAO().create(loadedTal));
					foundTal = loadedTal;
					created = true;
				}
				// If there were changes at the certificate, run validation
				boolean certUpdated = TrustAnchorValidationService.getAndValidateCertificate(foundTal,
						validationResult);
				if (created) {
					talsToDelete.put(foundTal.getId(), null);
					MasterScheduler.addTrustAnchorJob(loadedTal.getId());
					if (jobsLoaded) {
						MasterScheduler.triggerRpkiRepositoryValidation();
					}
				} else if (certUpdated && jobsLoaded) {
					MasterScheduler.triggerCertificateTreeValidation(foundTal.getId());
				}
				// Exists, do not delete it
				if (talsToDelete.containsKey(foundTal.getId())) {
					talsToDelete.put(foundTal.getId(), null);
				}
			} catch (TrustAnchorExtractorException e) {
				throw new InitializationException("Error loading tal from file " + talFile, e);
			} catch (ApiDataAccessException e) {
				throw new InitializationException("Error getting tal data from implementation " + talFile, e);
			}
		}
		for (Long talId : talsToDelete.keySet()) {
			if (talsToDelete.get(talId) != null) {
				if (jobsLoaded) {
					MasterScheduler.removeTrustAnchorJob(talId);
				}
				try {
					getTalDAO().delete(talsToDelete.get(talId));
				} catch (ApiDataAccessException e) {
					throw new InitializationException("Error deleting tal data from implementation", e);
				}
			}
		}
		return validationResult;
	}

	/**
	 * Get the certificate from the TAL and validate it, at the end return a boolean
	 * to know if the certificate was updated
	 * 
	 * @param tal
	 * @param validationResult
	 * @return
	 * @throws InitializationException
	 */
	private static boolean getAndValidateCertificate(Tal tal, ValidationResult validationResult) {
		boolean updated = false;
		URI cerUri = URI.create(tal.getTalUris().get(0).getLocation());
		validationResult = validationResult.setLocation(new ValidationLocation(cerUri));
		try {
			File targetFile = RsyncUtils.localFileFromRsyncUri(getLocalRsyncStorageDirectory(), cerUri);
			long trustAnchorCertificateSize = targetFile.length();

			if (trustAnchorCertificateSize < RpkiObject.MIN_SIZE) {
				validationResult.error(ErrorCodes.REPOSITORY_OBJECT_MINIMUM_SIZE, cerUri.toASCIIString(),
						String.valueOf(trustAnchorCertificateSize), String.valueOf(RpkiObject.MIN_SIZE));
			} else if (trustAnchorCertificateSize > RpkiObject.MAX_SIZE) {
				validationResult.error(ErrorCodes.REPOSITORY_OBJECT_MAXIMUM_SIZE, cerUri.toASCIIString(),
						String.valueOf(trustAnchorCertificateSize), String.valueOf(RpkiObject.MAX_SIZE));
			} else {
				X509ResourceCertificate certificate = parseCertificate(tal, targetFile, validationResult);
				if (!validationResult.hasFailureForCurrentLocation()) {
					// validity time?
					int comparedSerial = tal.getCertificate() == null ? 1
							: tal.getCertificate().getSerialNumber().compareTo(certificate.getSerialNumber());
					validationResult.warnIfTrue(comparedSerial < 0, "repository.object.is.older.than.previous.object",
							cerUri.toASCIIString());
					if (comparedSerial > 0) {
						tal.setCertificate(certificate);
						try {
							getTalDAO().updateLoadedCertificate(tal);
						} catch (ApiDataAccessException e) {
							validationResult.error(ErrorCodes.UNHANDLED_EXCEPTION, e.toString());
						}
						updated = true;
					}
				}

				// Get the related repository
				CertificateRepositoryObjectValidationContext context = new CertificateRepositoryObjectValidationContext(
						cerUri, certificate);
				CertificateTreeValidationService.loadOrCreateRepository(context.getRepositoryURI(), tal);
			}
		} catch (IOException e) {
			validationResult.error(ErrorCodes.UNHANDLED_EXCEPTION, e.toString());
		}
		return updated;
	}

	private static X509ResourceCertificate parseCertificate(Tal trustAnchor, File certificateFile,
			ValidationResult validationResult) throws IOException {
		CertificateRepositoryObject trustAnchorCertificate = CertificateRepositoryObjectFactory
				.createCertificateRepositoryObject(Files.toByteArray(certificateFile), validationResult);
		validationResult.rejectIfFalse(trustAnchorCertificate instanceof X509ResourceCertificate,
				ErrorCodes.REPOSITORY_OBJECT_IS_TRUST_ANCHOR_CERTIFICATE);
		if (validationResult.hasFailureForCurrentLocation()) {
			return null;
		}

		X509ResourceCertificate certificate = (X509ResourceCertificate) trustAnchorCertificate;

		String encodedSubjectPublicKeyInfo = X509CertificateUtil
				.getEncodedSubjectPublicKeyInfo(certificate.getCertificate());
		validationResult.rejectIfFalse(encodedSubjectPublicKeyInfo.equals(trustAnchor.getPublicKey()),
				"trust.anchor.subject.key.matches.locator");

		boolean signatureValid;
		try {
			certificate.getCertificate().verify(certificate.getPublicKey());
			signatureValid = true;
		} catch (GeneralSecurityException e) {
			signatureValid = false;
		}

		validationResult.rejectIfFalse(signatureValid, ErrorCodes.TRUST_ANCHOR_SIGNATURE, trustAnchor.getPublicKey());

		return certificate;
	}
}
