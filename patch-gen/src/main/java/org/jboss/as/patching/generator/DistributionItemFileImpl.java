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

import static org.jboss.as.patching.generator.PatchGenerator.processingError;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.patching.HashUtils;

/**
 * File based content item implementation.
 *
 * @author Emanuel Muckenhuber
 */
class DistributionItemFileImpl extends DistributionContentItem {

    private final File file;
    private final Set<DistributionContentItem> children;
    private byte[] cachedMetadataHash = null;

    protected DistributionItemFileImpl(File file, DistributionContentItem parent) {
        this(file, parent, file.getName());
    }

    protected DistributionItemFileImpl(File file, DistributionContentItem parent, String name) {
        super(parent, name);
        this.file = file;
        if (file.isDirectory()) {
            children = new TreeSet<DistributionContentItem>();
        } else {
            children = NO_CHILDREN;
        }
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public byte[] getMetadataHash() {
        try {
            if (cachedMetadataHash == null) {
                cachedMetadataHash = HashUtils.hashFile(file);
            }
            return cachedMetadataHash;
        } catch (IOException e) {
            throw processingError(e, "failed to generate hash");
        }
    }

    @Override
    public byte[] getComparisonHash() {
        try {
            return JarDiffUtils.calculateHash(file, this);
        } catch (Exception e) {
            throw processingError(e, "failed to generate hash");
        }
    }

    @Override
    public boolean isLeaf() {
        return file.isFile();
    }

    @Override
    public Set<DistributionContentItem> getChildren() {
        return children;
    }

}
