package org.unbrokendome.jackson.beanvalidation;

import java.io.IOException;
import java.util.Set;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidatorFactory;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

public class BeanValidationModule extends Module {

    private final ValidatorFactory validatorFactory;
    private final Set<BeanValidationFeature> features = BeanValidationFeature.getDefaultFeatures();

    public BeanValidationModule(ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    @Override
    public String getModuleName() {
        return "jackson-bean-validation";
    }


    @Override
    public Version version() {
        return Version.unknownVersion();
    }


    @Override
    public void setupModule(SetupContext context) {
        context.addBeanDeserializerModifier(new ValidationBeanDeserializerModifier(validatorFactory, features));

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
