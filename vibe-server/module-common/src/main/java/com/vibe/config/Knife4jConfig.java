package com.vibe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Knife4j / OpenAPI 3.0 接口文档配置
 *
 * <p>访问地址：/doc.html</p>
 *
 * @author vibe
 */
@Configuration
public class Knife4jConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer JWT";

    @Bean
    public OpenAPI vibeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vibe 项目管理系统 API 文档")
                        .description("网络设备原厂实施项目管理系统 - 后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("vibe team")
                                .email("dev@vibe.com"))
                        .license(new License().name("Proprietary")))
                // 全局 JWT 鉴权配置
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer 鉴权，输入 Token 值（无需 Bearer 前缀）")))
                .servers(List.of(
                        new io.swagger.v3.oas.models.servers.Server()
                                .url("/")
                                .description("当前环境")));
    }
}
