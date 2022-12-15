package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.FieldProperty;
import com.fasterxml.jackson.databind.deser.impl.MethodProperty;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;


final class ValidationBeanDeserializerModifier extends BeanDeserializerModifier {

    private final ValidatorFactory validatorFactory;
    private final BeanValidationFeatureSet features;


    ValidationBeanDeserializerModifier(ValidatorFactory validatorFactory, BeanValidationFeatureSet features) {
        this.validatorFactory = validatorFactory;
        this.features = features;
    }


    @Override
    public BeanDeserializerBuilder updateBuilder(
            DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder
    ) {
        List<SettableBeanProperty> properties = new ArrayList<>();
        builder.getProperties().forEachRemaining(properties::add);

        Validator validator = validatorFactory.getValidator();

        for (SettableBeanProperty property : properties) {
            if (property instanceof ValidationAwareBeanProperty<?>) {
                continue;
            }
            if (property instanceof MethodProperty) {
                builder.addOrReplaceProperty(new ValidationAwareMethodProperty(property, validator, false), true);
            } else if (property instanceof FieldProperty) {
                builder.addOrReplaceProperty(new ValidationAwareFieldProperty(property, validator, false), true);
            }
        }

        ValueInstantiator valueInstantiator = builder.getValueInstantiator();
        if (valueInstantiator instanceof StdValueInstantiator) {
            builder.setValueInstantiator(new ValidatingValueInstantiator(
                    (StdValueInstantiator) valueInstantiator, validatorFactory, features));
        }

        return builder;
    }


    @Override
    public JsonDeserializer<?> modifyDeserializer(
            DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer
    ) {
        // If deserializer is already a ValidatingBeanDeserializer, no need to modify anything
        if (deserializer instanceof ValidatingBeanDeserializer) {
            return deserializer;
        }

        if (deserializer instanceof BeanDeserializerBase) {

            // If the bean class is annotated with @JsonValidated, construct a validating deserializer
            JsonValidated annotation = beanDesc.getClassAnnotations().get(JsonValidated.class);
            return ValidatingBeanDeserializer.create(
                    (BeanDeserializerBase) deserializer, validatorFactory, features, annotation
            );

        } else {
            return deserializer;
        }
    }
}
