package dev.pmlsp.dict.infrastructure.config;

import dev.pmlsp.dict.domain.policy.CacheTtlPolicy;
import dev.pmlsp.dict.domain.port.out.DictEntryCache;
import dev.pmlsp.dict.domain.port.out.DictGateway;
import dev.pmlsp.dict.infrastructure.cache.CaffeineDictEntryCache;
import dev.pmlsp.dict.infrastructure.cache.RegulatoryCacheTtlPolicy;
import dev.pmlsp.dict.infrastructure.http.DictHttpClientFactory;
import dev.pmlsp.dict.infrastructure.http.DictHttpGateway;
import dev.pmlsp.dict.infrastructure.simulator.InMemoryDictStore;
import dev.pmlsp.dict.infrastructure.simulator.SimulatorBehavior;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

/**
 * Wires together the infrastructure beans that depend on {@link DictClientProperties}.
 * Kept narrow on purpose — anything Spring-specific belongs here, not in the use cases.
 */
@Configuration
public class DictClientAutoConfig {

    @Bean
    CacheTtlPolicy cacheTtlPolicy(DictClientProperties props) {
        return new RegulatoryCacheTtlPolicy(props.cache().ttl());
    }

    @Bean
    DictEntryCache dictEntryCache(DictClientProperties props, MeterRegistry registry) {
        return new CaffeineDictEntryCache(props.cache().maxSize(), registry);
    }

    @Bean
    RestClient dictRestClient(DictClientProperties props, SslBundles sslBundles) {
        return new DictHttpClientFactory(props, sslBundles).build();
    }

    @Bean
    DictGateway dictGateway(RestClient dictRestClient) {
        return new DictHttpGateway(dictRestClient);
    }

    @Bean
    @Profile("simulator")
    InMemoryDictStore inMemoryDictStore() {
        return new InMemoryDictStore();
    }

    @Bean
    @Profile("simulator")
    SimulatorBehavior simulatorBehavior(DictClientProperties props, MeterRegistry registry) {
        return new SimulatorBehavior(props.simulator(), registry);
    }
}
