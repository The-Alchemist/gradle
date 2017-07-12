/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.caching.internal.controller.service;

import org.gradle.api.GradleException;
import org.gradle.api.Nullable;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.caching.BuildCacheException;
import org.gradle.caching.BuildCacheKey;
import org.gradle.caching.BuildCacheService;
import org.gradle.caching.internal.controller.DefaultBuildCacheController;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BaseBuildCacheServiceHandle implements BuildCacheServiceHandle {

    private static final Logger LOGGER = Logging.getLogger(OpFiringBuildCacheServiceHandle.class);

    protected final BuildCacheService service;

    protected final BuildCacheServiceRole role;
    private final boolean pushEnabled;
    private final boolean logStackTraces;

    private final AtomicInteger errorCount = new AtomicInteger();

    private final AtomicReference<String> disabledMessage = new AtomicReference<String>();
    private boolean closed;

    public BaseBuildCacheServiceHandle(BuildCacheService service, boolean push, BuildCacheServiceRole role, boolean logStackTraces) {
        this.role = role;
        this.service = service;
        this.pushEnabled = push;
        this.logStackTraces = logStackTraces;
    }

    @Nullable
    @Override
    public BuildCacheService getService() {
        return service;
    }

    @Override
    public boolean canLoad() {
        return disabledMessage.get() == null;
    }

    @Override
    public final void load(BuildCacheKey key, LoadTarget loadTarget) {
        final String description = "Load entry " + key + " from " + role.getDisplayName() + " build cache";
        try {
            loadInner(description, key, loadTarget);
        } catch (Exception e) {
            if (e instanceof BuildCacheException) {
                reportFailure("load", "from", key, e);
                recordFailure();
            } else {
                throw new GradleException("Could not load entry " + key + " from " + role.getDisplayName() + " build cache", e);
            }
        }
    }

    protected void loadInner(String description, BuildCacheKey key, LoadTarget loadTarget) {
        LOGGER.debug(description);
        service.load(key, loadTarget);
    }

    @Override
    public boolean canStore() {
        return pushEnabled && disabledMessage.get() == null;
    }

    @Override
    public final void store(BuildCacheKey key, StoreTarget storeTarget) {
        String description = "Store entry " + key + " in " + role.getDisplayName() + " build cache";
        try {
            storeInner(description, key, storeTarget);
        } catch (Exception e) {
            reportFailure("store", "in", key, e);
            if (e instanceof BuildCacheException) {
                recordFailure();
            } else {
                disable("a non-recoverable error was encountered", true);
            }
        }
    }

    protected void storeInner(String description, BuildCacheKey key, StoreTarget storeTarget) {
        LOGGER.debug(description);
        service.store(key, storeTarget);
        if (!storeTarget.isStored()) {
            throw new IllegalStateException("Store operation of " + role.getDisplayName() + " build cache completed without storing the artifact");
        }
    }

    private void disable(final String message, final boolean nonRecoverable) {
        if (disabledMessage.compareAndSet(null, message)) {
            wasDisabled(message, nonRecoverable);
        }
    }

    private void recordFailure() {
        if (errorCount.incrementAndGet() == DefaultBuildCacheController.MAX_ERRORS) {
            disable(DefaultBuildCacheController.MAX_ERRORS + " recoverable errors were encountered", false);
        }
    }

    private void reportFailure(String verb, String preposition, BuildCacheKey key, Throwable e) {
        if (!LOGGER.isWarnEnabled()) {
            return;
        }
        if (logStackTraces) {
            LOGGER.warn("Could not {} entry {} {} {} build cache", verb, key, preposition, role.getDisplayName(), e);
        } else {
            LOGGER.warn("Could not {} entry {} {} {} build cache: {}", verb, key, preposition, role.getDisplayName(), e.getMessage());
        }
    }

    protected void wasDisabled(String message, boolean nonRecoverable) {
    }

    @Override
    public void close() {
        LOGGER.debug("Closing {} build cache", role.getDisplayName());
        if (!closed) {
            String disableMessage = disabledMessage.get();
            if (disableMessage != null) {
                LOGGER.warn("The {} build cache was disabled during the build because {}.", role.getDisplayName(), disableMessage);
            }
            try {
                service.close();
            } catch (Exception e) {
                if (logStackTraces) {
                    LOGGER.warn("Error closing {} build cache: ", role.getDisplayName(), e);
                } else {
                    LOGGER.warn("Error closing {} build cache: {}", role.getDisplayName(), e.getMessage());
                }
            } finally {
                closed = true;
            }
        }
    }

}
