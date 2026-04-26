package dev.pmlsp.dict.infrastructure.http;

import dev.pmlsp.dict.infrastructure.config.DictClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link RestClient} used by {@link DictHttpGateway}.
 *
 * <p>If {@code dict.mtls.enabled=true} and a bundle name is configured, the {@link SslBundle}
 * resolved by Spring Boot is injected into the request factory — the keystore (private key
 * issued to the participant) and truststore (ICP-Brasil chain) come from {@code spring.ssl.bundle.*}.
 *
 * <p>If mTLS is off (local/simulator), a plain factory is used.
 */
@Slf4j
public final class DictHttpClientFactory {

    private final DictClientProperties props;
    private final SslBundles sslBundles;

    public DictHttpClientFactory(DictClientProperties props, SslBundles sslBundles) {
        this.props = props;
        this.sslBundles = sslBundles;
    }

    public RestClient build() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(props.endpoint().connectTimeout())
                .withReadTimeout(props.endpoint().readTimeout());

        if (props.mtls().enabled() && props.mtls().bundleName() != null) {
            SslBundle bundle = sslBundles.getBundle(props.mtls().bundleName());
            settings = settings.withSslBundle(bundle);
            log.info("dict.http.mtls enabled bundle={}", props.mtls().bundleName());
        } else {
            log.warn("dict.http.mtls disabled — only acceptable for local/simulator profiles");
        }

        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(JdkClientHttpRequestFactory.class, settings);

        return RestClient.builder()
                .baseUrl(props.endpoint().baseUrl())
                .requestFactory(factory)
                .build();
    }
}
