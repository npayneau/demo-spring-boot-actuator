package org.example.nicop.demo_actuator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;


@Slf4j
public class PageHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {


        private static final String INVALID_DEFAULT_PAGE_SIZE = "Invalid default page size configured for method %s! Must not be less than one!";

        private static final String PAGE_PARAMETER = "page";
        private static final String SIZE_PARAMETER = "size";
        static final Pageable DEFAULT_PAGE_REQUEST = new PageRequest(0, 20);

        private Pageable fallbackPageable = DEFAULT_PAGE_REQUEST;
        private SortHandlerMethodArgumentResolver sortResolver;

        /**
         * Constructs an instance of this resolved with a default {@link SortHandlerMethodArgumentResolver}.
         */
        public PageHandlerMethodArgumentResolver() {
            this(null);
        }

        /**
         * Constructs an instance of this resolver with the specified {@link SortHandlerMethodArgumentResolver}.
         *
         * @param sortResolver The sort resolver to use
         */
        public PageHandlerMethodArgumentResolver(SortHandlerMethodArgumentResolver sortResolver) {
            this.sortResolver = sortResolver == null ? new SortHandlerMethodArgumentResolver() : sortResolver;
        }

        /**
         * Configures the {@link Pageable} to be used as fallback in case no {@link PageableDefault}
         *  (the latter only supported in legacy mode) can be found at the method parameter to be
         * resolved.
         * <p>
         * If you set this to {@literal null}, be aware that you controller methods will get {@literal null} handed into them
         * in case no {@link Pageable} data can be found in the request. Note, that doing so will require you supply bot the
         * page <em>and</em> the size parameter with the requests as there will be no default for any of the parameters
         * available.
         *
         * @param fallbackPageable the {@link Pageable} to be used as general fallback.
         */
        public void setFallbackPageable(Pageable fallbackPageable) {
            this.fallbackPageable = fallbackPageable;
        }

        /**
         * Returns whether the given {@link Pageable} is the fallback one.
         *
         * @param pageable
         * @since 1.9
         * @return
         */
        public boolean isFallbackPageable(Pageable pageable) {
            return this.fallbackPageable.equals(pageable);
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return Pageable.class.equals(parameter.getParameterType());
        }

        @Override
        public Pageable resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

            assertPageableUniqueness(methodParameter);

            Pageable defaultOrFallback = getDefaultFromAnnotationOrFallback(methodParameter);

            String pageString = webRequest.getParameter(PAGE_PARAMETER);
            String pageSizeString = webRequest.getParameter(SIZE_PARAMETER);

            boolean pageAndSizeGiven = StringUtils.hasText(pageString) && StringUtils.hasText(pageSizeString);

            if (!pageAndSizeGiven && defaultOrFallback == null) {
                return null;
            }

            int page = StringUtils.hasText(pageString) ? parseAndLowerBoundary(pageString, 0)
                    : defaultOrFallback.getPageNumber();
            int pageSize = StringUtils.hasText(pageSizeString) ? parseAndLowerBoundary(pageSizeString, 1)
                    : defaultOrFallback.getPageSize();

            Sort sort = sortResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);

            // Default if necessary and default configured
            sort = sort == null && defaultOrFallback != null ? defaultOrFallback.getSort() : sort;

            return new PageRequest(page, pageSize, sort);
        }


        private Pageable getDefaultFromAnnotationOrFallback(MethodParameter methodParameter) {

            if (methodParameter.hasParameterAnnotation(PageableDefault.class)) {
                return getDefaultPageRequestFrom(methodParameter);
            }

            return fallbackPageable;
        }

        private static Pageable getDefaultPageRequestFrom(MethodParameter parameter) {

            PageableDefault defaults = parameter.getParameterAnnotation(PageableDefault.class);

            Integer defaultPageNumber = defaults.page();
            Integer defaultPageSize = getSpecificPropertyOrDefaultFromValue(defaults, "size");

            if (defaultPageSize < 1) {
                Method annotatedMethod = parameter.getMethod();
                throw new IllegalStateException(String.format(INVALID_DEFAULT_PAGE_SIZE, annotatedMethod));
            }

            if (defaults.sort().length == 0) {
                return new PageRequest(defaultPageNumber, defaultPageSize);
            }

            return new PageRequest(defaultPageNumber, defaultPageSize, defaults.direction(), defaults.sort());
        }


    private int parseAndLowerBoundary(String parameter, int lowerLimit) {

        try {
            int parsed = Integer.parseInt(parameter);
            return parsed < lowerLimit ? lowerLimit : parsed;
        } catch (NumberFormatException e) {
            return lowerLimit;
        }
    }

    public static void assertPageableUniqueness(MethodParameter parameter) {

        Method method = parameter.getMethod();

        if (containsMoreThanOnePageableParameter(method)) {
            Annotation[][] annotations = method.getParameterAnnotations();
            assertQualifiersFor(method.getParameterTypes(), annotations);
        }
    }

    /**
     * Returns whether the given {@link Method} has more than one {@link Pageable} parameter.
     *
     * @param method must not be {@literal null}.
     * @return
     */
    private static boolean containsMoreThanOnePageableParameter(Method method) {

        boolean pageableFound = false;

        for (Class<?> type : method.getParameterTypes()) {

            if (pageableFound && type.equals(Pageable.class)) {
                return true;
            }

            if (type.equals(Pageable.class)) {
                pageableFound = true;
            }
        }

        return false;
    }

    /**
     * Asserts that every {@link Pageable} parameter of the given parameters carries an {@link Qualifier} annotation to
     * distinguish them from each other.
     *
     * @param parameterTypes must not be {@literal null}.
     * @param annotations must not be {@literal null}.
     */
    public static void assertQualifiersFor(Class<?>[] parameterTypes, Annotation[][] annotations) {

        Set<String> values = new HashSet<>();

        for (int i = 0; i < annotations.length; i++) {

            if (Pageable.class.equals(parameterTypes[i])) {

                Qualifier qualifier = findAnnotation(annotations[i]);

                if (null == qualifier) {
                    throw new IllegalStateException(
                            "Ambiguous Pageable arguments in handler method. If you use multiple parameters of type Pageable you need to qualify them with @Qualifier");
                }

                if (values.contains(qualifier.value())) {
                    throw new IllegalStateException("Values of the user Qualifiers must be unique!");
                }

                values.add(qualifier.value());
            }
        }
    }

    /**
     * Returns a {@link Qualifier} annotation from the given array of {@link Annotation}s. Returns {@literal null} if the
     * array does not contain a {@link Qualifier} annotation.
     *
     * @param annotations must not be {@literal null}.
     * @return
     */
    public static Qualifier findAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Qualifier) {
                return (Qualifier) annotation;
            }
        }
        return null;
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

        Object propertyDefaultValue = AnnotationUtils.getDefaultValue(annotation, property);
        Object propertyValue = AnnotationUtils.getValue(annotation, property);

        return (T) (ObjectUtils.nullSafeEquals(propertyDefaultValue, propertyValue) ? AnnotationUtils.getValue(annotation)
                : propertyValue);
    }

}
