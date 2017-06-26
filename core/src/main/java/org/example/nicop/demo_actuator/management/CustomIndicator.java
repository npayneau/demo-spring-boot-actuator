package org.example.nicop.demo_actuator.management;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Created by PNPU08861 on 16/05/2017.
 */
@Component
public class CustomIndicator implements HealthIndicator {

    @Override
    public Health health() {
        int errorCode = checkOtherService();
        if (errorCode != 0) {
            return Health.down().withDetail("Error Code", errorCode).withDetail("Detail", "Détail de l'erreur du service").build();
        }
        return Health.up().build();
    }

    private int checkOtherService(){
        //Check service
        return 12;
    }
}