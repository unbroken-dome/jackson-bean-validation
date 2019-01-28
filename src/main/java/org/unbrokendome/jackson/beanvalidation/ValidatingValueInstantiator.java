package org.unbrokendome.jackson.beanvalidation;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import org.unbrokendome.jackson.beanvalidation.path.PathBuilder;
import org.unbrokendome.jackson.beanvalidation.path.PathUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.ConstraintDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public class ValidatingValueInstantiator extends StdValueInstantiator {

    private final ValidatorFactory validatorFactory;


    public ValidatingValueInstantiator(StdValueInstantiator src, ValidatorFactory validatorFactory) {
        super(src);
        this.validatorFactory = validatorFactory;
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
                                        Map<Integer, Set<ConstraintViolation<?>>> propertyViolations)
            throws IOException {

        if (_withArgsCreator == null) { // sanity-check; caller should check
            return super.createFromObjectWith(ctxt, args);
        }

        boolean allParametersHaveViolations = true;
        for (int i = 0; i < args.length; i++) {
            if (!propertyViolations.containsKey(i)) {
                allParametersHaveViolations = false;
                break;
            }
        }

        Set<? extends ConstraintViolation<?>> creatorViolations = null;

        if (!allParametersHaveViolations) {

            Member creatorMember = _withArgsCreator.getMember();
            ExecutableValidator executableValidator = validatorFactory.getValidator().forExecutables();

            if (creatorMember instanceof Constructor) {
                creatorViolations = executableValidator.validateConstructorParameters(
                        (Constructor<?>) creatorMember, args);

            } else if (creatorMember instanceof Method) {
                creatorViolations = executableValidator.validateParameters(
                        _withArgsCreator.getDeclaringClass(),
                        (Method) creatorMember,
                        args);
            }
        }

        if (creatorViolations == null) {
            creatorViolations = Collections.emptySet();
        }

        if (!creatorViolations.isEmpty() || !propertyViolations.isEmpty()) {

            Set<ConstraintViolation<?>> allViolations = new LinkedHashSet<>();

            creatorViolations.forEach(violation -> {
                Path.ParameterNode parameterNode =
                        PathUtils.lastNode(violation.getPropertyPath()).as(Path.ParameterNode.class);

                if (!propertyViolations.containsKey(parameterNode.getParameterIndex())) {

                    String parameterName = this._constructorArguments[parameterNode.getParameterIndex()].getName();

                    ConstraintViolation<?> mappedViolation = ConstraintViolationUtils.copyWithNewPath(violation,
                            PathBuilder.create()
                                    .appendProperty(parameterName)
                                    .build());
                    allViolations.add(mappedViolation);
                }
            });

            propertyViolations.values().forEach(allViolations::addAll);

            throw new ConstraintViolationException(allViolations);
        }

        return super.createFromObjectWith(ctxt, args);
    }


    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) throws IOException {

        return createFromObjectWith(ctxt, args, Collections.emptyMap());
    }
}
