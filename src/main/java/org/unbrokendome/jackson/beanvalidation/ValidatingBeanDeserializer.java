package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.deser.impl.PropertyBasedCreator;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.unbrokendome.jackson.beanvalidation.path.PathBuilder;
import org.unbrokendome.jackson.beanvalidation.violation.ConstraintViolationUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.MessageInterpolator;
import javax.validation.Path;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.ConstraintDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


final class ValidatingBeanDeserializer extends BeanDeserializer {

    private final ValidatorFactory validatorFactory;
    private final BeanValidationFeatureSet features;
    private final Validator validator;
    private final MessageInterpolator messageInterpolator;
    private final JsonValidated validationAnnotation;


    ValidatingBeanDeserializer(
            BeanDeserializerBase src, ValidatorFactory validatorFactory,
            BeanValidationFeatureSet features, JsonValidated validationAnnotation
    ) {
        super(src);
        this.validatorFactory = validatorFactory;
        this.validator = validatorFactory.getValidator();
        this.messageInterpolator = validatorFactory.getMessageInterpolator();
        this.features = features;
        this.validationAnnotation = validationAnnotation;
    }


    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        super.resolve(ctxt);

        List<SettableBeanProperty> toBeWrapped = null;
        List<SettableBeanProperty> wrappedProperties = null;

        // If we have any bean-typed properties annotated with @JsonValidated or @Valid (but not the property type),
        // wrap them so they can use a ValidatingBeanDeserializer too
        for (SettableBeanProperty beanProperty : _beanProperties) {

            JsonValidated annotation = beanProperty.getAnnotation(JsonValidated.class);
            if (annotation == null) {
                // Use @Valid annotation as an alternative, inheriting the JsonValidated from the containing bean
                Valid validAnnotation = beanProperty.getAnnotation(Valid.class);
                if (validAnnotation != null) {
                    annotation = this.validationAnnotation;
                }
            }

            if (annotation != null) {
                JsonDeserializer<Object> valueDeserializer = beanProperty.getValueDeserializer();
                if (valueDeserializer instanceof BeanDeserializerBase &&
                        // no need to change if the property is already using ValidatingBeanDeserializer
                        // (i.e. the type is also annotated)
                        !(valueDeserializer instanceof ValidatingBeanDeserializer)) {

                    if (toBeWrapped == null) {
                        toBeWrapped = new ArrayList<>(_beanProperties.size());
                        wrappedProperties = new ArrayList<>(_beanProperties.size());
                    }
                    toBeWrapped.add(beanProperty);
                    wrappedProperties.add(beanProperty.withValueDeserializer(
                            new ValidatingBeanDeserializer((BeanDeserializerBase) valueDeserializer,
                                    validatorFactory, features, annotation)));
                }
            }
        }

        if (toBeWrapped != null) {
            SettableBeanProperty[] creatorProperties = null;

            if (_propertyBasedCreator != null) {
                creatorProperties = _propertyBasedCreator.properties().toArray(new SettableBeanProperty[0]);
                Arrays.sort(creatorProperties, Comparator.comparing(SettableBeanProperty::getCreatorIndex));
            }

            for (int i = 0, len = toBeWrapped.size(); i < len; i++) {
                _replaceProperty(_beanProperties, creatorProperties,
                        toBeWrapped.get(i), wrappedProperties.get(i));
            }

            if (_propertyBasedCreator != null) {
                assert creatorProperties != null;
                _propertyBasedCreator = PropertyBasedCreator.construct(ctxt, _valueInstantiator,
                        creatorProperties, _beanProperties);
            }
        }
    }


    /**
     * Main deserialization method for bean-based objects (POJOs).
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // common case first
        if (p.isExpectedStartObjectToken()) {
            if (_vanillaProcessing) {
                p.nextToken();
                return vanillaDeserialize(p, ctxt);
            }
            // 23-Sep-2015, tatu: This is wrong at some many levels, but for now... it is
            //    what it is, including "expected behavior".
            p.nextToken();
            if (_objectIdReader != null) {
                return deserializeWithObjectId(p, ctxt);
            }
            return deserializeFromObject(p, ctxt);
        }
        return _deserializeOther(p, ctxt, p.getCurrentToken());
    }


    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object bean) throws IOException {
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        if (_unwrappedPropertyHandler != null) {
            return deserializeWithUnwrapped(p, ctxt, bean);
        }
        if (_externalTypeIdHandler != null) {
            return deserializeWithExternalTypeId(p, ctxt, bean);
        }

        // Keep track of the properties that still need validation.
        // Start with all non-creator properties (bean has already been instantiated, so we don't need to deal with
        // them anymore).
        Set<String> propsToValidate = new HashSet<>(_beanProperties.size());
        for (SettableBeanProperty beanProperty : _beanProperties) {
            if (!(beanProperty instanceof CreatorProperty)) {
                propsToValidate.add(beanProperty.getName());
            }
        }

        String propName;

        // 23-Mar-2010, tatu: In some cases, we start with full JSON object too...
        if (p.isExpectedStartObjectToken()) {
            propName = p.nextFieldName();
            if (propName == null) {
                return validateRemainingProperties(bean, ctxt, propsToValidate, null);
            }

        } else if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
            propName = p.getCurrentName();

        } else {
            return validateRemainingProperties(bean, ctxt, propsToValidate, null);
        }

        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(p, ctxt, bean, view);
            }
        }

        Set<ConstraintViolation<?>> violations = null;
        do {
            p.nextToken();
            SettableBeanProperty prop = _beanProperties.find(propName);

            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(p, ctxt, bean);

                } catch (ConstraintViolationException ex) {
                    if (violations == null) {
                        violations = new HashSet<>();
                    }
                    violations.addAll(ex.getConstraintViolations());

                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);

                } finally {
                    propsToValidate.remove(propName);
                }
                continue;
            }
            handleUnknownVanilla(p, ctxt, bean, propName);
        } while ((propName = p.nextFieldName()) != null);

        return validateRemainingProperties(bean, ctxt, propsToValidate, violations);
    }


    private Object validateRemainingProperties(
            Object bean, DeserializationContext ctxt, Set<String> propsToValidate,
            @Nullable Set<ConstraintViolation<?>> previousViolations
    ) throws JsonMappingException {

        Set<ConstraintViolation<?>> violations = null;
        if (previousViolations != null) {
            violations = new LinkedHashSet<>(previousViolations);
        }

        // There are no more properties in the input; however we still need to validate
        // properties that have not been set explicitly
        if (bean instanceof InvalidObject) {
            Class<?> beanType = ((InvalidObject) bean).getType();
            // If we haven't instantiated the bean, we can only guess that unset properties would be
            // left to the default value for the Java type (e.g. 0 for ints and null for reference types)
            for (String propNameToValidate : propsToValidate) {
                SettableBeanProperty prop = _beanProperties.find(propNameToValidate);
                String beanPropertyName = PropertyUtils.getPropertyNameFromMember(prop.getMember());
                Object value = prop.getNullValueProvider().getNullValue(ctxt);

                Set<? extends ConstraintViolation<?>> propertyViolations =
                        validator.validateValue(beanType, beanPropertyName, value);
                if (!propertyViolations.isEmpty()) {
                    if (violations == null) {
                        violations = new HashSet<>();
                    }
                    violations.addAll(propertyViolations);
                }
            }

        } else {
            // if we have an actual bean, we can validate the properties directly, like in vanilla mode
            for (String propNameToValidate : propsToValidate) {
                violations = validateProperty(bean, propNameToValidate, violations);
            }
        }

        if (violations != null && !violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return bean;
    }


    /**
     * Streamlined version that is only used when no "special"
     * features are enabled.
     */
    private Object vanillaDeserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Set<ConstraintViolation<?>> violations = null;

        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        // [databind#631]: Assign current value, to be accessible by custom serializers
        p.setCurrentValue(bean);

        // Keep track of the properties that still need validation
        Set<String> propsToValidate = StreamSupport.stream(_beanProperties.spliterator(), true)
                .map(SettableBeanProperty::getName)
                .collect(Collectors.toSet());

        if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
            String propName = p.getCurrentName();
            do {
                p.nextToken();
                SettableBeanProperty prop = _beanProperties.find(propName);

                if (prop != null) { // normal case
                    try {
                        if (p.hasToken(JsonToken.VALUE_NULL) &&
                                NullsConstantProvider.isSkipper(prop.getNullValueProvider())) {
                            continue;
                        }

                        Object value = deserializeProperty(p, ctxt, bean, prop);
                        prop.set(bean, value);

                    } catch (ConstraintViolationException ex) {
                        if (violations == null) {
                            violations = new LinkedHashSet<>();
                        }
                        violations.addAll(ex.getConstraintViolations());

                        // We can consider this property fully validated
                        propsToValidate.remove(prop.getName());

                    } catch (Exception e) {
                        wrapAndThrow(e, bean, propName, ctxt);
                    }

                    continue;
                }
                handleUnknownVanilla(p, ctxt, bean, propName);
            } while ((propName = p.nextFieldName()) != null);
        }

        // If there are any properties left that we haven't encountered, they will be left with their
        // default value (or whatever the constructor initialized them with). Since these properties didn't
        // go through deserialization, we have to validate them now.
        if (!propsToValidate.isEmpty()) {
            for (String propName : propsToValidate) {
                violations = validateProperty(bean, propName, violations);
            }
        }

        // TODO handle @AssertValid here?

        if (violations != null && !violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        return bean;
    }


    @Nullable
    private Set<ConstraintViolation<?>> validateProperty(
            Object bean, String propName, @Nullable Set<ConstraintViolation<?>> violationsCollector
    ) {
        SettableBeanProperty prop = _beanProperties.find(propName);
        assert !(prop instanceof CreatorProperty);

        String beanPropertyName = PropertyUtils.getPropertyNameFromMember(prop.getMember());

        Set<ConstraintViolation<Object>> propertyViolations = validator.validateProperty(bean, beanPropertyName);

        if (propertyViolations != null && !propertyViolations.isEmpty()) {
            if (violationsCollector == null) {
                violationsCollector = new LinkedHashSet<>();
            }
            violationsCollector.addAll(propertyViolations);
        }

        // If the property is annotated with @Valid, validateProperty won't cascade, so we have to
        // validate the bean value manually
        if (prop.getAnnotation(Valid.class) != null) {
            Object value = PropertyUtils.getProperty(bean, beanPropertyName);
            if (value != null) {
                Set<ConstraintViolation<Object>> cascadedViolations = validator.validate(value);
                if (cascadedViolations != null && !cascadedViolations.isEmpty()) {
                    if (violationsCollector == null) {
                        violationsCollector = new HashSet<>();
                    }

                    Path propertyBasePath = PathBuilder.create()
                            .appendBeanNode()
                            .appendProperty(propName)
                            .build();

                    for (ConstraintViolation<Object> cascadedViolation : cascadedViolations) {
                        @SuppressWarnings("unchecked")
                        ConstraintViolation<?> resolvedViolation =
                                ConstraintViolationUtils.withBasePath(cascadedViolation,
                                        bean, (Class) handledType(), propertyBasePath);
                        violationsCollector.add(resolvedViolation);
                    }
                }
            }
        }

        return violationsCollector;
    }


    /**
     * Method called to deserialize bean using "property-based creator":
     * this means that a non-default constructor or factory method is
     * called, and then possibly other setters. The trick is that
     * values for creator method need to be buffered, first; and
     * due to non-guaranteed ordering possibly some other properties
     * as well.
     */
    @Override
    @SuppressWarnings("resource")
    protected Object _deserializeUsingPropertyBased(JsonParser p, DeserializationContext ctxt) throws IOException {

        final PropertyBasedCreator creator = _propertyBasedCreator;

        // In addition to the default PropertyValueBuffer we also need to store constraint violations
        ValidationAwarePropertyValueBuffer buffer = new ValidationAwarePropertyValueBuffer(
                p, ctxt, creator.properties().size(), _objectIdReader, this._beanType, features,
                messageInterpolator, validationAnnotation);

        TokenBuffer unknown = null;
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;

        JsonToken t = p.getCurrentToken();

        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            String propName = p.getCurrentName();
            p.nextToken(); // to point to value
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {

                if ((activeView != null) && !creatorProp.visibleInView(activeView)) {
                    p.skipChildren();
                    continue;
                }

                Object value;
                try {
                    value = deserializeProperty(p, ctxt, null, creatorProp);

                } catch (ConstraintViolationException ex) {
                    buffer.assignViolations(creatorProp, ex.getConstraintViolations());

                    // We still need to call buffer.assignParameter with any value to indicate
                    // that this parameter has been processed
                    value = null;
                }

                // Last creator property to set?
                if (buffer.assignParameter(creatorProp, value)) {
                    p.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);

                    } catch (ConstraintViolationException ex) {
                        // Retrieve any violation from the set, to find out which concrete type was
                        // being instantiated
                        ConstraintViolation<?> violation = ex.getConstraintViolations().iterator().next();
                        bean = new InvalidObject(violation.getRootBeanClass(), ex.getConstraintViolations());

                    } catch (Exception e) {
                        bean = wrapInstantiationProblem(e, ctxt);
                    }

                    if (bean == null) {
                        return ctxt.handleInstantiationProblem(handledType(), null,
                                _creatorReturnedNullException());
                    }
                    // [databind#631]: Assign current value, to be accessible by custom serializers
                    p.setCurrentValue(bean);

                    //  polymorphic?
                    if (!(bean instanceof InvalidObject) && bean.getClass() != _beanType.getRawClass()) {
                        return handlePolymorphic(p, ctxt, bean, unknown);
                    }
                    if (unknown != null) { // nope, just extra unknown stuff...
                        bean = handleUnknownProperties(ctxt, bean, unknown);
                    }
                    // or just clean?
                    bean = deserialize(p, ctxt, bean);
                    return unwrapInvalidObject(bean);
                }
                continue;
            }
            // regular property? needs buffering
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                buffer.bufferProperty(prop, deserializeProperty(p, ctxt, null, prop));
            }
            // Things marked as ignorable should not be passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                try {
                    buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(p, ctxt));
                } catch (Exception e) {
                    wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                }
                continue;
            }
            // Ok then, let's collect the whole field; name and value
            if (unknown == null) {
                unknown = new TokenBuffer(p, ctxt);
            }
            unknown.writeFieldName(propName);
            unknown.copyCurrentStructure(p);
        }

        // We hit END_OBJECT, so:
        Object bean = null;
        try {
            bean = creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            assert false; // never gets here
        }
        if (unknown != null) {
            // polymorphic?
            if (bean.getClass() != _beanType.getRawClass()) {
                return handlePolymorphic(null, ctxt, bean, unknown);
            }
            // no, just some extra unknown properties
            return handleUnknownProperties(ctxt, bean, unknown);
        }
        return bean;
    }


    private Object unwrapInvalidObject(Object bean) {
        if (bean instanceof InvalidObject) {
            throw new ConstraintViolationException(((InvalidObject) bean).getViolations());
        } else {
            return bean;
        }
    }


    @SuppressWarnings("unchecked")
    private Object deserializeProperty(
            JsonParser p, DeserializationContext ctxt, @Nullable Object bean, SettableBeanProperty prop
    ) throws IOException {

        Object value = null;
        Set<ConstraintViolation<?>> propertyViolations = Collections.emptySet();

        try {
            value = prop.deserialize(p, ctxt);

            if (!(prop instanceof CreatorProperty)) {
                String beanPropertyName = PropertyUtils.getPropertyNameFromMember(prop.getMember());
                propertyViolations =
                        (Set) validator.validateValue(handledType(), beanPropertyName, value);
            }

        } catch (MismatchedInputException ex) {
            propertyViolations = Collections.singleton(handleMismatchedInput(p, bean, prop));

        } catch (ConstraintViolationException ex) {

            Path propertyBasePath = PropertyPathUtils.constructPropertyPath(prop, features);
            propertyViolations = new LinkedHashSet<>(ex.getConstraintViolations().size());

            for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                ConstraintViolation<?> resolvedViolation = ConstraintViolationUtils.withBasePath(
                        violation, bean, (Class) handledType(), propertyBasePath);
                propertyViolations.add(resolvedViolation);
            }

        } catch (Exception ex) {
            wrapAndThrow(ex, handledType(), prop.getName(), ctxt);
        }

        if (!propertyViolations.isEmpty()) {
            throw new ConstraintViolationException(propertyViolations);
        }
        return value;
    }


    @Nonnull
    @SuppressWarnings("unchecked")
    private ConstraintViolation<?> handleMismatchedInput(
            JsonParser p, @Nullable Object bean, SettableBeanProperty prop
    ) {
        JsonValidInput constraintAnnotation = prop.getAnnotation(JsonValidInput.class);
        if (constraintAnnotation == null) {
            constraintAnnotation = JsonConstraints.validInput(validationAnnotation.validInputMessage());
        }

        ConstraintDescriptor<?> constraintDescriptor =
                new JsonValidInputConstraintDescriptor(constraintAnnotation);

        Object invalidValue;
        try {
            invalidValue = p.getText();
        } catch (IOException ex) {
            invalidValue = null;
        }

        return ConstraintViolationUtils.create(
                bean, (Class) handledType(), null, PropertyPathUtils.constructPropertyPath(prop, features),
                invalidValue, constraintDescriptor, messageInterpolator);
    }
}
