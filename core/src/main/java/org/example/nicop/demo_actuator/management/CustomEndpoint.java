package org.example.nicop.demo_actuator.management;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by PNPU08861 on 16/05/2017.
 */
@Component
public class CustomEndpoint implements Endpoint<Map<String, String>> {

    public String getId() {
        return "myEndpoint";
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean isSensitive() {
        return true;
    }

    public Map<String, String> invoke() {
        // Custom logic to build the output
        Map<String, String> result =  Collections.singletonMap("key1", "value1");
        result.put("key2", "value2");
        return result;
    }
}