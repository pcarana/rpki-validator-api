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

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.ValidationRunDAO;

public class ValidationRunCleanupService {

	private static final Logger logger = Logger.getLogger(ValidationRunCleanupService.class.getName());

	private static ValidationRunDAO validationRuns;

	private static Duration cleanupGraceDuration;

	private ValidationRunCleanupService() {
	}

	public static long cleanupValidationRuns() {
		cleanupGraceDuration = Duration.parse(ApiConfiguration.getValidationRunCleanupInterval());
		validationRuns = DataAccessService.getValidationRunDAO();
		if (validationRuns == null) {
			logger.log(Level.SEVERE,
					"There was at least one necesary DAO whose implementation couldn't be found, exiting validation run cleanup");
			return -1L;
		}
		// Delete all validation runs older than `cleanupGraceDuration` that have a
		// later validation run.
		Instant completedBefore = Instant.now().minus(cleanupGraceDuration);
		long removedCount = -1L;
		try {
			removedCount = validationRuns.removeOldValidationRuns(completedBefore);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "There was an error deleting the old validation runs, exiting cleanup", e);
			return -1L;
		}
		logger.info("Old validation runs removed " + removedCount);
		return removedCount;
	}
}
