package org.jboss.as.patching.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.runner.ContentItemFilter;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SkipMiscFilesContentItemFilter implements ContentItemFilter {
    private final List<Pattern> includedMiscFiles;

    private SkipMiscFilesContentItemFilter(List<Pattern> includedMiscFiles) {
        this.includedMiscFiles = includedMiscFiles;
    }

    static SkipMiscFilesContentItemFilter create(List<String> includedMiscFiles) {
        List<Pattern> patterns = new ArrayList<>();
        if (includedMiscFiles != null) {
            for (String s : includedMiscFiles) {
                patterns.add(Pattern.compile(s));
            }
        }
        return new SkipMiscFilesContentItemFilter(patterns);
    }

    @Override
    public boolean accepts(ContentItem item) {
        if (item.getContentType() != ContentType.MISC) {
            return true;
        }
        for (Pattern pattern : includedMiscFiles) {
            if (pattern.matcher(item.getRelativePath()).matches()) {
                return true;
            }
        }
        return false;
    }

}
