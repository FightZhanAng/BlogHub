package com.blog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI blogHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BlogHub API")
                        .description("BlogHub 技术博客系统 RESTful API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BlogHub")
                                .url("https://github.com/bloghub"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
