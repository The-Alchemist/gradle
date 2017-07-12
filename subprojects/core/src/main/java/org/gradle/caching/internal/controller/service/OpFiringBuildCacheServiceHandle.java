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

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.caching.BuildCacheKey;
import org.gradle.caching.BuildCacheService;
import org.gradle.caching.internal.controller.operations.DisableOperationDetails;
import org.gradle.caching.internal.controller.operations.DisableOperationResult;
import org.gradle.caching.internal.controller.operations.LoadOperationDetails;
import org.gradle.caching.internal.controller.operations.LoadOperationHitResult;
import org.gradle.caching.internal.controller.operations.LoadOperationMissResult;
import org.gradle.caching.internal.controller.operations.StoreOperationDetails;
import org.gradle.caching.internal.controller.operations.StoreOperationResult;
import org.gradle.caching.internal.operations.BuildCacheDisableServiceBuildOperationType.Details.DisabledReason;
import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.RunnableBuildOperation;
import org.gradle.internal.progress.BuildOperationDescriptor;

public class OpFiringBuildCacheServiceHandle extends BaseBuildCacheServiceHandle {

    private static final Logger LOGGER = Logging.getLogger(OpFiringBuildCacheServiceHandle.class);

    private final BuildOperationExecutor buildOperationExecutor;

    public OpFiringBuildCacheServiceHandle(BuildCacheService service, boolean push, BuildCacheServiceRole role, BuildOperationExecutor buildOperationExecutor, boolean logStackTraces) {
        super(service, push, role, logStackTraces);
        this.buildOperationExecutor = buildOperationExecutor;
    }

    @Override
    protected void loadInner(final String description, final BuildCacheKey key, final LoadTarget loadTarget) {
        buildOperationExecutor.run(new RunnableBuildOperation() {
            @Override
            public void run(BuildOperationContext context) {
                OpFiringBuildCacheServiceHandle.super.loadInner(description, key, loadTarget);
                context.setResult(
                    loadTarget.isLoaded()
                        ? new LoadOperationHitResult(loadTarget.getLoadedSize())
                        : LoadOperationMissResult.INSTANCE
                );
            }

            @Override
            public BuildOperationDescriptor.Builder description() {
                return BuildOperationDescriptor.displayName(description)
                    .details(new LoadOperationDetails(key));
            }
        });
    }

    @Override
    protected void storeInner(final String description, final BuildCacheKey key, final StoreTarget storeTarget) {
        buildOperationExecutor.run(new RunnableBuildOperation() {
            @Override
            public void run(BuildOperationContext context) {
                OpFiringBuildCacheServiceHandle.super.storeInner(description, key, storeTarget);
                context.setResult(StoreOperationResult.INSTANCE);
            }

            @Override
            public BuildOperationDescriptor.Builder description() {
                return BuildOperationDescriptor.displayName(description)
                    .details(new StoreOperationDetails(key, storeTarget.getSize()));
            }
        });
    }

    @Override
    protected void wasDisabled(final String message, final boolean nonRecoverable) {
        buildOperationExecutor.run(new RunnableBuildOperation() {
            @Override
            public void run(BuildOperationContext context) {
                LOGGER.warn("The {} build cache is now disabled because {}.", role.getDisplayName(), message);
                context.setResult(DisableOperationResult.INSTANCE);
            }

            @Override
            public BuildOperationDescriptor.Builder description() {
                return BuildOperationDescriptor.displayName("Disable " + role.getDisplayName() + " build cache")
                    .details(new DisableOperationDetails(
                        message,
                        nonRecoverable ? DisabledReason.NON_RECOVERABLE_ERROR : DisabledReason.TOO_MANY_RECOVERABLE_ERRORS
                    ));
            }
        });
    }
}
