package dev.pmlsp.dict.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI dictClientOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("DICT Client Reference API")
                .description("Reference Java client for the BCB DICT — facade over the use cases")
                .version("0.1.0")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
