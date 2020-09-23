package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.util.EnumSet;


public final class BeanValidationModule extends Module {

    private final ValidatorFactory validatorFactory;
    private final EnumSet<BeanValidationFeature> features;
    @Nullable private ConstructorValidatorFactory constructorValidatorFactory;


    public BeanValidationModule(ValidatorFactory validatorFactory) {
        this(validatorFactory, BeanValidationFeature.getDefaultFeatures());
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
        features.add(feature);
        return this;
    }


    public BeanValidationModule disable(BeanValidationFeature feature) {
        features.remove(feature);
        return this;
    }

    public BeanValidationModule setConstructorValidatorFactory(@Nullable ConstructorValidatorFactory factory) {
        this.constructorValidatorFactory = factory;
        return this;
    }


    @Override
    public void setupModule(SetupContext context) {

        BeanValidationFeatureSet featureSet = new BeanValidationFeatureSet(features);

        context.addBeanDeserializerModifier(new ValidationBeanDeserializerModifier(
                validatorFactory, featureSet, constructorValidatorFactory));

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
