package org.jboss.as.patching.generator;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.as.patching.HashUtils;

/**
 * @author Emanuel Muckenhuber
 */
class ModuleDiffUtils implements XMLStreamConstants {

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    /**
     * Process a module.
     *
     * This will go parse the module.xml and record all attributes and elements, except resources, which are processed
     * after separately to ignore different file system paths.
     *
     * @param root          the module root
     * @param moduleName    the module name
     * @param metadataHash  the hash used for the metadata
     * @return the comparison hash for the module
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static byte[] processModule(final File root, final String moduleName, final byte[] metadataHash) throws IOException, NoSuchAlgorithmException {

        if (! moduleName.startsWith("org.jboss.as")) {
            return metadataHash;
        }

        final File moduleXml = new File(root, "module.xml");
        if (! moduleXml.isFile()) {
            throw new IOException("not a module" + root.getAbsolutePath());
        }

        final Set<String> resources = new LinkedHashSet<>();
        final MessageDigest moduleDigest = MessageDigest.getInstance("SHA1");

        // Process the module.xml
        final InputStream stream = new FileInputStream(moduleXml);
        try {
            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
            processRoot(reader, moduleDigest, resources);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        // Process resource paths
        for (final String path : resources) {
            final File resource = new File(root, path);
            if (! resource.exists()) {
                throw new FileNotFoundException(resource.getAbsolutePath());
            }
            if (path.endsWith(".jar")) {
                try {
                    JarDiffUtils.internalJarComparison(resource, moduleDigest, false);
                } catch (Exception e) {
                    throw new IOException("failed to process " + resource.getAbsolutePath(), e);
                }
            } else {
                moduleDigest.update(HashUtils.hashFile(resource));
            }
        }

        // Process native libs
        final File lib = new File(root, "lib");
        if (lib.exists()) {
            moduleDigest.update(HashUtils.hashFile(lib));
        }

        return moduleDigest.digest();
    }

    protected static void processRoot(final XMLStreamReader reader, final MessageDigest digest, final Set<String> resources) throws XMLStreamException {

        reader.require(START_DOCUMENT, null, null);
        reader.nextTag();
        reader.require(START_ELEMENT, null, null);

        final String namespace = reader.getNamespaceURI();
        digest.update(namespace.getBytes());
        processAttributes(reader, digest);
        processXml(reader, digest, resources);
        while (reader.next() != END_DOCUMENT) {
        }
    }

    protected static void processXml(final XMLStreamReader reader, final MessageDigest digest, final Set<String> resources) throws XMLStreamException {
        processAttributes(reader, digest);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            if (localName.equals("resources")) {
                processResources(reader, resources);
            } else {
                digest.update(localName.getBytes());
                processXml(reader, digest, resources);
            }
        }
    }

    protected static void processAttributes(final XMLStreamReader reader, final MessageDigest digest) {
        int attributes = reader.getAttributeCount();
        for (int i = 0; i < attributes; i++) {
            final String name = reader.getAttributeLocalName(i);
            final String value = reader.getAttributeValue(i);
            digest.update(name.getBytes());
            digest.update(value.getBytes());
        }
    }

    protected static void processResources(final XMLStreamReader reader, final Set<String> resources) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            if ("resource-root".equals(localName)) {
                processResource(reader, resources);
            } else {
                throw new XMLStreamException("unrecognized element " + localName);
            }
        }
    }

    protected static void processResource(final XMLStreamReader reader, final Set<String> resources) throws XMLStreamException {
        final int attributeCount = reader.getAttributeCount();
        if (attributeCount != 1) {
            throw new XMLStreamException();
        }

        final String path = reader.getAttributeValue(0);
        resources.add(path.trim());

        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            throw new XMLStreamException("unexpected element");
        }
    }

    private static void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

}
