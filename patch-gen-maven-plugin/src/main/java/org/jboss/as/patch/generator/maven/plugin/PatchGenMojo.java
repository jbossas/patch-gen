/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.patch.generator.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jboss.as.patching.generator.PatchGenerator;

/**
 * Maven plug-in for creating patches for WildFly / JBoss EAP, using the patch-gen tool.
 * <p>
 * All options from {@link PatchGenerator} are supported. The configuration option names are the same as for the CLI
 * options, but without hyphens and camel-cased. E.g. use {@code appliesToDist} as counterpart to
 * {@code --applies-to-dist}.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * <plugin>
 *     <groupId>org.jboss.as</groupId>
 *     <artifactId>patch-gen-maven-plugin</artifactId>
 *     <version>2.0.1.Alpha1-SNAPSHOT</version>
 *     <executions>
 *         <execution>
 *             <id>create-patch-file</id>
 *             <phase>prepare-package</phase>
 *             <goals>
 *                 <goal>GenPatch</goal>
 *             </goals>
 *             <configuration>
 *                 <appliesToDist>path/to/source/dist</appliesToDist>
 *                 <updatedDist>path/to/updated/dist</updatedDist>
 *                 <patchConfig>path/to/patch.xml</patchConfig>
 *                 <outputFile>path/to/my-patch.zip</outputFile>
 *                 <includeVersion>true</includeVersion>
 *             </configuration>
 *         </execution>
 *     </executions>
 * </plugin>
 * }
 * </pre>
 *
 * @author Gunnar Morling
 */
@Mojo( name = "generate-patch", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class PatchGenMojo extends AbstractMojo {

    @Parameter( property = "patchConfig", required = true )
    private File patchConfig;

    @Parameter( property = "appliesToDist", required = true )
    private File appliesToDist;

    @Parameter( property = "updatedDist", required = true )
    private File updatedDist;

    @Parameter( property = "outputFile", required = true )
    private File outputFile;

    @Parameter( property = "assemblePatchBundle" )
    private Boolean assemblePatchBundle;

    @Parameter( property = "createTemplate" )
    private Boolean createTemplate;

    @Parameter( property = "detailedInspection" )
    private Boolean detailedInspection;

    @Parameter( property = "includeVersion" )
    private Boolean includeVersion;

    @Parameter( property = "combineWith" )
    private File combineWith;

    @Override
    public void execute() throws MojoExecutionException {
        List<String> args = new ArrayList<>();
        args.add( PatchGenerator.APPLIES_TO_DIST + "=" + appliesToDist.getPath() );
        args.add( PatchGenerator.OUTPUT_FILE + "=" + outputFile.getPath() );
        args.add( PatchGenerator.PATCH_CONFIG + "=" + patchConfig.getPath() );
        args.add( PatchGenerator.UPDATED_DIST + "=" + updatedDist.getPath() );

        if ( assemblePatchBundle != null ) {
            args.add( PatchGenerator.ASSEMBLE_PATCH_BUNDLE );
        }

        if ( createTemplate != null ) {
            args.add( PatchGenerator.CREATE_TEMPLATE );
        }

        if ( detailedInspection != null ) {
            args.add( PatchGenerator.DETAILED_INSPECTION );
        }

        if ( includeVersion != null ) {
            args.add( PatchGenerator.INCLUDE_VERSION );
        }

        if ( combineWith != null ) {
            args.add( PatchGenerator.COMBINE_WITH + "=" + combineWith.getPath() );
        }

        PatchGenerator.main( args.toArray( new String[0] ) );
    }
}
