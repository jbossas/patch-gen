package org.jboss.as.patching.generator;

/**
* @author Emanuel Muckenhuber
*/
interface DistributionContentItemFilter {

    boolean accept(DistributionContentItem item);

}
