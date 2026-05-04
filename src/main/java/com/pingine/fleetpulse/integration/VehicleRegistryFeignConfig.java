package com.pingine.fleetpulse.integration;

import feign.Retryer;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

public class VehicleRegistryFeignConfig {

    @Bean
    public Retryer registryRetryer() {
        return new Retryer.Default(200, TimeUnit.SECONDS.toMillis(2), 3);
    }

    @Bean
    public ErrorDecoder registryErrorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            if (response.status() >= 500) {
                return new RetryableException(
                        response.status(),
                        "Registry server error " + response.status(),
                        response.request().httpMethod(),
                        null,
                        response.request());
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }
}
