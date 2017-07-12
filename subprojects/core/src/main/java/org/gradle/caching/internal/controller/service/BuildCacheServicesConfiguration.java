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

import org.gradle.caching.BuildCacheService;
import org.gradle.caching.local.internal.LocalBuildCacheService;

public final class BuildCacheServicesConfiguration {

    public final BuildCacheService legacyLocal;
    public final boolean legacyLocalPush;

    public final BuildCacheService remote;
    public final boolean remotePush;

    public final LocalBuildCacheService local;
    public final boolean localPush;

    public BuildCacheServicesConfiguration(
        BuildCacheService legacyLocal, boolean legacyLocalPush,
        BuildCacheService remote, boolean remotePush,
        LocalBuildCacheService local, boolean localPush
    ) {
        this.legacyLocal = legacyLocal;
        this.legacyLocalPush = legacyLocalPush;
        this.remote = remote;
        this.remotePush = remotePush;
        this.local = local;
        this.localPush = localPush;
    }
}
