package org.example.nicop.demo_actuator.util;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter.FilterExceptFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter.SerializeExceptFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;

import java.text.MessageFormat;
import java.util.*;

import static java.util.Collections.emptySet;
import static org.springframework.http.HttpStatus.*;



/**
 * Gère la réponse. Elle peut être de plusieurs types :
 *
 *  Object
 *  ResponseEntity<Object>
 *  Page<Object>
 *  ResponseEntity<Page<Object>>
 *  Collection<Object>
 *  ResponseEntity<Collection<Object>>
 *  Exception
 *
 */
public final class ResponseWrapperProcessor {


    private static final String PARTIAL_RESPONSE_INCLUDED_PARAM = "fields";
    private static final String PARTIAL_RESPONSE_EXCLUDED_PARAM = "excluded_fields";

    private static final String PARTIAL_RESPONSE_SEPARATOR = ",";

    @Getter
    private HttpStatus status;

    @Getter
    private HttpHeaders responseHeaders = new HttpHeaders();

    private ResponseEntity wrappingEntity;
    private Page wrappingPage;

    private Map<String, String> pathParams;

    private Object body;

    @Getter
    private ResponseWrapper responseWrapper;

    @Getter
    private MappingJacksonValue mappingJacksonValue;

    public ResponseWrapperProcessor(Object body, Map<String, String> pathParams) {
        this.pathParams = pathParams;
        this.body = body;
    }

    public void process() {
        try {

            unwrapFromResponseEntity();

            unwrapFromPage();

            wrapResponseBody();

            filterPartialResponse();

            determineResponseHeaders();

            determineStatusCode();

        } catch(RestServiceException bde) {
            wrapError(bde.getErrors());
            responseHeaders.clear();
            status = bde.getErrors().getStatus();
        }
    }

    private void unwrapFromResponseEntity() {
        if(body instanceof ResponseEntity) {
            wrappingEntity = (ResponseEntity) body;
            body = wrappingEntity.getBody();
        }
    }

    private void unwrapFromPage() {
        if(isPaged()) {
            wrappingPage = ((Page<?>) body);
            body = wrappingPage.getContent();
        }
    }

    private void wrapResponseBody() {
        responseWrapper = new ResponseWrapper();

        if(isErrorMessage()) {
            wrapError((RestErrorList)body);
        } else {
            responseWrapper.setData(body);
            if(hasPageInformation()) {
                Map<String, String> metadata= new HashMap<>();
                metadata.put("page", Integer.toString(wrappingPage.getNumber()));
                metadata.put("size", Integer.toString(wrappingPage.getSize()));
                metadata.put("total_elements", Long.toString(wrappingPage.getTotalElements()));
                metadata.put("total_pages", Integer.toString(wrappingPage.getTotalPages()));
                metadata.put("last", Boolean.toString(wrappingPage.isLast()));
                metadata.put("first", Boolean.toString(wrappingPage.isFirst()));
                responseWrapper.setMetadata(metadata);
            }else if(isCollection()) {
                Map<String, String> metadata= new HashMap<>();
                Collection collection = (Collection) body;
                metadata.put("page", "1");
                metadata.put("size", Integer.toString(collection.size()));
                metadata.put("total_elements", Integer.toString(collection.size()));
                metadata.put("total_pages", "1");
                metadata.put("last", "true");
                metadata.put("first", "true");
                responseWrapper.setMetadata(metadata);
            }
        }
    }

    private void wrapError(RestErrorList errors) {
        responseWrapper = new ResponseWrapper();
        responseWrapper.setData(null);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("status", String.valueOf(errors.getStatus()));
        responseWrapper.setMetadata(metadata);
        responseWrapper.setErrors(errors);
    }

    private void filterPartialResponse() {
        mappingJacksonValue = new MappingJacksonValue(responseWrapper);

        assertOnlyOnePartialContentParam();

        FilterProvider filters;

        if(pathParams.containsKey(PARTIAL_RESPONSE_INCLUDED_PARAM)) {
            Set<String> fields = extractFieldsToFilter(PARTIAL_RESPONSE_INCLUDED_PARAM, PARTIAL_RESPONSE_SEPARATOR);
            filters = new ExcludeAllButFilterProvider(fields);
        } else if(pathParams.containsKey(PARTIAL_RESPONSE_EXCLUDED_PARAM)) {
            Set<String> fields = extractFieldsToFilter(PARTIAL_RESPONSE_EXCLUDED_PARAM, PARTIAL_RESPONSE_SEPARATOR);
            filters = new IncludeAllButFilterProvider(fields);
        } else {
            filters = new NoFilterProvider();
        }

        mappingJacksonValue.setFilters(filters);
    }

    private Set<String> extractFieldsToFilter(String param, String separator) {
        String filter = pathParams.get(param);
        String[] fields = filter.split(separator);
        return new HashSet<>(Arrays.asList(fields));
    }

    private void assertOnlyOnePartialContentParam() {
        if(pathParams.containsKey(PARTIAL_RESPONSE_INCLUDED_PARAM) && pathParams.containsKey(PARTIAL_RESPONSE_EXCLUDED_PARAM)) {
            String detail = MessageFormat.format("only one of {0} or {1} should be set in the request", PARTIAL_RESPONSE_INCLUDED_PARAM, PARTIAL_RESPONSE_EXCLUDED_PARAM);
            String msg = "Erreur de requête partielle";
            throw new RestServiceException(BAD_REQUEST, msg, "E000", detail);
        }
    }

    private void determineStatusCode() {
        if(isErrorMessage()) {
            status = ((RestErrorList)body).getStatus();
        } else if(hasStatusCode()) {
            status = wrappingEntity.getStatusCode();
        } else if(hasPageInformation()) {
            status = wrappingPage.getTotalPages() == 1 ? OK : PARTIAL_CONTENT;
        } else {
            status = OK;
        }
    }

    void determineResponseHeaders() {
        if(hasResponseHeaders()) {
            responseHeaders = wrappingEntity.getHeaders();
        }
    }

    boolean isErrorMessage() {
        return body instanceof RestErrorList;
    }

    boolean isPaged() {
        return  body instanceof Page;
    }

    boolean hasStatusCode() {
        return wrappingEntity != null;
    }

    boolean hasResponseHeaders() {
        return wrappingEntity != null;
    }

    boolean hasPageInformation() {
        return wrappingPage != null;
    }

    boolean isCollection() {
        return body instanceof Collection;
    }


    /*
    /**********************************************************
    /* Sub-classes
    /**********************************************************
    */


    /**
     * Filter provider implementation which defaults to no filter.
     */
    public static class NoFilterProvider extends SimpleFilterProvider {
        @Override
        public PropertyFilter findPropertyFilter(Object filterId, Object valueToFilter) {
            return new SerializeExceptFilter(emptySet());
        }
    }

    /**
     * Filter implementation which serializes all properties except the ones explicitly listed.
     */
    public static class IncludeAllButFilterProvider extends SimpleFilterProvider {
        Set<String> properties;

        IncludeAllButFilterProvider(Set<String> properties) {
            this.properties = properties;
        }

        @Override
        public PropertyFilter findPropertyFilter(Object filterId, Object valueToFilter) {
            return new SerializeExceptFilter(properties);
        }
    }

    /**
     * Filter implementation which filters out all properties and only serializes ones explicitly listed.
     */
    public static class ExcludeAllButFilterProvider extends SimpleFilterProvider {
        Set<String> properties;

        ExcludeAllButFilterProvider(Set<String> properties) {
            this.properties = properties;
        }

        @Override
        public PropertyFilter findPropertyFilter(Object filterId, Object valueToFilter) {
            return new FilterExceptFilter(properties);
        }
    }

}
