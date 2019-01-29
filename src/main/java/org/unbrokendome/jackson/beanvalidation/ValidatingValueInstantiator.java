package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import org.unbrokendome.jackson.beanvalidation.path.PathBuilder;
import org.unbrokendome.jackson.beanvalidation.path.PathUtils;
import org.unbrokendome.jackson.beanvalidation.violation.ConstraintViolationUtils;

import javax.annotation.Nonnull;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
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


class ValidatingValueInstantiator extends StdValueInstantiator {

    private final ValidatorFactory validatorFactory;
    private final BeanValidationFeatureSet features;


    ValidatingValueInstantiator(StdValueInstantiator src, ValidatorFactory validatorFactory,
                                BeanValidationFeatureSet features) {
        super(src);
        this.validatorFactory = validatorFactory;
        this.features = features;
    }


    @Override
    public Object createFromObjectWith(DeserializationContext ctxt,
                                       SettableBeanProperty[] props, PropertyValueBuffer buffer)
            throws IOException {

        if (buffer instanceof ValidationAwarePropertyValueBuffer) {
            return createFromObjectWith(ctxt,
                    buffer.getParameters(props),
                    ((ValidationAwarePropertyValueBuffer) buffer).getParameterViolations());
        }

        return createFromObjectWith(ctxt, buffer.getParameters(props));
    }


    private Object createFromObjectWith(DeserializationContext ctxt, Object[] args,
                                        Map<Integer, Set<ConstraintViolation<?>>> parameterViolations)
            throws IOException {

        if (_withArgsCreator == null) { // sanity-check; caller should check
            return super.createFromObjectWith(ctxt, args);
        }

        Set<? extends ConstraintViolation<?>> creatorViolations = validateCreatorArgs(args, parameterViolations);

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
                            mapParameterViolation(violation, parameterNode, parameterIndex));
                }
            }

            for (Set<ConstraintViolation<?>> constraintViolations : parameterViolations.values()) {
                allViolations.addAll(constraintViolations);
            }

            throw new ConstraintViolationException(allViolations);
        }

        return super.createFromObjectWith(ctxt, args);
    }


    @Nonnull
    @SuppressWarnings("unchecked")
    private Set<ConstraintViolation<?>> validateCreatorArgs(
            Object[] args, Map<Integer, Set<ConstraintViolation<?>>> parameterViolations) {

        if (allParametersHaveViolations(args.length, parameterViolations)) {
            return Collections.emptySet();
        }

        Member creatorMember = _withArgsCreator.getMember();
        ExecutableValidator executableValidator = validatorFactory.getValidator().forExecutables();

        if (creatorMember instanceof Constructor) {
            return (Set) executableValidator.validateConstructorParameters(
                    (Constructor<?>) creatorMember, args);

        } else if (creatorMember instanceof Method) {
            // Bean validation doesn't support parameter validation for static methods :-(
            return Collections.emptySet();

        } else {
            return Collections.emptySet();
        }
    }


    private boolean allParametersHaveViolations(
            int parameterCount, Map<Integer, Set<ConstraintViolation<?>>> parameterViolations) {
        for (int i = 0; i < parameterCount; i++) {
            if (!parameterViolations.containsKey(i)) {
                return false;
            }
        }
        return true;
    }


    @Nonnull
    private ConstraintViolation<?> mapParameterViolation(ConstraintViolation<?> violation,
                                                         Path.Node parameterNode, int parameterIndex) {

        if (features.isEnabled(BeanValidationFeature.MAP_CREATOR_VIOLATIONS_TO_PROPERTY_VIOLATIONS)) {

            String propertyName;

            if (features.isEnabled(BeanValidationFeature.REPORT_BEAN_PROPERTY_PATHS_IN_VIOLATIONS)) {
                propertyName = parameterNode.getName();
            } else {
                propertyName = this._constructorArguments[parameterIndex].getName();
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
                    .appendParameter(_constructorArguments[parameterIndex].getName(), parameterIndex)
                    .appendPath(PathUtils.dropUntil(violation.getPropertyPath(), ElementKind.PARAMETER))
                    .build();
            return ConstraintViolationUtils.withNewPath(violation, newPath);

        } else {
            // no mapping required
            return violation;
        }
    }


    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) throws IOException {
        return createFromObjectWith(ctxt, args, Collections.emptyMap());
    }
}
