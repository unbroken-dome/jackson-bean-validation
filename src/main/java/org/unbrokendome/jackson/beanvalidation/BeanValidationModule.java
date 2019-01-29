package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;


public final class BeanValidationModule extends Module {

    private final ValidatorFactory validatorFactory;
    private final EnumSet<BeanValidationFeature> features;


    public BeanValidationModule(ValidatorFactory validatorFactory) {
        this(validatorFactory, BeanValidationFeature.getDefaultFeatures());
    }


    public BeanValidationModule(ValidatorFactory validatorFactory, Collection<BeanValidationFeature> features) {
        this(validatorFactory, EnumSet.copyOf(features));
    }


    private BeanValidationModule(ValidatorFactory validatorFactory, EnumSet<BeanValidationFeature> features) {
        this.validatorFactory = validatorFactory;
        this.features = features;
    }


    @Override
    public String getModuleName() {
        return "jackson-bean-validation";
    }


    @Override
    public Version version() {
        return Version.unknownVersion();
    }


    public BeanValidationModule configure(BeanValidationFeature feature, boolean enabled) {
        return enabled ? enable(feature) : disable(feature);
    }


    public BeanValidationModule enable(BeanValidationFeature feature) {
        if (features.contains(feature)) {
            return this;
        }
        EnumSet<BeanValidationFeature> newFeatures = EnumSet.copyOf(features);
        newFeatures.add(feature);
        return new BeanValidationModule(validatorFactory, newFeatures);
    }


    public BeanValidationModule disable(BeanValidationFeature feature) {
        if (!features.contains(feature)) {
            return this;
        }
        EnumSet<BeanValidationFeature> newFeatures = EnumSet.copyOf(features);
        newFeatures.remove(feature);
        return new BeanValidationModule(validatorFactory, newFeatures);
    }


    @Override
    public void setupModule(SetupContext context) {

        BeanValidationFeatureSet featureSet = new BeanValidationFeatureSet(features);

        context.addBeanDeserializerModifier(new ValidationBeanDeserializerModifier(validatorFactory, featureSet));

        context.addDeserializationProblemHandler(new DeserializationProblemHandler() {
            @Override
            public Object handleInstantiationProblem(DeserializationContext ctxt, Class<?> instClass,
                                                     Object argument, Throwable t) throws IOException {

                if (t instanceof ConstraintViolationException) {
                    throw (ConstraintViolationException) t;
                } else {
                    return super.handleInstantiationProblem(ctxt, instClass, argument, t);
                }
            }
        });
    }
}
