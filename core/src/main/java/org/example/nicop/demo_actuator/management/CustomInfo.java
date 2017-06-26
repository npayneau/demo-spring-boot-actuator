package org.example.nicop.demo_actuator.management;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Created by PNPU08861 on 16/05/2017.
 */
@Component
public class CustomInfo implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("monIndicateur1",
                Collections.singletonMap("key1", "value1"));
        builder.withDetail("monIndicateur2",
                "Ma valeur");
    }
}
