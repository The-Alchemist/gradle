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


package org.gradle.binarycompatibility.transforms

import groovy.transform.CompileStatic
import org.gradle.api.artifacts.transform.ArtifactTransform

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@CompileStatic
class ExplodeZipAndFindJars extends ArtifactTransform {

    @Override
    List<File> transform(final File file) {
        List<File> result = []
        if (outputDirectory.exists() && outputDirectory.listFiles().length == 0) {
            ZipInputStream zin = new ZipInputStream(file.newInputStream())
            ZipEntry zipEntry
            while (zipEntry = zin.nextEntry) {
                String shortName = zipEntry.name
                if (shortName.contains('/')) {
                    shortName = shortName.substring(shortName.lastIndexOf('/') + 1)
                }
                if (shortName.startsWith('gradle-') && shortName.endsWith('.jar')) {
                    def out = new File(outputDirectory, shortName)

                    out.withOutputStream { os ->
                        byte[] buffer = new byte[2048]
                        int n
                        while ((n = zin.read(buffer, 0, 2048)) > -1) {
                            os.write(buffer, 0, n)
                        }

                    }
                    zin.closeEntry()
                    result << out
                }
            }
        }
        result
    }
}
