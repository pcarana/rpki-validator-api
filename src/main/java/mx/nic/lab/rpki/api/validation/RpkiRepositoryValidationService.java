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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;

import mx.nic.lab.rpki.api.util.Hex;
import mx.nic.lab.rpki.api.util.RsyncUtils;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.ErrorCodes;
import mx.nic.lab.rpki.db.pojo.RpkiObject;
import mx.nic.lab.rpki.db.pojo.RpkiRepository;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.ValidationRun;
import mx.nic.lab.rpki.db.util.Sha256;
import net.ripe.rpki.commons.crypto.CertificateRepositoryObject;
import net.ripe.rpki.commons.crypto.util.CertificateRepositoryObjectFactory;
import net.ripe.rpki.commons.validation.ValidationLocation;
import net.ripe.rpki.commons.validation.ValidationResult;

public class RpkiRepositoryValidationService extends ValidationService {

	private static final Logger logger = Logger.getLogger(RpkiRepositoryValidationService.class.getName());

	private RpkiRepositoryValidationService() {
		// No code
	}

	public static void validateRsyncRepositories() {
		Instant cutoffTime = Instant.now().minus(getRsyncRepositoryDownloadInterval());
		logger.info("updating all rsync repositories that have not been downloaded since " + cutoffTime);
		Set<Tal> affectedTrustAnchors = new HashSet<>();
		ValidationRun validationRun = new ValidationRun(ValidationRun.Type.RPKI_REPOSITORY);
		// Create validation run with initial status (running)
		try {
			validationRun.setId(getValidationRunDAO().create(validationRun));
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "Error persisting validation run " + validationRun.toString(), e);
			return;
		}
		final Map<String, RpkiObject> objectsBySha256 = new HashMap<>();
		final Map<URI, RpkiRepository> fetchedLocations = new HashMap<>();
		Stream<RpkiRepository> repositories = null;
		try {
			repositories = getRpkiRepositoryDAO().findRsyncRepositories();
		} catch (ApiDataAccessException e) {
			logger.log(Level.WARNING, "There was an error getting the repositories, exiting repository validation", e);
			return;
		}
		ValidationResult results = repositories
				/*
				 * TODO Maybe this wont be needed .filter((repository) -> { /* boolean
				 * needsUpdate = repository.isPending() || repository.getLastDownloadedAt() ==
				 * null || repository.getLastDownloadedAt().isBefore(cutoffTime); if
				 * (!needsUpdate) {
				 * 
				 * //fetchedLocations.put(URI.create(repository.getLocationUri()), repository);
				 * return true; /* } return needsUpdate;
				 * 
				 * })
				 */.map((repository) -> processRsyncRepository(affectedTrustAnchors, validationRun, fetchedLocations,
						objectsBySha256, repository))
				.collect(() -> ValidationResult.withLocation("placeholder"), ValidationResult::addAll,
						ValidationResult::addAll);

		validationRun.completeWith(results);
		affectedTrustAnchors.forEach((trustAnchor) -> {
			MasterScheduler.triggerCertificateTreeValidation(trustAnchor.getId());
		});
		try {
			getValidationRunDAO().completeValidation(validationRun);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "There was an error updating the RPKI repository validation run", e);
		}

		// Log Info about the validation
		logger.log(Level.INFO, "Finish RPKI Repository validationRun " + validationRun.toString());
	}

	private static ValidationResult processRsyncRepository(Set<Tal> affectedTrustAnchors, ValidationRun validationRun,
			Map<URI, RpkiRepository> fetchedLocations, Map<String, RpkiObject> objectsBySha256,
			RpkiRepository repository) {

		final ValidationResult validationResult = ValidationResult
				.withLocation(URI.create(repository.getLocationUri()));

		validationRun.addRpkiRepository(repository);

		try {
			File targetDirectory = RsyncUtils.localFileFromRsyncUri(getLocalRsyncStorageDirectory(),
					URI.create(repository.getLocationUri()));
			RpkiRepository parentRepository = findDownloadedParentRepository(fetchedLocations, repository);
			if (parentRepository == null) {
				logger.log(Level.INFO, "Storing object downloaded for " + repository.getLocationUri());
				storeObjects(targetDirectory, validationRun, validationResult, objectsBySha256, repository);
			}
		} catch (IOException e) {
			validationResult.error(ErrorCodes.RSYNC_REPOSITORY_IO, e.toString(), ExceptionUtils.getStackTrace(e));
		}

		affectedTrustAnchors.addAll(repository.getTrustAnchors());
		fetchedLocations.put(URI.create(repository.getLocationUri()), repository);

		return validationResult;
	}

	private static RpkiRepository findDownloadedParentRepository(Map<URI, RpkiRepository> fetchedLocations,
			RpkiRepository repository) {
		URI location = URI.create(repository.getLocationUri());
		for (URI parentLocation : RsyncUtils.generateCandidateParentUris(location)) {
			RpkiRepository parentRepository = fetchedLocations.get(parentLocation);
			if (parentRepository != null) {
				repository.setParentRepository(parentRepository);
				logger.log(Level.INFO, "Already fetched " + repository.getLocationUri() + " as part of "
						+ parentRepository.getLocationUri() + ", skipping");
				return parentRepository;
			}
		}
		return null;
	}

	private static void storeObjects(File targetDirectory, ValidationRun validationRun,
			ValidationResult validationResult, Map<String, RpkiObject> objectsBySha256, RpkiRepository repository)
			throws IOException {
		Files.walkFileTree(targetDirectory.toPath(), new SimpleFileVisitor<Path>() {
			private URI currentLocation = URI.create(repository.getLocationUri());
			private Long rpkiRepositoryId = repository.getId();

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.equals(targetDirectory.toPath())) {
					return FileVisitResult.CONTINUE;
				}

				logger.log(Level.FINE, "visiting " + dir);

				super.preVisitDirectory(dir, attrs);
				currentLocation = currentLocation.resolve(dir.getFileName().toString() + "/");
				validationResult.setLocation(new ValidationLocation(currentLocation));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (dir.equals(targetDirectory.toPath())) {
					return FileVisitResult.CONTINUE;
				}

				logger.log(Level.FINE, "leaving " + dir);
				super.postVisitDirectory(dir, exc);
				currentLocation = currentLocation.resolve("..").normalize();
				validationResult.setLocation(new ValidationLocation(currentLocation));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				super.visitFile(file, attrs);
				logger.log(Level.FINE, "at file " + file + " in location " + currentLocation);
				validationResult
						.setLocation(new ValidationLocation(currentLocation.resolve(file.getFileName().toString())));

				byte[] content = Files.readAllBytes(file);
				byte[] sha256 = Sha256.hash(content);
				String hexSha256 = Hex.format(sha256);

				objectsBySha256.compute(hexSha256, (key, existing) -> {
					if (existing == null) {
						try {
							existing = getRpkiObjectDAO().findBySha256(sha256).orElse(null);
						} catch (ApiDataAccessException e) {
							logger.log(Level.WARNING,
									"There was an error fetching the object by its sha256 " + hexSha256, e);
							return null;
						}
					}
					if (existing != null) {
						existing.addLocation(validationResult.getCurrentLocation().getName());
						if (existing.addRpkiRepository(rpkiRepositoryId)) {
							try {
								getRpkiObjectDAO().addRpkiRepository(existing, rpkiRepositoryId);
							} catch (ApiDataAccessException e) {
								// Return the existing object
								logger.log(Level.WARNING, "There was an updating the object " + existing, e);
							}
						}
						return existing;
					} else {
						CertificateRepositoryObject obj = CertificateRepositoryObjectFactory
								.createCertificateRepositoryObject(content, validationResult);
						validationRun.addChecks(validationResult);

						if (validationResult.hasFailureForCurrentLocation()) {
							logger.log(Level.INFO,
									"parsing " + file + " failed: " + validationResult.getFailuresForCurrentLocation());
							return null;
						}

						RpkiObject object = new RpkiObject(validationResult.getCurrentLocation().getName(),
								rpkiRepositoryId, obj);
						try {
							object.setId(getRpkiObjectDAO().create(object));
						} catch (ApiDataAccessException e) {
							logger.log(Level.WARNING, "There was an error storing the object " + object.toString(), e);
							return null;
						}
						logger.log(Level.FINE, "added to database " + object);
						return object;
					}
				});

				return FileVisitResult.CONTINUE;
			}
		});
	}
}
