package org.example.nicop.demo_actuator.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
@RequestMapping("public") // TODO A ADAPTER SELON LE PROJET
@Slf4j
public class PublicController {

    @GetMapping(value = "hello", produces = APPLICATION_JSON_UTF8_VALUE)
    public WelcomeMessage helloworld() {
        log.info("Hello World !");
        return new WelcomeMessage("Hello World !");
    }

    @Data
    @AllArgsConstructor
    static class WelcomeMessage {
        String value;
    }

}
