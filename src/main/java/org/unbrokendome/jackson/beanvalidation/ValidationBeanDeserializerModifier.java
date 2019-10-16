package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.impl.FieldProperty;
import com.fasterxml.jackson.databind.deser.impl.MethodProperty;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.type.CollectionType;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;
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
            if (property instanceof MethodProperty) {
                builder.addOrReplaceProperty(new ValidationAwareMethodProperty(property, validator), true);
            } else if (property instanceof FieldProperty) {
                builder.addOrReplaceProperty(new ValidationAwareFieldProperty(property, validator), true);
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
        JsonValidated annotation = beanDesc.getClassAnnotations().get(JsonValidated.class);
        if (annotation != null && deserializer instanceof BeanDeserializerBase) {
            return ValidatingBeanDeserializer.create(
                    (BeanDeserializerBase) deserializer, validatorFactory, features, annotation
            );
        } else {
            return deserializer;
        }
    }


    @Override
    public JsonDeserializer<?> modifyCollectionDeserializer(
            DeserializationConfig config, CollectionType type,
            BeanDescription beanDesc, JsonDeserializer<?> deserializer
    ) {
        return super.modifyCollectionDeserializer(config, type, beanDesc, deserializer);
    }
}
