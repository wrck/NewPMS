package com.vibe.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Jackson 全局序列化配置
 *
 * <p>核心目的：解决雪花算法 Long 主键经 JSON 数字下发到前端时的 JavaScript 精度丢失问题。</p>
 *
 * <p>背景：JS Number 安全整数上限为 2^53 = 9007199254740991（16 位），
 * 而雪花 ID 通常是 18-19 位，超限后末位会被置 0，导致前端拿到错误 id 回传后端查无此记录。</p>
 *
 * <p>处理策略：</p>
 * <ul>
 *   <li>Long / long：序列化为字符串（如 1901234567890123456L -> "1901234567890123456"）</li>
 *   <li>BigInteger：同上</li>
 *   <li>BigDecimal：保留原值，序列化为字符串避免科学计数法</li>
 * </ul>
 *
 * <p>影响范围：所有通过 Spring MVC ResponseEntity / @ResponseBody 返回的 JSON，
 * 以及 RedisTemplate 的 Jackson2JsonRedisSerializer（因共用同一个 ObjectMapper 配置）。</p>
 *
 * <p>前端配合：id 字段类型从 number 放宽为 string | number，URL 拼接天然支持字符串。</p>
 *
 * @author vibe
 */
@Configuration
public class JacksonConfig {

    /**
     * 注册 Jackson2ObjectMapperBuilderCustomizer，让 Spring Boot 自动装配的 ObjectMapper
     * 应用 Long -> String 序列化。
     *
     * <p>使用 Customizer 而非直接 @Bean ObjectMapper，是为了不破坏 Spring Boot 默认配置
     * （如日期格式、时区、非空包含等 application.yml 中已有的设置）。</p>
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            SimpleModule longModule = new SimpleModule();
            longModule.addSerializer(Long.class, ToStringSerializer.instance);
            longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
            longModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
            // BigDecimal 用字符串避免科学计数法，反序列化时 Jackson 能自动从字符串还原
            longModule.addSerializer(BigDecimal.class, ToStringSerializer.instance);
            builder.modules(longModule);
        };
    }
}
