package org.unbrokendome.jackson.beanvalidation;

import java.util.EnumSet;


public enum BeanValidationFeature {

    REPORT_BEAN_PROPERTY_PATHS_IN_VIOLATIONS(false),
    REPORT_MISSING_REQUIRED_AS_NOTNULL_VIOLATION(false);


    private final boolean enabledByDefault;


    BeanValidationFeature(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }


    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }


    public static EnumSet<BeanValidationFeature> getDefaultFeatures() {
        EnumSet<BeanValidationFeature> features = EnumSet.noneOf(BeanValidationFeature.class);
        for (BeanValidationFeature feature : values()) {
            if (feature.isEnabledByDefault()) {
                features.add(feature);
            }
        }
        return features;
    }
}
