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

import static java.util.stream.Collectors.toList;
import static net.ripe.rpki.commons.validation.ValidationString.VALIDATOR_CRL_FOUND;
import static net.ripe.rpki.commons.validation.ValidationString.VALIDATOR_MANIFEST_CONTAINS_ONE_CRL_ENTRY;
import static net.ripe.rpki.commons.validation.ValidationString.VALIDATOR_MANIFEST_ENTRY_FOUND;
import static net.ripe.rpki.commons.validation.ValidationString.VALIDATOR_MANIFEST_ENTRY_HASH_MATCHES;
import static net.ripe.rpki.commons.validation.ValidationString.VALIDATOR_RPKI_REPOSITORY_PENDING;
import static net.ripe.rpki.commons.validation.ValidationString.VALIDATOR_TRUST_ANCHOR_CERTIFICATE_AVAILABLE;
import static net.ripe.rpki.commons.validation.ValidationString.VALIDATOR_TRUST_ANCHOR_CERTIFICATE_RRDP_NOTIFY_URI_OR_REPOSITORY_URI_PRESENT;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.base.Objects;

import mx.nic.lab.rpki.api.util.RsyncUtils;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.ErrorCodes;
import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.pojo.RpkiObject;
import mx.nic.lab.rpki.db.pojo.RpkiRepository;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.ValidationRun;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.RpkiObjectDAO;
import mx.nic.lab.rpki.db.spi.RpkiRepositoryDAO;
import mx.nic.lab.rpki.db.spi.TalDAO;
import mx.nic.lab.rpki.db.spi.ValidationRunDAO;
import net.ripe.rpki.commons.crypto.CertificateRepositoryObject;
import net.ripe.rpki.commons.crypto.cms.manifest.ManifestCms;
import net.ripe.rpki.commons.crypto.crl.X509Crl;
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificate;
import net.ripe.rpki.commons.util.RepositoryObjectType;
import net.ripe.rpki.commons.validation.ValidationLocation;
import net.ripe.rpki.commons.validation.ValidationOptions;
import net.ripe.rpki.commons.validation.ValidationResult;
import net.ripe.rpki.commons.validation.ValidationString;
import net.ripe.rpki.commons.validation.objectvalidators.CertificateRepositoryObjectValidationContext;

public class CertificateTreeValidationService {

	private static final Logger logger = Logger.getLogger(CertificateTreeValidationService.class.getName());

	private static final ValidationOptions VALIDATION_OPTIONS = new ValidationOptions();

	private static RpkiRepositoryDAO rpkiRepositories;
	private static RpkiObjectDAO rpkiObjects;
	private static TalDAO trustAnchors;
	private static ValidationRunDAO validationRuns;

	private CertificateTreeValidationService() {
		// No code
	}

	/**
	 * Init what's required to run this service
	 * 
	 * @throws InitializationException
	 */
	public static void init() throws InitializationException {
		rpkiRepositories = DataAccessService.getRpkiRepositoryDAO();
		rpkiObjects = DataAccessService.getRpkiObjectDAO();
		trustAnchors = DataAccessService.getTalDAO();
		validationRuns = DataAccessService.getValidationRunDAO();
		if (rpkiRepositories == null || rpkiObjects == null || trustAnchors == null || validationRuns == null) {
			String message = "There was at least one necesary DAO whose implementation couldn't be found, " + "exiting "
					+ CertificateTreeValidationService.class.getName();
			throw new InitializationException(message);
		}
	}

	public static void validate(long trustAnchorId) {
		Map<URI, RpkiRepository> registeredRepositories = new HashMap<>();

		Tal trustAnchor;
		try {
			trustAnchor = trustAnchors.getById(trustAnchorId);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			return;
		}
		if (trustAnchor == null) {
			logger.warning("The TAL with ID " + trustAnchorId + " doesn't exist, exiting from validation");
			return;
		}
		logger.info("starting tree validation for " + trustAnchor.getName() + " with id " + trustAnchorId);

		ValidationRun validationRun = new ValidationRun(ValidationRun.Type.CERTIFICATE_TREE, trustAnchor);
		String trustAnchorLocation = trustAnchor.getTalUris().get(0).getLocation();
		ValidationResult validationResult = ValidationResult.withLocation(trustAnchorLocation);
		try {
			X509ResourceCertificate certificate = trustAnchor.getCertificate();
			validationResult.rejectIfNull(certificate, VALIDATOR_TRUST_ANCHOR_CERTIFICATE_AVAILABLE);
			if (certificate == null) {
				return;
			}

			CertificateRepositoryObjectValidationContext context = new CertificateRepositoryObjectValidationContext(
					URI.create(trustAnchorLocation), certificate);

			certificate.validate(trustAnchorLocation, context, null, null, VALIDATION_OPTIONS, validationResult);
			if (validationResult.hasFailureForCurrentLocation()) {
				return;
			}

			URI locationUri = Objects.firstNonNull(certificate.getRrdpNotifyUri(), certificate.getRepositoryUri());
			validationResult.warnIfNull(locationUri,
					VALIDATOR_TRUST_ANCHOR_CERTIFICATE_RRDP_NOTIFY_URI_OR_REPOSITORY_URI_PRESENT);
			if (locationUri == null) {
				return;
			}

			validationRun.getValidatedObjects().addAll(
					validateCertificateAuthority(trustAnchor, registeredRepositories, context, validationResult));

			// @pcarana FIXME DELETE ME IF NEEDED:
			// net.ripe.rpki.validator3.domain.ValidatedRpkiObjects retrieved
			// the validated objects and was filtering them just to consult them (apparently
			// this
			// isn't necessary for now, this comment is only a reference so delete it if
			// that class
			// [ValidatedRpkiObjects] isn't needed)
		} finally {
			validationRun.completeWith(validationResult);
			logger.info("tree validation " + validationRun.getStatus() + " for " + trustAnchor);
		}
		try {
			validationRuns.create(validationRun);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "There was an error persisting the validation run", e);
		}
	}

	private static List<RpkiObject> validateCertificateAuthority(Tal trustAnchor,
			Map<URI, RpkiRepository> registeredRepositories, CertificateRepositoryObjectValidationContext context,
			ValidationResult validationResult) {
		final List<RpkiObject> validatedObjects = new ArrayList<>();

		ValidationLocation certificateLocation = validationResult.getCurrentLocation();
		ValidationResult temporary = ValidationResult.withLocation(certificateLocation);
		try {
			RpkiRepository rpkiRepository = registerRepository(trustAnchor, registeredRepositories, context);
			if (rpkiRepository == null) {
				return validatedObjects;
			}

			temporary.warnIfTrue(rpkiRepository.isPending(), VALIDATOR_RPKI_REPOSITORY_PENDING,
					rpkiRepository.getLocationUri());
			if (rpkiRepository.isPending()) {
				return validatedObjects;
			}

			X509ResourceCertificate certificate = context.getCertificate();
			URI manifestUri = certificate.getManifestUri();
			temporary.setLocation(new ValidationLocation(manifestUri));

			Optional<RpkiObject> manifestObject = rpkiObjects
					.findLatestByTypeAndAuthorityKeyIdentifier(RpkiObject.Type.MFT, context.getSubjectKeyIdentifier());
			if (!manifestObject.isPresent()) {
				if (rpkiRepository.getStatus() == RpkiRepository.Status.FAILED) {
					temporary.error(ValidationString.VALIDATOR_NO_MANIFEST_REPOSITORY_FAILED,
							rpkiRepository.getLocationUri());
				} else {
					temporary.error(ValidationString.VALIDATOR_NO_LOCAL_MANIFEST_NO_MANIFEST_IN_REPOSITORY,
							rpkiRepository.getLocationUri());
				}
			}

			Optional<ManifestCms> maybeManifest = manifestObject.flatMap(x -> {
				try {
					return rpkiObjects.findCertificateRepositoryObject(x.getId(), ManifestCms.class, temporary);
				} catch (ApiDataAccessException e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
					temporary.error(ValidationString.VALIDATOR_OBJECT_PROCESSING_EXCEPTION);
					return Optional.empty();
				}
			});

			temporary.rejectIfTrue(
					manifestObject.isPresent() && rpkiRepository.getStatus() == RpkiRepository.Status.FAILED
							&& maybeManifest.isPresent() && maybeManifest.get().isPastValidityTime(),
					ValidationString.VALIDATOR_OLD_LOCAL_MANIFEST_REPOSITORY_FAILED, rpkiRepository.getLocationUri());

			if (temporary.hasFailureForCurrentLocation()) {
				return validatedObjects;
			}

			ManifestCms manifest = maybeManifest.get();
			List<Map.Entry<String, byte[]>> crlEntries = manifest.getFiles().entrySet().stream()
					.filter((entry) -> RepositoryObjectType.parse(entry.getKey()) == RepositoryObjectType.Crl)
					.collect(toList());
			temporary.rejectIfFalse(crlEntries.size() == 1, VALIDATOR_MANIFEST_CONTAINS_ONE_CRL_ENTRY,
					String.valueOf(crlEntries.size()));
			if (temporary.hasFailureForCurrentLocation()) {
				return validatedObjects;
			}

			Map.Entry<String, byte[]> crlEntry = crlEntries.get(0);
			URI crlUri = manifestUri.resolve(crlEntry.getKey());

			Optional<RpkiObject> crlObject = rpkiObjects.findBySha256(crlEntry.getValue());
			temporary.rejectIfFalse(crlObject.isPresent(), VALIDATOR_CRL_FOUND, crlUri.toASCIIString());
			if (temporary.hasFailureForCurrentLocation()) {
				return validatedObjects;
			}

			temporary.setLocation(new ValidationLocation(crlUri));
			Optional<X509Crl> crl = crlObject.flatMap(x -> {
				try {
					return rpkiObjects.findCertificateRepositoryObject(x.getId(), X509Crl.class, temporary);
				} catch (ApiDataAccessException e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
					temporary.error(ValidationString.VALIDATOR_OBJECT_PROCESSING_EXCEPTION);
					return Optional.empty();
				}
			});
			if (temporary.hasFailureForCurrentLocation()) {
				return validatedObjects;
			}

			crl.get().validate(crlUri.toASCIIString(), context, null, VALIDATION_OPTIONS, temporary);
			if (temporary.hasFailureForCurrentLocation()) {
				return validatedObjects;
			}

			temporary.setLocation(new ValidationLocation(manifestUri));
			manifest.validate(manifestUri.toASCIIString(), context, crl.get(), manifest.getCrlUri(), VALIDATION_OPTIONS,
					temporary);
			if (temporary.hasFailureForCurrentLocation()) {
				return validatedObjects;
			}
			validatedObjects.add(manifestObject.get());

			Map<URI, RpkiObject> manifestEntries = retrieveManifestEntries(manifest, manifestUri, temporary);

			manifestEntries.forEach((location, obj) -> {
				temporary.setLocation(new ValidationLocation(location));

				Optional<CertificateRepositoryObject> maybeCertificateRepositoryObject = null;
				try {
					maybeCertificateRepositoryObject = rpkiObjects.findCertificateRepositoryObject(obj.getId(),
							CertificateRepositoryObject.class, temporary);
				} catch (ApiDataAccessException e) {
					// TODO @pcarana Test behavior at this scenario
					logger.log(Level.SEVERE, e.getMessage(), e);
					temporary.error(ValidationString.VALIDATOR_OBJECT_PROCESSING_EXCEPTION);
				}
				if (temporary.hasFailureForCurrentLocation()) {
					return;
				}

				maybeCertificateRepositoryObject.ifPresent(certificateRepositoryObject -> {
					certificateRepositoryObject.validate(location.toASCIIString(), context, crl.get(), crlUri,
							VALIDATION_OPTIONS, temporary);

					if (!temporary.hasFailureForCurrentLocation()) {
						validatedObjects.add(obj);
					}

					if (certificateRepositoryObject instanceof X509ResourceCertificate
							&& ((X509ResourceCertificate) certificateRepositoryObject).isCa()
							&& !temporary.hasFailureForCurrentLocation()) {

						CertificateRepositoryObjectValidationContext childContext = context.createChildContext(location,
								(X509ResourceCertificate) certificateRepositoryObject);
						validatedObjects.addAll(validateCertificateAuthority(trustAnchor, registeredRepositories,
								childContext, temporary));
					}
				});
			});
		} catch (Exception e) {
			logger.log(Level.INFO, e.getMessage(), e);
			validationResult.error(ErrorCodes.UNHANDLED_EXCEPTION, e.toString(), ExceptionUtils.getStackTrace(e));
		} finally {
			validationResult.addAll(temporary);
		}

		return validatedObjects;
	}

	private static RpkiRepository registerRepository(Tal trustAnchor, Map<URI, RpkiRepository> registeredRepositories,
			CertificateRepositoryObjectValidationContext context) {
		if (context.getRpkiNotifyURI() != null) {
			return registeredRepositories.computeIfAbsent(context.getRpkiNotifyURI(), (uri) -> {
				return loadOrCreateRepository(uri, trustAnchor);
			});
		} else {
			return registeredRepositories.computeIfAbsent(context.getRepositoryURI(), (uri) -> {
				return loadOrCreateRepository(uri, trustAnchor);
			});
		}
	}

	private static Map<URI, RpkiObject> retrieveManifestEntries(ManifestCms manifest, URI manifestUri,
			ValidationResult validationResult) {
		Map<URI, RpkiObject> result = new LinkedHashMap<>();
		for (Map.Entry<String, byte[]> entry : manifest.getFiles().entrySet()) {
			URI location = manifestUri.resolve(entry.getKey());
			validationResult.setLocation(new ValidationLocation(location));

			Optional<RpkiObject> object = null;
			try {
				object = rpkiObjects.findBySha256(entry.getValue());
			} catch (ApiDataAccessException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				validationResult.error(ValidationString.VALIDATOR_OBJECT_PROCESSING_EXCEPTION);
				continue;
			}
			validationResult.rejectIfFalse(object.isPresent(), VALIDATOR_MANIFEST_ENTRY_FOUND,
					manifestUri.toASCIIString());

			object.ifPresent(obj -> {
				boolean hashMatches = Arrays.equals(obj.getSha256(), entry.getValue());
				validationResult.rejectIfFalse(hashMatches, VALIDATOR_MANIFEST_ENTRY_HASH_MATCHES, entry.getKey());
				if (!hashMatches) {
					return;
				}

				result.put(location, obj);
			});
		}
		return result;
	}

	public static RpkiRepository loadOrCreateRepository(URI uri, Tal trustAnchor) {
		String uriString = uri.toASCIIString();
		RpkiRepositoryDAO rpkiRepositories = DataAccessService.getRpkiRepositoryDAO();
		RpkiRepository rpkiRepository;
		try {
			rpkiRepository = rpkiRepositories.findByURI(uriString).orElseGet(() -> {
				RpkiRepository newRepository = new RpkiRepository(trustAnchor, uri.toASCIIString());
				newRepository.setDownloaded();
				try {
					Long id = rpkiRepositories.create(newRepository);
					newRepository.setId(id);
				} catch (ApiDataAccessException e) {
					logger.log(Level.SEVERE, "Error persisting repository " + newRepository.toString(), e);
					return null;
				}
				return newRepository;
			});
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "Error getting repository with URI" + uriString, e);
			return null;
		}

		RpkiRepository parentRepository = findRsyncParentRepository(uri);
		if (parentRepository != null) {
			rpkiRepository.setParentRepository(parentRepository);
			// TODO Is it really necessary?
			// if (parentRepository.isDownloaded()) {
			// rpkiRepository.setDownloaded(parentRepository.getLastDownloadedAt());
			// }
			try {
				rpkiRepositories.updateParentRepository(rpkiRepository);
			} catch (ApiDataAccessException e) {
				logger.log(Level.SEVERE, "Error updating parentRepository to " + rpkiRepository.toString(), e);
			}
		}

		return rpkiRepository;
	}

	private static RpkiRepository findRsyncParentRepository(URI uri) {
		for (URI parentURI : RsyncUtils.generateCandidateParentUris(uri)) {
			try {
				Optional<RpkiRepository> optional = rpkiRepositories.findByURI(parentURI.normalize().toASCIIString());
				if (optional.isPresent()) {
					return optional.get();
				}
			} catch (ApiDataAccessException e) {
				logger.log(Level.SEVERE, "Error searching RpkiRepositories by URI", e);
				return null;
			}
		}
		return null;
	}
}
