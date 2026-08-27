package dev.hustletech.interview.compustaff.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI compustaffOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Payment Eligibility API")
                .version("v1")
                .description("""
                        Decides whether an account may make a payment. The check does not move money.

                        A rejected payment is a successful answer: it returns 200 with every applicable \
                        reason, ordered by the declaration order of RejectionReason. Only malformed or \
                        unprocessable input returns 4xx.

                        Monetary values are JSON strings with two decimal places, because JSON numbers \
                        are IEEE-754 doubles and lose precision when parsed in the browser."""));
    }

}
