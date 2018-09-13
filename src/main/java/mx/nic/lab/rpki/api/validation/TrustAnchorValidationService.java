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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.io.Files;

import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.api.util.RsyncUtils;
import mx.nic.lab.rpki.api.util.TrustAnchorExtractorException;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.ErrorCodes;
import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.exception.ValidationErrorType;
import mx.nic.lab.rpki.db.exception.ValidationException;
import mx.nic.lab.rpki.db.pojo.RpkiObject;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.ValidationCheck;
import mx.nic.lab.rpki.db.pojo.ValidationRun;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.TalDAO;
import mx.nic.lab.rpki.db.spi.ValidationRunDAO;
import net.ripe.rpki.commons.crypto.CertificateRepositoryObject;
import net.ripe.rpki.commons.crypto.util.CertificateRepositoryObjectFactory;
import net.ripe.rpki.commons.crypto.x509cert.X509CertificateUtil;
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificate;
import net.ripe.rpki.commons.rsync.CommandExecutionException;
import net.ripe.rpki.commons.validation.ValidationResult;
import net.ripe.rpki.commons.validation.objectvalidators.CertificateRepositoryObjectValidationContext;

public class TrustAnchorValidationService {

	private static final Logger logger = Logger.getLogger(TrustAnchorValidationService.class.getName());

	private static TalDAO trustAnchorRepository;
	private static ValidationRunDAO validationRunRepository;
	private static File localRsyncStorageDirectory;

	private TrustAnchorValidationService() {
		// No code
	}

	/**
	 * Init what's required to run this service
	 * 
	 * @throws InitializationException
	 */
	public static void init() throws InitializationException {
		localRsyncStorageDirectory = new File(ApiConfiguration.getDownloadedRepositoriesLocation());
		trustAnchorRepository = DataAccessService.getTalDAO();
		validationRunRepository = DataAccessService.getValidationRunDAO();
		if (trustAnchorRepository == null || validationRunRepository == null) {
			String message = "There was at least one necesary DAO whose implementation couldn't be found, " + "exiting "
					+ TrustAnchorValidationService.class.getName();
			logger.log(Level.SEVERE, message);
			throw new InitializationException(message);
		}
	}

	public static void addTalFromFile(File talFile) throws InitializationException {
		if (!talFile.isFile() && !talFile.getName().endsWith(".tal")) {
			logger.log(Level.WARNING, talFile + " isn't a file nor has the extension .tal, it will be ommitted");
			return;
		}
		Tal newTal = null;
		try {
			newTal = Util.loadTalfromFile(talFile);
			newTal.setId(trustAnchorRepository.create(newTal));
		} catch (TrustAnchorExtractorException e) {
			throw new InitializationException("Error loading tal from file " + talFile, e);
		} catch (ApiDataAccessException e) {
			if (e instanceof ValidationException) {
				ValidationException ve = (ValidationException) e;
				// If the object already exists, ignore the error, otherwise throw it up
				// The error is at the first element
				if (ve.getValidationErrors().get(0).getErrorType() == ValidationErrorType.OBJECT_EXISTS) {
					logger.log(Level.INFO,
							"The object " + newTal.toString() + " already exists, keep processing the others");
					return;
				}
			}
			throw new InitializationException("Error creating TAL " + newTal, e);
		}
		// Now get the certificate and validate it
		URI cerUri = URI.create(newTal.getTalUris().get(0).getLocation());
		try {
			File targetFile = RsyncUtils.localFileFromRsyncUri(localRsyncStorageDirectory, cerUri);
			long trustAnchorCertificateSize = targetFile.length();

			if (trustAnchorCertificateSize < RpkiObject.MIN_SIZE || trustAnchorCertificateSize > RpkiObject.MAX_SIZE) {
				throw new InitializationException("The TAL certificate at " + cerUri.toASCIIString()
						+ " has an invalid size of " + String.valueOf(trustAnchorCertificateSize) + " bytes");
			}
			ValidationResult validationResult = ValidationResult.withLocation(cerUri);
			X509ResourceCertificate certificate = parseCertificate(newTal, targetFile, validationResult);
			if (!validationResult.hasFailureForCurrentLocation()) {
				// validity time?
				int comparedSerial = newTal.getCertificate() == null ? 1
						: newTal.getCertificate().getSerialNumber().compareTo(certificate.getSerialNumber());
				validationResult.warnIfTrue(comparedSerial < 0, "repository.object.is.older.than.previous.object",
						cerUri.toASCIIString());
				if (comparedSerial > 0) {
					newTal.setCertificate(certificate);
					try {
						trustAnchorRepository.updateLoadedCertificate(newTal);
					} catch (ApiDataAccessException e) {
						throw new InitializationException(
								"There was an error updating the TAL certificate of " + newTal.toString(), e);
					}
				}
			}
			if (validationResult.hasFailureForCurrentLocation()) {
				throw new InitializationException("Errors found validating the TAL " + newTal.getName()
						+ " certificate:" + validationResult.toString());
			}

			// Get the related repository
			CertificateRepositoryObjectValidationContext context = new CertificateRepositoryObjectValidationContext(
					cerUri, certificate);
			CertificateTreeValidationService.loadOrCreateRepository(context.getRepositoryURI(), newTal);
		} catch (IOException e) {
			throw new InitializationException("Error loading the TAL " + newTal.getName() + " certificate", e);
		}
	}

	public static void validate(long trustAnchorId) {
		Tal trustAnchor;
		try {
			trustAnchor = trustAnchorRepository.getById(trustAnchorId);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "There was an error fetching the TAL " + trustAnchorId, e);
			return;
		}

		logger.info("trust anchor " + trustAnchor.getName() + " located at " + trustAnchor.getTalUris()
				+ " with subject public key info " + trustAnchor.getPublicKey());

		ValidationRun validationRun = new ValidationRun(ValidationRun.Type.TRUST_ANCHOR, trustAnchor);
		try {
			MasterScheduler.triggerCertificateTreeValidation(trustAnchor);
		} catch (CommandExecutionException e) {
			logger.log(Level.WARNING, "validation run for trust anchor " + trustAnchor + " failed", e);
			validationRun.addCheck(new ValidationCheck(validationRun, validationRun.getTalCertificateURI(),
					ValidationCheck.Status.ERROR, ErrorCodes.UNHANDLED_EXCEPTION, e.toString()));
			validationRun.setFailed();
		}
		try {
			validationRunRepository.create(validationRun);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "There was an error persisting the TAL validation run " + validationRun.toString(),
					e);
		}

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
