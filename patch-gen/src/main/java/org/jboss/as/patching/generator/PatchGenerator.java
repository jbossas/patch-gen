/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.generator;

import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;

import javax.xml.stream.XMLStreamException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.logging.PatchLogger;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchMerger;
import org.jboss.as.version.ProductConfig;
import org.jboss.modules.Module;

/**
 * Generates a patch archive.
 * Run it using JBoss modules:
 * <pre><code>
 *   java -jar jboss-modules.jar -mp modules/ org.jboss.as.patching.generator
 * </code></pre>
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PatchGenerator {

    private static final String APPLIES_TO_DIST = "--applies-to-dist";
    private static final String ASSEMBLE_PATCH_BUNDLE = "--assemble-patch-bundle";
    private static final String CREATE_TEMPLATE = "--create-template";
    private static final String DETAILED_INSPECTION = "--detailed-inspection";
    private static final String INCLUDE_VERSION = "--include-version";
    private static final String COMBINE_WITH = "--combine-with";
    private static final String OUTPUT_FILE = "--output-file";
    private static final String PATCH_CONFIG = "--patch-config";
    private static final String UPDATED_DIST = "--updated-dist";

    public static void main(String[] args) {
        try {
            PatchGenerator patchGenerator = parse(args);
            if (patchGenerator != null) {
                patchGenerator.process();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final boolean includeVersion;
    private final File patchConfigFile;
    private File oldRoot;
    private File newRoot;
    private File patchFile;
    private File previousCp;
    private File tmp;

    private PatchGenerator(File patchConfig, File oldRoot, File newRoot, File patchFile, boolean includeVersion, File previousCp) {
        this.patchConfigFile = patchConfig;
        this.oldRoot = oldRoot;
        this.newRoot = newRoot;
        this.patchFile = patchFile;
        this.includeVersion = includeVersion;
        this.previousCp = previousCp;
    }

    private void process() throws PatchingException, IOException, XMLStreamException {

        try {
            PatchConfig patchConfig = parsePatchConfig();

            Set<String> required = new TreeSet<String>();
            if (newRoot == null) {
                required.add(UPDATED_DIST);
            }
            if (oldRoot == null) {
                required.add(APPLIES_TO_DIST);
            }
            if (patchFile == null) {
                if (newRoot != null) {
                    patchFile = new File(newRoot, "patch-" + System.currentTimeMillis() + ".par");
                } else {
                    required.add(OUTPUT_FILE);
                }
            }
            if (!required.isEmpty()) {
                System.err.printf(PatchLogger.ROOT_LOGGER.missingRequiredArgs(required));
                usage();
                return;
            }

            createTempStructure(patchConfig.getPatchId());

            // See whether to include the updated version information
            boolean includeVersion = patchConfig.getPatchType() == Patch.PatchType.CUMULATIVE ? true : this.includeVersion;
            final String[] ignored = includeVersion ? new String[0] : new String[] {"org/jboss/as/product", "org/jboss/as/version"};

            // Create the distributions
            final Distribution base = Distribution.create(oldRoot, ignored);
            final Distribution updated = Distribution.create(newRoot, ignored);

            if (!base.getName().equals(updated.getName())) {
                throw processingError("distribution names don't match, expected: %s, but was %s ", base.getName(), updated.getName());
            }
            //
            if (patchConfig.getAppliesToProduct() != null && ! patchConfig.getAppliesToProduct().equals(base.getName())) {
                throw processingError("patch target does not match, expected: %s, but was %s", patchConfig.getAppliesToProduct(), base.getName());
            }
            //
            if (patchConfig.getAppliesToVersion() != null && ! patchConfig.getAppliesToVersion().equals(base.getVersion())) {
                throw processingError("patch target version does not match, expected: %s, but was %s", patchConfig.getAppliesToVersion(), base.getVersion());
            }

            // Build the patch metadata
            final PatchBuilderWrapper builder = patchConfig.toPatchBuilder();
            builder.setPatchId(patchConfig.getPatchId());
            builder.setDescription(patchConfig.getDescription());
            builder.setOptionalPaths(patchConfig.getOptionalPaths());
            if (patchConfig.getPatchType() == Patch.PatchType.CUMULATIVE) {
                // CPs need to upgrade
                if (base.getVersion().equals(updated.getVersion())) {
                    System.out.println("WARN: cumulative patch does not upgrade version " + base.getVersion());
                }
                builder.upgradeIdentity(base.getName(), base.getVersion(), updated.getVersion());
            } else {
                builder.oneOffPatchIdentity(base.getName(), base.getVersion());
            }

            // Create the resulting patch
            final Patch patch = builder.compare(base, updated, includeVersion);

            // Copy the contents to the temp dir structure
            PatchContentWriter.process(tmp, newRoot, patch);

            if(previousCp != null) {
                PatchMerger.merge(previousCp, tmp, patchFile);
            } else {
                ZipUtils.zip(tmp, patchFile);
            }

        } finally {
            IoUtils.recursiveDelete(tmp);
        }

    }

    private PatchConfig parsePatchConfig() throws FileNotFoundException, XMLStreamException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(patchConfigFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            return PatchConfigXml.parse(bis);
        } finally {
            IoUtils.safeClose(fis);
        }
    }

    private void createTempStructure(String patchId) {

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        int count = 0;
        while (tmp == null || tmp.exists()) {
            count++;
            tmp = new File(tmpDir, "jboss-as-patch-" + patchId + "-" + count);
        }
        if (!tmp.mkdirs()) {
            throw processingError("Cannot create tmp dir for patch create at %s", tmp.getAbsolutePath());
        }
        tmp.deleteOnExit();
        File metaInf = new File(tmp, "META-INF");
        metaInf.mkdir();
        metaInf.deleteOnExit();
        File misc = new File(tmp, "misc");
        misc.mkdir();
        misc.deleteOnExit();
    }

    private static PatchGenerator parse(String[] args) throws Exception {

        File patchConfig = null;
        File oldFile = null;
        File newFile = null;
        File patchFile = null;
        boolean includeVersion = false;
        File combineWith = null;

        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if ("--version".equals(arg) || "-v".equals(arg)
                        || "-version".equals(arg) || "-V".equals(arg)) {
                    final String homeDir = getSecurityManager() == null ? getProperty("jboss.home.dir") : Usage.getSystemProperty("jboss.home.dir");
                    ProductConfig productConfig = new ProductConfig(Module.getBootModuleLoader(), homeDir, Collections.emptyMap());
                    System.out.println(productConfig.getPrettyVersionString());
                    return null;
                } else if ("--help".equals(arg) || "-h".equals(arg) || "-H".equals(arg)) {
                    usage();
                    return null;
                } else if (arg.startsWith(APPLIES_TO_DIST)) {
                    String val = arg.substring(APPLIES_TO_DIST.length() + 1);
                    oldFile = new File(val);
                    if (!oldFile.exists()) {
                        System.err.printf(PatchLogger.ROOT_LOGGER.fileDoesNotExist(arg));
                        usage();
                        return null;
                    } else if (!oldFile.isDirectory()) {
                        System.err.printf(PatchLogger.ROOT_LOGGER.fileIsNotADirectory(arg));
                        usage();
                        return null;
                    }
                } else if (arg.startsWith(UPDATED_DIST)) {
                    String val = arg.substring(UPDATED_DIST.length() + 1);
                    newFile = new File(val);
                    if (!newFile.exists()) {
                        System.err.printf(PatchLogger.ROOT_LOGGER.fileDoesNotExist(arg));
                        usage();
                        return null;
                    } else if (!newFile.isDirectory()) {
                        System.err.printf(PatchLogger.ROOT_LOGGER.fileIsNotADirectory(arg));
                        usage();
                        return null;
                    }
                } else if (arg.startsWith(PATCH_CONFIG)) {
                    String val = arg.substring(PATCH_CONFIG.length() + 1);
                    patchConfig = new File(val);
                    if (!patchConfig.exists()) {
                        System.err.printf(PatchLogger.ROOT_LOGGER.fileDoesNotExist(arg));
                        usage();
                        return null;
                    } else if (patchConfig.isDirectory()) {
                        System.err.printf(PatchLogger.ROOT_LOGGER.fileIsADirectory(arg));
                        usage();
                        return null;
                    }
                } else if (arg.startsWith(OUTPUT_FILE)) {
                    String val = arg.substring(OUTPUT_FILE.length() + 1);
                    patchFile = new File(val);
                    if (patchFile.exists() && patchFile.isDirectory()) {
                        System.err.printf(PatchLogger.ROOT_LOGGER.fileIsADirectory(arg));
                        usage();
                        return null;
                    }
                } else if (arg.equals(DETAILED_INSPECTION)) {
                    ModuleDiffUtils.deepInspection = true;
                } else if (arg.equals(INCLUDE_VERSION)) {
                    includeVersion = true;
                } else if (arg.equals(CREATE_TEMPLATE)) {
                    TemplateGenerator.generate(args);
                    return null;
                } else if (arg.equals(ASSEMBLE_PATCH_BUNDLE)) {
                    PatchBundleGenerator.assemble(args);
                    return null;
                } else if (arg.startsWith(COMBINE_WITH)) {
                    String val = arg.substring(COMBINE_WITH.length() + 1);
                    combineWith = new File(val);
                    if (!combineWith.exists()) {
                        System.err.printf(PatchLogger.ROOT_LOGGER.fileDoesNotExist(arg));
                        usage();
                        return null;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.printf(PatchLogger.ROOT_LOGGER.argumentExpected(arg));
                usage();
                return null;
            }
        }

        if (patchConfig == null) {
            System.err.printf(PatchLogger.ROOT_LOGGER.missingRequiredArgs(Collections.singleton(PATCH_CONFIG)));
            usage();
            return null;
        }

        return new PatchGenerator(patchConfig, oldFile, newFile, patchFile, includeVersion, combineWith);
    }

    private static void usage() {

        Usage usage = new Usage();

        usage.addArguments(APPLIES_TO_DIST + "=<file>");
        usage.addInstruction("Filesystem path of a pristine unzip of the distribution of the version of the software to which the generated patch applies");

        usage.addArguments("-h", "--help");
        usage.addInstruction("Display this message and exit");

        usage.addArguments(OUTPUT_FILE + "=<file>");
        usage.addInstruction("Filesystem location to which the generated patch file should be written");

        usage.addArguments(PATCH_CONFIG + "=<file>");
        usage.addInstruction("Filesystem path of the patch generation configuration file to use");

        usage.addArguments(UPDATED_DIST + "=<file>");
        usage.addInstruction("Filesystem path of a pristine unzip of a distribution of software which contains the changes that should be incorporated in the patch");

        usage.addArguments("-v", "--version");
        usage.addInstruction("Print version and exit");

        usage.addArguments(DETAILED_INSPECTION);
        usage.addInstruction("Enable detailed inspection for all modules.");

        usage.addArguments(COMBINE_WITH + "=<file>");
        usage.addInstruction("Filesystem path of the previous CP to be included into the same package with the newly generated one");

        String headline = usage.getDefaultUsageHeadline("patch-gen");
        System.out.print(usage.usage(headline));

    }

    static RuntimeException processingError(String message, Object... arguments) {
        return new RuntimeException(String.format(message, arguments)); // no 18n for the generation
    }

    static RuntimeException processingError(Exception e, String message, Object... arguments) {
        return new RuntimeException(String.format(message, arguments), e); // no 18n for the generation
    }

}
