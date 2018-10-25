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

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.RpkiObject;
import mx.nic.lab.rpki.db.pojo.Tal;
import net.ripe.rpki.commons.crypto.cms.manifest.ManifestCms;
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificate;
import net.ripe.rpki.commons.validation.ValidationResult;

public class RpkiObjectCleanupService extends ValidationService {

	private static final Logger logger = Logger.getLogger(RpkiObjectCleanupService.class.getName());

	private RpkiObjectCleanupService() {
		// No code
	}

	/**
	 * Marks all RPKI objects that are reachable from a trust anchor by following
	 * the entries in the manifests. Objects that are no longer reachable will be
	 * deleted after a configurable grace duration.
	 */
	public static long cleanupRpkiObjects() {
		Instant now = Instant.now();
		Set<RpkiObject> reachedObjects = new HashSet<>();
		try {
			for (Tal trustAnchor : getTalDAO().getAll(null).getResults()) {
				logger.info("tracing objects for trust anchor " + trustAnchor.getName() + " with id "
						+ trustAnchor.getId());
				X509ResourceCertificate resourceCertificate = trustAnchor.getCertificate();
				if (resourceCertificate != null) {
					traceCertificateAuthority(now, resourceCertificate, reachedObjects);
				}
			}
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "There was an error getting the TALs, exiting cleanup", e);
			return -1L;
		}
		try {
			getRpkiObjectDAO().updateReachedObjects(reachedObjects);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "There was an error updating the reached objects, exiting cleanup", e);
			return -1L;
		}

		return deleteUnreachableObjects(now);
	}

	private static long deleteUnreachableObjects(Instant now) {
		Instant unreachableSince = now.minus(getRpkiObjectCleanupGrace());
		long count = 0;
		try {
			count = getRpkiObjectDAO().deleteUnreachableObjects(unreachableSince);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "There was an error deleting the unreachable objects, exiting cleanup", e);
			return -1L;
		}
		logger.info("Removed " + count + " RPKI objects that have not been marked reachable since " + unreachableSince);
		return count;
	}

	private static void traceCertificateAuthority(Instant now, X509ResourceCertificate resourceCertificate,
			Set<RpkiObject> reachedObjects) throws ApiDataAccessException {
		if (resourceCertificate == null || resourceCertificate.getManifestUri() == null) {
			return;
		}

		Optional<RpkiObject> maybeManifest = getRpkiObjectDAO().findLatestByTypeAndAuthorityKeyIdentifier(
				RpkiObject.Type.MFT, resourceCertificate.getSubjectKeyIdentifier());
		maybeManifest.ifPresent(manifest -> {
			markAndTraceObject(now, "manifest.mft", manifest, reachedObjects);
		});
	}

	private static void markAndTraceObject(Instant now, String name, RpkiObject rpkiObject,
			Set<RpkiObject> reachedObjects) {
		// Compare object instance identity to see if we've already visited
		// the `rpkiObject` in the current run.
		if (now == rpkiObject.getLastMarkedReachableAt()) {
			logger.log(Level.FINE, "object already marked, skipping " + rpkiObject);
		}

		rpkiObject.markReachable(now);
		reachedObjects.add(rpkiObject);
		switch (rpkiObject.getType()) {
		case MFT:
			traceManifest(now, name, rpkiObject, reachedObjects);
			break;
		case CER:
			traceCaCertificate(now, name, rpkiObject, reachedObjects);
			break;
		default:
			break;
		}
	}

	private static void traceManifest(Instant now, String name, RpkiObject manifest, Set<RpkiObject> reachedObjects) {
		try {
			getRpkiObjectDAO().findCertificateRepositoryObject(manifest.getId(), ManifestCms.class,
					ValidationResult.withLocation(name)).ifPresent(manifestCms -> {
						try {
							getRpkiObjectDAO().findObjectsInManifest(manifestCms).forEach((entry, rpkiObject) -> {
								markAndTraceObject(now, entry, rpkiObject, reachedObjects);
							});
						} catch (ApiDataAccessException e) {
							logger.log(Level.WARNING,
									"There was an getting the object from the manifest at location " + name, e);
						}
					});
		} catch (ApiDataAccessException e) {
			logger.log(Level.WARNING, "There was an error tracing the manifest for " + name, e);
		}
	}

	private static void traceCaCertificate(Instant now, String name, RpkiObject caCertificate,
			Set<RpkiObject> reachedObjects) {
		try {
			getRpkiObjectDAO().findCertificateRepositoryObject(caCertificate.getId(), X509ResourceCertificate.class,
					ValidationResult.withLocation(name)).ifPresent(certificate -> {
						if (certificate.isCa() && certificate.getManifestUri() != null) {
							try {
								traceCertificateAuthority(now, certificate, reachedObjects);
							} catch (ApiDataAccessException e) {
								logger.log(Level.WARNING, "There was an error tracing the cert authority for " + name,
										e);
							}
						}
					});
		} catch (ApiDataAccessException e) {
			logger.log(Level.WARNING, "There was an error tracing the CA cert for " + name, e);
		}
	}

}
