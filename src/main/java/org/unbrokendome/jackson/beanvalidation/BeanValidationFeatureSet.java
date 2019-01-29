package org.unbrokendome.jackson.beanvalidation;

import java.util.Set;


final class BeanValidationFeatureSet {

    private final Set<BeanValidationFeature> features;


    BeanValidationFeatureSet(Set<BeanValidationFeature> features) {
        this.features = features;
    }


    boolean isEnabled(BeanValidationFeature feature) {
        return features.contains(feature);
    }


    boolean isDisabled(BeanValidationFeature feature) {
        return !features.contains(feature);
    }
}
