package org.unbrokendome.jackson.beanvalidation;


import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import org.unbrokendome.jackson.beanvalidation.path.PathBuilder;

import javax.annotation.Nonnull;
import javax.validation.Path;
import java.lang.reflect.Constructor;


final class PropertyPathUtils {

    private PropertyPathUtils() {
    }


    @Nonnull
    static Path constructPropertyPath(SettableBeanProperty prop, BeanValidationFeatureSet features) {

        PathBuilder propertyPathBuilder = PathBuilder.create()
                .appendBeanNode();

        String propertyName;
        if (features.isEnabled(BeanValidationFeature.REPORT_BEAN_PROPERTY_PATHS_IN_VIOLATIONS)) {
            propertyName = PropertyUtils.getPropertyNameFromMember(prop.getMember());

        } else {
            propertyName = prop.getName();
        }

        if (prop instanceof CreatorProperty &&
                features.isDisabled(BeanValidationFeature.MAP_CREATOR_VIOLATIONS_TO_PROPERTY_VIOLATIONS)) {
            Constructor<?> constructor = (Constructor<?>) prop.getMember().getMember();
            propertyPathBuilder
                    .appendConstructor(constructor)
                    .appendParameter(propertyName, prop.getCreatorIndex());

        } else {
            propertyPathBuilder
                    .appendProperty(propertyName);
        }

        return propertyPathBuilder.build();
    }
}
