package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import org.unbrokendome.jackson.beanvalidation.path.PathBuilder;
import org.unbrokendome.jackson.beanvalidation.path.PathUtils;
import org.unbrokendome.jackson.beanvalidation.violation.ConstraintViolationUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


class ValidatingValueInstantiator extends AbstractDelegatingValueInstantiator {

    private final ValidatorFactory validatorFactory;
    private final BeanValidationFeatureSet features;
    private final ConstructorValidatorFactory constructorValidatorFactory;
    private boolean validationEnabled = false;


    ValidatingValueInstantiator(
            StdValueInstantiator delegate, ValidatorFactory validatorFactory,
            BeanValidationFeatureSet features,
            @Nullable ConstructorValidatorFactory constructorValidatorFactory
    ) {
        super(delegate);
        this.validatorFactory = validatorFactory;
        this.features = features;
        this.constructorValidatorFactory = constructorValidatorFactory != null
                ? constructorValidatorFactory
                : (factory, constructor, parameterValues) -> factory.getValidator();
    }


    public boolean isValidationEnabled() {
        return validationEnabled;
    }


    void enableValidation(boolean enabled) {
        this.validationEnabled = enabled;
    }


    @Override
    public Object createFromObjectWith(
            DeserializationContext ctxt, SettableBeanProperty[] props, PropertyValueBuffer buffer
    ) throws IOException {

        if (!validationEnabled) {
            return super.createFromObjectWith(ctxt, props, buffer);
        }

        if (getWithArgsCreator() == null) { // sanity-check; caller should check
            return super.createFromObjectWith(ctxt, props, buffer);
        }

        // Call getParameters eagerly to make sure all the violations are known (result will be cached)
        buffer.getParameters(props);

        Map<Integer, Set<ConstraintViolation<?>>> parameterViolations;
        if (buffer instanceof ValidationAwarePropertyValueBuffer) {
            parameterViolations = ((ValidationAwarePropertyValueBuffer) buffer).getParameterViolations();
        } else {
            parameterViolations = Collections.emptyMap();
        }

        return createFromObjectWith(ctxt, props, buffer, parameterViolations);
    }


    private Object createFromObjectWith(
            DeserializationContext ctxt,
            SettableBeanProperty[] props, PropertyValueBuffer buffer,
            Map<Integer, Set<ConstraintViolation<?>>> parameterViolations
    ) throws IOException {

        Set<? extends ConstraintViolation<?>> creatorViolations =
                validateCreatorArgs(props, buffer, parameterViolations);

        throwIfHasViolations(ctxt, parameterViolations, creatorViolations);

        return super.createFromObjectWith(ctxt, props, buffer);
    }


    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) throws IOException {
        if (validationEnabled) {
            return createFromObjectWith(ctxt, args, Collections.emptyMap());
        } else {
            return super.createFromObjectWith(ctxt, args);
        }
    }


    private Object createFromObjectWith(
            DeserializationContext ctxt, Object[] args,
            Map<Integer, Set<ConstraintViolation<?>>> parameterViolations
    ) throws IOException {

        Set<? extends ConstraintViolation<?>> creatorViolations = validateCreatorArgs(args, parameterViolations);

        throwIfHasViolations(ctxt, parameterViolations, creatorViolations);

        return super.createFromObjectWith(ctxt, args);
    }


    private void throwIfHasViolations(
            DeserializationContext ctxt, Map<Integer,
            Set<ConstraintViolation<?>>> parameterViolations,
            Set<? extends ConstraintViolation<?>> creatorViolations
    ) {
        if (!creatorViolations.isEmpty() || !parameterViolations.isEmpty()) {

            Set<ConstraintViolation<?>> allViolations = new LinkedHashSet<>();

            if (!creatorViolations.isEmpty()) {

                for (ConstraintViolation<?> violation : creatorViolations) {
                    Path.Node parameterNode = PathUtils.firstNodeOfKind(violation.getPropertyPath(), ElementKind.PARAMETER);
                    if (parameterNode == null) {
                        continue;
                    }
                    int parameterIndex = parameterNode.as(Path.ParameterNode.class).getParameterIndex();

                    // Only use the violation from the constructor argument if there was no previous violation
                    if (parameterViolations.containsKey(parameterIndex)) {
                        continue;
                    }

                    allViolations.add(
                            mapParameterViolation(violation, parameterNode, parameterIndex, ctxt));
                }
            }

            for (Set<ConstraintViolation<?>> constraintViolations : parameterViolations.values()) {
                allViolations.addAll(constraintViolations);
            }

            throw new ConstraintViolationException(allViolations);
        }
    }


    @Nonnull
    private Set<ConstraintViolation<?>> validateCreatorArgs(
            SettableBeanProperty[] props, PropertyValueBuffer buffer,
            Map<Integer, Set<ConstraintViolation<?>>> parameterViolations
    ) throws IOException {

        if (allParametersHaveViolations(props.length, parameterViolations)) {
            return Collections.emptySet();
        }

        Object[] args = buffer.getParameters(props);
        return validateCreatorArgs(args, parameterViolations);
    }


    @Nonnull
    @SuppressWarnings("unchecked")
    private Set<ConstraintViolation<?>> validateCreatorArgs(
            Object[] args, Map<Integer, Set<ConstraintViolation<?>>> parameterViolations
    ) {
        if (allParametersHaveViolations(args.length, parameterViolations)) {
            return Collections.emptySet();
        }

        Member creatorMember = getWithArgsCreator().getMember();

        if (creatorMember instanceof Constructor) {
            Constructor constructor = (Constructor) creatorMember;
            Validator validator = constructorValidatorFactory.getValidator(validatorFactory, constructor, args);
            return validator.forExecutables().validateConstructorParameters(constructor, args);

        } else if (creatorMember instanceof Method) {
            // Bean validation doesn't support parameter validation for static methods :-(
            return Collections.emptySet();

        } else {
            return Collections.emptySet();
        }
    }


    private boolean allParametersHaveViolations(
            int parameterCount, Map<Integer, Set<ConstraintViolation<?>>> parameterViolations
    ) {
        for (int i = 0; i < parameterCount; i++) {
            if (!parameterViolations.containsKey(i)) {
                return false;
            }
        }
        return true;
    }


    @Nonnull
    private ConstraintViolation<?> mapParameterViolation(
            ConstraintViolation<?> violation,
            Path.Node parameterNode, int parameterIndex,
            DeserializationContext context
    ) {
        SettableBeanProperty[] constructorArguments = getFromObjectArguments(context.getConfig());

        if (features.isEnabled(BeanValidationFeature.MAP_CREATOR_VIOLATIONS_TO_PROPERTY_VIOLATIONS)) {

            String propertyName;

            if (features.isEnabled(BeanValidationFeature.REPORT_BEAN_PROPERTY_PATHS_IN_VIOLATIONS)) {
                propertyName = parameterNode.getName();
            } else {
                propertyName = constructorArguments[parameterIndex].getName();
            }
            Path newPath = PathBuilder.create()
                    .appendBeanNode()
                    .appendProperty(propertyName)
                    .appendPath(PathUtils.dropUntil(violation.getPropertyPath(), ElementKind.PARAMETER))
                    .build();

            return ConstraintViolationUtils.withNewPath(violation, newPath);

        } else if (features.isDisabled(BeanValidationFeature.REPORT_BEAN_PROPERTY_PATHS_IN_VIOLATIONS)) {

            Path newPath = PathBuilder.create()
                    .appendPath(PathUtils.takeUntil(violation.getPropertyPath(), ElementKind.CONSTRUCTOR))
                    .appendParameter(constructorArguments[parameterIndex].getName(), parameterIndex)
                    .appendPath(PathUtils.dropUntil(violation.getPropertyPath(), ElementKind.PARAMETER))
                    .build();
            return ConstraintViolationUtils.withNewPath(violation, newPath);

        } else {
            // no mapping required
            return violation;
        }
    }


    @Override
    public Object createFromString(DeserializationContext ctxt, String value) throws IOException {
        validateSimpleConstructor(_fromStringCreator, value);
        return super.createFromString(ctxt, value);
    }


    @Override
    public Object createFromInt(DeserializationContext ctxt, int value) throws IOException {
        validateSimpleConstructor(_fromIntCreator, value);
        return super.createFromInt(ctxt, value);
    }


    @Override
    public Object createFromLong(DeserializationContext ctxt, long value) throws IOException {
        validateSimpleConstructor(_fromLongCreator, value);
        return super.createFromLong(ctxt, value);
    }


    @Override
    public Object createFromDouble(DeserializationContext ctxt, double value) throws IOException {
        validateSimpleConstructor(_fromDoubleCreator, value);
        return super.createFromDouble(ctxt, value);
    }


    @Override
    public Object createFromBoolean(DeserializationContext ctxt, boolean value) throws IOException {
        validateSimpleConstructor(_fromBooleanCreator, value);
        return super.createFromBoolean(ctxt, value);
    }


    private void validateSimpleConstructor(@Nullable AnnotatedWithParams creator, @Nullable Object value) {
        if (validationEnabled && creator instanceof AnnotatedConstructor) {
            ExecutableValidator validator = validatorFactory.getValidator().forExecutables();
            Set<? extends ConstraintViolation<?>> violations = validator.validateConstructorParameters(
                    ((AnnotatedConstructor) _fromStringCreator).getAnnotated(),
                    new Object[]{value});

            if (violations != null && !violations.isEmpty()) {

                // The value should appear as the actual object in the validation path, we don't want any
                // ".value" or ".arg0" appended to it - so strip it down to the BEAN node
                Path strippedPath = PathBuilder.create().appendBeanNode().build();

                Set<ConstraintViolation<?>> mappedViolations = new LinkedHashSet<>(violations.size());

                for (ConstraintViolation<?> violation : violations) {
                    ConstraintViolation<?> mappedViolation =
                            ConstraintViolationUtils.withNewPath(violation, strippedPath);
                    mappedViolations.add(mappedViolation);
                }

                throw new ConstraintViolationException(mappedViolations);
            }
        }
    }
}
