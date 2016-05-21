/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.testkit.runner

import org.gradle.integtests.fixtures.AvailableJavaHomes
import org.gradle.internal.jvm.UnsupportedJavaRuntimeException
import org.gradle.testkit.runner.fixtures.NoDebug
import org.gradle.testkit.runner.fixtures.NonCrossVersion
import org.gradle.tooling.GradleConnectionException
import org.gradle.util.GradleVersion
import spock.lang.IgnoreIf

@NonCrossVersion
class GradleRunnerSupportedBuildJvmIntegrationTest extends BaseGradleRunnerIntegrationTest {
    @IgnoreIf({AvailableJavaHomes.jdk6 == null})
    @NoDebug
    def "fails when build is configured to use Java 6"() {
        given:
        testDirectory.file("gradle.properties").writeProperties("org.gradle.java.home": AvailableJavaHomes.jdk6.javaHome.absolutePath)

        when:
        runner().buildAndFail()

        then:
        IllegalStateException e = thrown()
        e.message.startsWith("An error occurred executing build with no args in directory ")
        e.cause instanceof GradleConnectionException
        e.cause.cause.message == "Gradle ${GradleVersion.current().version} requires Java 7 or later to run. Your build is currently configured to use Java 6."
    }
}
