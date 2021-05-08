package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.deser.impl.PropertyBasedCreator;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.unbrokendome.jackson.beanvalidation.path.PathBuilder;
import org.unbrokendome.jackson.beanvalidation.violation.ConstraintViolationUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.*;
import javax.validation.constraints.NotNull;
import javax.validation.metadata.ConstraintDescriptor;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


class ValidatingBeanDeserializer extends BeanDeserializer {

    protected final ValidatorFactory validatorFactory;
    protected final BeanValidationFeatureSet features;
    private final Validator validator;
    private final MessageInterpolator messageInterpolator;
    @Nullable
    private final JsonValidated validationAnnotation;


    private static final ThreadLocal<Stack<JsonValidated>> TL_VALIDATION_ANNOTATION =
            ThreadLocal.withInitial(Stack::new);


    ValidatingBeanDeserializer(
            BeanDeserializerBase src, ValidatorFactory validatorFactory,
            BeanValidationFeatureSet features, @Nullable JsonValidated validationAnnotation
    ) {
        super(src);
        this.validatorFactory = validatorFactory;
        this.validator = validatorFactory.getValidator();
        this.messageInterpolator = validatorFactory.getMessageInterpolator();
        this.features = features;
        this.validationAnnotation = validationAnnotation;

        if (validationAnnotation != null) {
            ValueInstantiator valueInstantiator = getValueInstantiator();
            if (valueInstantiator instanceof ValidatingValueInstantiator) {
                ((ValidatingValueInstantiator) valueInstantiator).enableValidation(true);
            }

            properties().forEachRemaining(property -> {
                if (property instanceof ValidationAwareBeanProperty<?>) {
                    ((ValidationAwareBeanProperty<?>) property).enableValidation();
                }
            });
        }
    }


    static ValidatingBeanDeserializer create(
            BeanDeserializerBase src, ValidatorFactory validatorFactory,
            BeanValidationFeatureSet features, @Nullable JsonValidated validationAnnotation
    ) {
        if (KotlinDetector.isKotlinType(src.handledType())) {
            return new KotlinValidatingBeanDeserializer(src, validatorFactory, features, validationAnnotation);
        }

        return new ValidatingBeanDeserializer(src, validatorFactory, features, validationAnnotation);
    }


    @Override
    public JsonDeserializer<?> createContextual(
            DeserializationContext ctxt, @Nullable BeanProperty property
    ) throws JsonMappingException {

        BeanDeserializerBase deser = (BeanDeserializerBase) super.createContextual(ctxt, property);

        if (property == null) {
            // at the root level, no need to contextualize
            return deser;
        }

        JsonValidated validationAnnotation = property.getAnnotation(JsonValidated.class);
        if (validationAnnotation == null) {
            validationAnnotation = this.validationAnnotation;
        }
        if (validationAnnotation == null && property.getAnnotation(Valid.class) != null) {
            Stack<JsonValidated> validationAnnotations = TL_VALIDATION_ANNOTATION.get();
            validationAnnotation = validationAnnotations.isEmpty() ? null : validationAnnotations.peek();
        }

        if (validationAnnotation != null) {
            return create(deser, validatorFactory, features, validationAnnotation);
        } else {
            return deser;
        }
    }


    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {

        if (this.validationAnnotation != null) {
            Stack<JsonValidated> validationAnnotations = TL_VALIDATION_ANNOTATION.get();
            validationAnnotations.push(this.validationAnnotation);

            try {
                super.resolve(ctxt);
            } finally {
                validationAnnotations.pop();
            }
        } else {
            super.resolve(ctxt);
        }
    }


    /**
     * Main deserialization method for bean-based objects (POJOs).
     */
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        if (validationAnnotation == null) {
            return super.deserialize(p, ctxt);
        }

        Object bean = _deserialize(p, ctxt);

        if (bean != null && features.isEnabled(BeanValidationFeature.VALIDATE_BEAN_AFTER_CONSTRUCTION)) {
            Set<? extends ConstraintViolation<?>> violations = validator.validate(bean);
            if (violations != null && !violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        return bean;
    }


    private Object _deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
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

        if (validationAnnotation == null) {
            return super.deserialize(p, ctxt, bean);
        }

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

        if (bean instanceof InvalidObject) {

            Class<?> beanType = ((InvalidObject) bean).getType();

            // If we haven't instantiated the bean, we can only guess that unset properties would be
            // left to the default value for the Java type (e.g. 0 for ints and null for reference types)
            for (String propNameToValidate : propsToValidate) {
                SettableBeanProperty prop = _beanProperties.find(propNameToValidate);
                Object value = prop.getNullValueProvider().getNullValue(ctxt);

                Set<? extends ConstraintViolation<?>> propertyViolations = _validateValue(beanType, prop, value);

                if (!propertyViolations.isEmpty()) {
                    if (violations == null) {
                        violations = new HashSet<>();
                    }
                    violations.addAll(propertyViolations);
                }
            }

        } else {
            // if we have an actual bean, we can validate the properties directly
            for (String propNameToValidate : propsToValidate) {
                violations = _validateProperty(bean, propNameToValidate, violations);
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

                        Object value = _deserializeProperty(p, ctxt, bean, prop);
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
        return validateRemainingProperties(bean, ctxt, propsToValidate, violations);
    }


    @Nullable
    protected Set<ConstraintViolation<?>> _validateProperty(
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


    protected Set<? extends ConstraintViolation<?>> _validateValue(
            Class<?> beanType, SettableBeanProperty prop, @Nullable Object value
    ) {
        String beanPropertyName = PropertyUtils.getPropertyNameFromMember(prop.getMember());
        return validator.validateValue(beanType, beanPropertyName, value);
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

        if (validationAnnotation == null) {
            return super._deserializeUsingPropertyBased(p, ctxt);
        }

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
                    value = _deserializeProperty(p, ctxt, null, creatorProp);

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
                buffer.bufferProperty(prop, _deserializeProperty(p, ctxt, null, prop));
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
    protected Object _deserializeProperty(
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

            Path propertyBasePath = PropertyPathUtils.constructPropertyPath(prop, features, p.getParsingContext());
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
        assert validationAnnotation != null;

        Object invalidValue;
        ConstraintDescriptor<?> constraintDescriptor;

        if (features.isEnabled(BeanValidationFeature.REPORT_NULL_PRIMITIVE_AS_NOTNULL_VIOLATION) &&
                p.currentToken() == JsonToken.VALUE_NULL &&
                prop.getType().isPrimitive()) {

            constraintDescriptor = createNotNullConstraintDescriptor(prop);
            invalidValue = null;

        } else {

            JsonValidInput constraintAnnotation = prop.getAnnotation(JsonValidInput.class);
            if (constraintAnnotation == null) {
                constraintAnnotation = JsonConstraints.validInput(validationAnnotation.validInputMessage());
            }
            constraintDescriptor = new JsonValidInputConstraintDescriptor(constraintAnnotation);

            try {
                invalidValue = p.getText();
            } catch (IOException ex) {
                invalidValue = null;
            }
        }

        return ConstraintViolationUtils.create(
                bean, (Class) handledType(), null,
                PropertyPathUtils.constructPropertyPath(prop, features, p.getParsingContext()),
                invalidValue, constraintDescriptor, messageInterpolator);
    }


    @SuppressWarnings("unchecked")
    protected ConstraintViolation<?> _createNotNullViolation(@Nullable Object bean, SettableBeanProperty prop) {
        ConstraintDescriptor<?> constraintDescriptor = createNotNullConstraintDescriptor(prop);
        return ConstraintViolationUtils.create(
                bean, (Class) handledType(), null, PropertyPathUtils.constructPropertyPath(prop, features),
                null, constraintDescriptor, messageInterpolator);
    }


    @Nonnull
    private ConstraintDescriptor<?> createNotNullConstraintDescriptor(SettableBeanProperty prop) {
        NotNull notNullAnnotation = prop.getAnnotation(NotNull.class);
        if (notNullAnnotation == null) {
            notNullAnnotation = JsonConstraints.notNull();
        }
        return new NotNullConstraintDescriptor(notNullAnnotation);
    }
}
