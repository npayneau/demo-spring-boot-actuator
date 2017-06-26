package org.example.nicop.demo_actuator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.core.annotation.AnnotationUtils.getValue;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.StringUtils.*;


@Slf4j
public class SortHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String SORT_PARAMETER = "sort";
    private static final String PROPERTY_DELIMITER = ",";
    private static final Sort DEFAULT_SORT = null;
    private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.ASC;

    private static final String SORT_DEFAULTS_NAME = SortDefault.SortDefaults.class.getSimpleName();
    private static final String SORT_DEFAULT_NAME = SortDefault.class.getSimpleName();

    private Sort fallbackSort = DEFAULT_SORT;


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(Sort.class);
    }

    @Override
    public Sort resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        String parameterValue = webRequest.getParameter(SORT_PARAMETER);

        // No parameter
        if (parameterValue == null) {
            return getDefaultFromAnnotationOrFallback(parameter);
        }

        // Single empty parameter, e.g "sort="
        if (!hasText(parameterValue)) {
            return getDefaultFromAnnotationOrFallback(parameter);
        }

        return parseParameterIntoSort(parameterValue, PROPERTY_DELIMITER);
    }


    /**
     * Parses the given sort expressions into a {@link Sort} instance. The implementation expects the sources to be a
     * concatenation of Strings using the given delimiter.
     *
     * @param source will never be {@literal null}.
     * @param delimiter the delimiter to be used to split up the source elements, will never be {@literal null}.
     * @return
     */
    private Sort parseParameterIntoSort(String source, String delimiter) {

        List<Sort.Order> allOrders = new ArrayList<>();
        for(String element : source.split(delimiter)) {
            allOrders.add(getSortFromProperty(element));
        }
        return allOrders.isEmpty() ? null : new Sort(allOrders);
    }

    /**
     * Parses a sort property into a {@link Sort} instance
     * @return
     */
    private Sort.Order getSortFromProperty(String property) {
        Sort.Direction direction = DEFAULT_DIRECTION;
        if(property.startsWith("-")) {
            direction = Sort.Direction.DESC;
            property = property.substring(1);
        }
        return new Sort.Order(direction, property);
    }

    /**
     * Reads the default {@link Sort} to be used from the given {@link MethodParameter}. Rejects the parameter if both an
     * {@link SortDefault.SortDefaults} and {@link SortDefault} annotation is found as we cannot build a reliable {@link Sort}
     * instance then (property ordering).
     *
     * @param parameter will never be {@literal null}.
     * @return the default {@link Sort} instance derived from the parameter annotations or the configured fallback-sort
     * {@link #setFallbackSort(Sort)}.
     */
    private Sort getDefaultFromAnnotationOrFallback(MethodParameter parameter) {

        SortDefault.SortDefaults annotatedDefaults = parameter.getParameterAnnotation(SortDefault.SortDefaults.class);
        SortDefault annotatedDefault = parameter.getParameterAnnotation(SortDefault.class);

        if (annotatedDefault != null && annotatedDefaults != null) {
            throw new IllegalArgumentException(
                    String.format("Cannot use both @%s and @%s on parameter %s! Move %s into %s to define sorting order!",
                            SORT_DEFAULTS_NAME, SORT_DEFAULT_NAME, parameter.toString(), SORT_DEFAULT_NAME, SORT_DEFAULTS_NAME));
        }

        if (annotatedDefault != null) {
            return appendOrCreateSortTo(annotatedDefault, null);
        }

        if (annotatedDefaults != null) {
            Sort sort = null;
            for (SortDefault currentAnnotatedDefault : annotatedDefaults.value()) {
                sort = appendOrCreateSortTo(currentAnnotatedDefault, sort);
            }
            return sort;
        }

        return fallbackSort;
    }


    /**
     * Creates a new {@link Sort} instance from the given {@link SortDefault} or appends it to the given {@link Sort}
     * instance if it's not {@literal null}.
     *
     * @param sortDefault
     * @param sortOrNull
     * @return
     */
    private Sort appendOrCreateSortTo(SortDefault sortDefault, Sort sortOrNull) {

        String[] fields = getSpecificPropertyOrDefaultFromValue(sortDefault, SORT_PARAMETER);

        if (fields.length == 0) {
            return null;
        }

        Sort sort = new Sort(sortDefault.direction(), fields);
        return sortOrNull == null ? sort : sortOrNull.and(sort);
    }

    /**
     * Returns the value of the given specific property of the given annotation. If the value of that property is the
     * properties default, we fall back to the value of the {@code value} attribute.
     *
     * @param annotation must not be {@literal null}.
     * @param property must not be {@literal null} or empty.
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSpecificPropertyOrDefaultFromValue(Annotation annotation, String property) {

        Object propertyDefaultValue = getDefaultValue(annotation, property);
        Object propertyValue = getValue(annotation, property);

        return (T) (nullSafeEquals(propertyDefaultValue, propertyValue) ? getValue(annotation)
                : propertyValue);
    }

    public void setFallbackSort(Sort fallbackSort) {
        this.fallbackSort = fallbackSort;
    }



}
