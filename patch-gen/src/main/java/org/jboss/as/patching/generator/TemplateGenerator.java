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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.UUID;

import org.jboss.as.patching.logging.PatchLogger;

/**
 * Generate a simple template for a give patch type.
 *
 * @author Emanuel Muckenhuber
 */
class TemplateGenerator {

    private static final String TAB = "   ";

    static void generate(final String... args) throws IOException {

        boolean stdout = false;
        Boolean oneOff = null;
        String patchID = UUID.randomUUID().toString();
        String appliesToVersion = null;

        final int argsLength = args.length;
        for (int i = 0; i < argsLength; i++) {
            final String arg = args[i];
            try {
                if ("--help".equals(arg) || "-h".equals(arg) || "-H".equals(arg)) {
                    usage();
                    return;
                } else if(arg.equals("--one-off")) {
                    if (oneOff == null) {
                        oneOff = Boolean.TRUE;
                        patchID = args[++i];
                    } else {
                        usage();
                        return;
                    }
                } else if(arg.equals("--cumulative")) {
                    if (oneOff == null) {
                        oneOff = Boolean.FALSE;
                        patchID = args[++i];
                    } else {
                        usage();
                        return;
                    }
                } else if(arg.equals("--applies-to-version")) {
                    appliesToVersion = args[++i];
                } else if(arg.equals("--std.out")) {
                    stdout = true;
                } else if (arg.equals("--create-template")) {
                    continue;
                } else {
                    System.err.println(PatchLogger.ROOT_LOGGER.argumentExpected(arg));
                    usage();
                    return;
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println(PatchLogger.ROOT_LOGGER.argumentExpected(arg));
                usage();
                return;
            }
        }

        if (oneOff == null) {
            usage();
            return;
        }

        final Writer target;
        if (stdout) {
            target = new OutputStreamWriter(System.out);        	
        } else {
        	target = new FileWriter(new File("patch-config-" +patchID + ".xml"));
        }
        
        try(BufferedWriter bw = new BufferedWriter(target)) {
            bw.write("<?xml version='1.0' encoding='UTF-8'?>");bw.newLine();
            elementStart(bw, 0, "patch-config", "xmlns", "urn:jboss:patch-config:1.0");
            element(bw, 1, "name", patchID);
            element(bw, 1, "description", "No description available");
            element(bw, 1, oneOff ? "one-off" : "cumulative", "applies-to-version", appliesToVersion);
            
            // Write patch element
            elementStart(bw, 1, "element", "patch-id", "layer-base-" + patchID);
            element(bw, 2, oneOff ? "one-off" : "cumulative", "name", "base");
            element(bw, 2, "description", "No description available");

            if (oneOff) {
				elementStart(bw, 2, "specified-content");
				elementStart(bw, 3, "modules");
				element(bw, 4, "updated", "name", "org.jboss.as.server");
				elementEnd(bw, 3, "modules");
				elementEnd(bw, 2, "specified-content");
			}
			elementEnd(bw, 1, "element");

			if (oneOff) {
				elementStart(bw, 1, "specified-content");
				elementStart(bw, 2, "misc-files");
				element(bw, 3, "updated", "path", "version.txt");
				elementEnd(bw, 2, "misc-files");
				elementEnd(bw, 1, "specified-content");
			} else {
				element(bw, 1, "generate-by-diff");
			}

            elementEnd(bw, 0, "patch-config");
        }
    }


    private static void elementStart(BufferedWriter writer, int offset, String name) throws IOException {
    	elementStart(writer, offset, name, null, null);
    }

    private static void elementStart(BufferedWriter writer, int offset, String name, String attrName, String attrValue) throws IOException {
    	writeStart(writer, offset, name, attrName, attrValue, false);
    	writer.newLine();
    }

    private static void element(BufferedWriter writer, int offset, String name) throws IOException {
    	element(writer, offset, name, null);
    }

    private static void element(BufferedWriter writer, int offset, String name, String content) throws IOException {
    	element(writer, offset, name, null, null, content);
    }

    private static void element(BufferedWriter writer, int offset, String name, String attrName, String attrValue) throws IOException {
    	element(writer, offset, name, attrName, attrValue, null);
    }

    private static void element(BufferedWriter writer, int offset, String name, String attrName, String attrValue, String content) throws IOException {
    	writeStart(writer, offset, name, attrName, attrValue, content == null);
    	if(content != null) {
    		writer.write(content);
    		elementEnd(writer, 0, name);
    	}
    }

	private static void elementEnd(BufferedWriter writer, int offset, String name) throws IOException {
		for(int i = 0; i < offset; ++i) {
    	    writer.write(TAB);
    	}
		writer.write("</");
		writer.write(name);
		writer.write('>');
    	writer.newLine();
	}

	private static void writeStart(BufferedWriter writer, int offset, String name, String attrName, String attrValue, boolean empty) throws IOException {
		for(int i = 0; i < offset; ++i) {
    	    writer.write(TAB);
    	}
    	writer.write('<');
    	writer.write(name);
    	if(attrValue != null) {
    		writer.write(' ');
    		writer.write(attrName);
    		writer.write("=\"");
    		writer.write(attrValue);
    		writer.write('\"');
    	}
    	if(empty) {
    		writer.write("/>");
    		writer.newLine();
    	} else {
    		writer.write('>');
    	}
	}
    
    static void usage() {
        System.err.println("USAGE:");
        System.err.println("patch-gen.sh --create-template --one-off    [patch-id]");
        System.err.println("patch-gen.sh --create-template --cumulative [patch-id]");
        System.err.println();
        System.err.println("this will create a patch-config-[patch-id].xml");
        System.err.println("if this is not desired just append --std.out");
    }
}
