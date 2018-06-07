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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
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
 *     <version>2.0.1.Final</version>
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

    private static final String LOG_FILE = "patchgen.log";

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

    @Parameter( property = "argLine" )
    private String argLine;

    @Parameter( property = "project.build.directory" )
    private File buildDirectory;

    @Parameter( property = "plugin.artifacts" )
    protected List<Artifact> pluginArtifacts;

    @Override
    public void execute() throws MojoExecutionException {
        List<String> args = new ArrayList<>();

        args.add( "java" );

        for ( String additionalArg : getAdditionalArgs() ) {
            args.add( additionalArg );
        }

        args.add( "-cp" );
        args.add( getClasspath() );
        args.add( PatchGenerator.class.getName() );

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

        // Ideally, we'd just invoke PatchGenerator directly; currently we cannot do so due to https://issues.jboss.org/browse/MODULES-136:
        // JBoss Modules, when used as a library, will set some system properties to values causing trouble for other plug-ins later in the
        // build; e.g. SAXParserFactory is redirected to a JBoss Modules specific variant which then cannot be found by other users such as
        // the Checkstyle plug-in (which naturally doesn't have JBoss Modules on the plug-in dependency path). Hence we start patch-gen in
        // a separate process
        //
        // PatchGenerator.main( args.toArray( new String[0] ) );
        try {
            Process p = new ProcessBuilder( args )
                    .redirectOutput( new File( buildDirectory, LOG_FILE ) )
                    .redirectError( new File( buildDirectory, LOG_FILE ) )
                    .start();
            p.waitFor();
        }
        catch (IOException | InterruptedException e) {
            throw new MojoExecutionException( "Execution of PatchGenerator failed. See " + LOG_FILE + " for details.", e );
        }

        if ( !outputFile.exists() ) {
            throw new MojoExecutionException( "Execution of PatchGenerator failed. See " + LOG_FILE + " for details." );
        }
    }

    private String getClasspath() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for ( Artifact artifact : pluginArtifacts ) {
            if ( first ) {
                first = false;
            }
            else {
                sb.append( File.pathSeparator );
            }
            sb.append( artifact.getFile().getPath() );
        }

        return sb.toString();
    }

    private String[] getAdditionalArgs() throws MojoExecutionException {
        if ( argLine == null || argLine.trim().length() == 0 ) {
            return new String[0];
        }

        try {
            return CommandLineUtils.translateCommandline( argLine.replace( "\n", " " ).replaceAll( "\r", " " ) );
        }
        catch (CommandLineException e) {
            throw new MojoExecutionException( "Unable to parse argLine: " + argLine, e );
        }
    }
}
