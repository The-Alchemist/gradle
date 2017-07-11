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

package org.gradle.ide.xcode.tasks;

import com.dd.plist.NSDictionary;
import com.facebook.buck.apple.xcode.GidGenerator;
import com.facebook.buck.apple.xcode.XcodeprojSerializer;
import com.facebook.buck.apple.xcode.xcodeproj.PBXFileReference;
import com.facebook.buck.apple.xcode.xcodeproj.PBXNativeTarget;
import com.facebook.buck.apple.xcode.xcodeproj.PBXProject;
import com.facebook.buck.apple.xcode.xcodeproj.PBXReference;
import com.facebook.buck.apple.xcode.xcodeproj.PBXShellScriptBuildPhase;
import com.facebook.buck.apple.xcode.xcodeproj.PBXTarget;
import com.facebook.buck.apple.xcode.xcodeproj.XCBuildConfiguration;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.UncheckedException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;

public class GenerateXcodeProjectFileTask extends DefaultTask {
    @TaskAction
    void generate() {
        // TODO - this task should really only merge the in-memory model generated by a task inside each project
        PBXProject project = new PBXProject(getProject().getName());

        // Required for making think the project isn't corrupted...
        // TODO - create according to buildTypes container
        XCBuildConfiguration c = project.getBuildConfigurationList().getBuildConfigurationsByName().getUnchecked("Debug");
        c.setBuildSettings(new NSDictionary());


        ConfigurableFileTree sources = getProject().fileTree("src/main/swift");
        for (File source : sources.getFiles()) {
            project.getMainGroup().getChildren().add(new PBXFileReference(source.getName(), source.getAbsolutePath(), PBXReference.SourceTree.ABSOLUTE));
        }
        if (getProject().getBuildFile().exists()) {
            project.getMainGroup().getChildren().add(new PBXFileReference(getProject().getBuildFile().getName(), getProject().getBuildFile().getAbsolutePath(), PBXReference.SourceTree.ABSOLUTE));
        }

        // TODO - create a target per component
        project.getTargets().add(createTarget("target11", getProject().getName(), ":linkMain", "build/exe"));


        // Serialize the model
        // TODO - Write the pipeworks to use GeneratorTask
        XcodeprojSerializer serializer = new XcodeprojSerializer(new GidGenerator(Collections.<String>emptySet()), project);
        NSDictionary rootObject = serializer.toPlist();
        File xcodeprojDir = getProject().file(getProject().getName() + ".xcodeproj");
        xcodeprojDir.mkdirs();
        File outputFile = new File(xcodeprojDir, "project.pbxproj");

        // TODO - Use GeneratorTask since this is the same code as AbstractPersitableConfigurationObject
        try {
            Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outputFile)), "UTF-8");
            try {
                String content = rootObject.toASCIIPropertyList();
                writer.write(content);
                writer.flush();
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    private PBXShellScriptBuildPhase createBuildPhase(String taskName) {
        PBXShellScriptBuildPhase gradleBuildPhase = new PBXShellScriptBuildPhase();
        // TODO - Validate we can't put space in a shebang https://lists.gnu.org/archive/html/bug-bash/2008-05/msg00051.html
        gradleBuildPhase.setShellPath("/bin/bash");
        // TODO - Use the right task to build the binary only
        String shellScript = "";
        // TODO - allow to modify the location of gradlew/gradle through task input
        if (getProject().file("gradlew").exists()) {
            shellScript += "exec \"" + getProject().file("gradlew").getAbsolutePath() + "\" " + taskName;
        } else {
            // TODO - default to gradle on the path (or should we generate an error if no gradle is in the path?)
            shellScript += "exec \"~/gradle/gradle-source-build/bin/gradle\" " + taskName;
        }
        gradleBuildPhase.setShellScript(shellScript);

        return gradleBuildPhase;
    }

    /**
     *
     * @param name name of the component (project)
     * @param productName the baseName of a component
     * @param taskName should be the lifecycle task of the binary to build
     * @param path For testing only!
     * @return
     */
    private PBXNativeTarget createTarget(String name, String productName, String taskName, String path) {
        String productFilename = getProject().file(path + productName).getAbsolutePath();

        PBXNativeTarget target = new PBXNativeTarget(name, PBXTarget.ProductType.TOOL);
        target.setProductName(productName);
        target.setProductReference(new PBXFileReference(productName, productFilename, PBXReference.SourceTree.ABSOLUTE));
        NSDictionary buildSettings = new NSDictionary();
        // TODO - Should we redirect all intermediate files from xcode to folder inside the build tree (so we can clean it instead of relying on XCode)
        buildSettings.put("CONFIGURATION_BUILD_DIR", getProject().file(path).getAbsolutePath()); // Point output to the right directory
        buildSettings.put("PRODUCT_NAME", productName);  // Mandatory
        target.getBuildConfigurationList().getBuildConfigurationsByName().getUnchecked("Debug").setBuildSettings(buildSettings);
        target.getBuildPhases().add(createBuildPhase(taskName));

        return target;
    }
}
