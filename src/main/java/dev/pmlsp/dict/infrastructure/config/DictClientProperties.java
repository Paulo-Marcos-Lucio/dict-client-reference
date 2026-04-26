package dev.pmlsp.dict.infrastructure.config;

import dev.pmlsp.dict.domain.model.PixKeyType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Top-level binding for {@code dict.*} configuration. Uses Java records so each
 * field is constructor-bound and immutable after startup.
 */
@ConfigurationProperties(prefix = "dict")
public record DictClientProperties(
        String participantIspb,
        Endpoint endpoint,
        Cache cache,
        Mtls mtls,
        Simulator simulator) {

    public record Endpoint(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {}

    public record Cache(int maxSize, Ttl ttl) {

        public record Ttl(
                Duration cpf,
                Duration cnpj,
                Duration email,
                Duration phone,
                Duration evp) {

            public Duration forKey(PixKeyType type) {
                return switch (type) {
                    case CPF -> cpf;
                    case CNPJ -> cnpj;
                    case EMAIL -> email;
                    case PHONE -> phone;
                    case EVP -> evp;
                };
            }
        }
    }

    public record Mtls(boolean enabled, String bundleName) {}

    public record Simulator(
            boolean enabled,
            double failureRate,
            Duration latencyJitter,
            int storeCapacity) {}
}
