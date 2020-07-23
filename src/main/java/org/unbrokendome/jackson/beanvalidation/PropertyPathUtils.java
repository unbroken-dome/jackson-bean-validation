package org.unbrokendome.jackson.beanvalidation;


import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import org.unbrokendome.jackson.beanvalidation.path.PathBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Path;
import java.lang.reflect.Constructor;
import java.util.Collection;


final class PropertyPathUtils {

    private PropertyPathUtils() {
    }


    @Nonnull
    static Path constructPropertyPath(SettableBeanProperty prop, BeanValidationFeatureSet features) {
        return constructPropertyPath(prop, features, null);
    }

    @Nonnull
    static Path constructPropertyPath(SettableBeanProperty prop, BeanValidationFeatureSet features,
                                      @Nullable JsonStreamContext parsingContext) {

        PathBuilder propertyPathBuilder = PathBuilder.create()
                .appendBeanNode();

        String propertyName;
        if (features.isEnabled(BeanValidationFeature.REPORT_BEAN_PROPERTY_PATHS_IN_VIOLATIONS)) {
            propertyName = PropertyUtils.getPropertyNameFromMember(prop.getMember());

        } else {
            propertyName = prop.getName();
        }

        if (parsingContext != null && parsingContext.inArray()) {
            if (parsingContext.hasCurrentIndex()) {
                propertyName += "[" + parsingContext.getCurrentIndex() + "]";
            } else if (parsingContext.getParent().getCurrentValue() instanceof Collection) {
                Collection currentValue = (Collection) parsingContext.getParent().getCurrentValue();
                propertyName += "[" + currentValue.size() + "]";
            }
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
