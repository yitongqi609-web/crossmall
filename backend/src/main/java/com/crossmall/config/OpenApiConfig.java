package com.crossmall.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置(支持 Authorize 里填 Bearer token)
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_NAME = "Authorization";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CrossMall · 潮汐全球购 API")
                        .description("跨境电商独立站:多币种/关税/国际物流/履约状态机/支付幂等")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(SECURITY_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(SECURITY_NAME)))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_NAME));
    }
}
