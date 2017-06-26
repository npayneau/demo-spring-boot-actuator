package org.example.nicop.demo_actuator.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping  // TODO A ADAPTER SELON LE PROJET
@Slf4j
public class ProtectedController {

    @PreAuthorize("hasAuthority('reader')")
    @GetMapping(value = "secured_hello", produces = APPLICATION_JSON_VALUE)
    public WelcomeMessage helloworld() {
        log.info("[Secured] Hello World !");
        return new WelcomeMessage("[Secured] Hello World !");
    }

    @Data
    @AllArgsConstructor
    static class WelcomeMessage {
        String value;
    }

}
