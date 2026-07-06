package com.vibe.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * ElasticSearch 8.x 客户端配置
 *
 * <p>使用官方推荐的 <b>Elasticsearch Java API Client 8.x</b>
 * （{@code co.elastic.clients:elasticsearch-java}），基于底层
 * {@link RestClient}（low-level REST client）+ {@link RestClientTransport} 适配层，
 * <b>不使用</b> Spring Data ES 的自动装配 {@code RestHighLevelClient}（已废弃）。</p>
 *
 * <p>读取 {@code spring.elasticsearch.*} 配置：</p>
 * <pre>
 * spring:
 *   elasticsearch:
 *     uris: http://localhost:9200
 *     connection-timeout: 5s
 *     socket-timeout: 30s
 *     username: elastic
 *     password: changeme
 * </pre>
 *
 * <p>容错策略：ES 在本地开发环境可暂不启动，{@link RestClient} 仅在调用时才发起连接，
 * Bean 装配阶段不会因 ES 不可用而失败，业务可正常运行（仅检索能力降级到 MySQL）。</p>
 *
 * @author vibe
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.elasticsearch")
public class ElasticSearchConfig {

    /** ES 服务地址列表，如 http://localhost:9200 */
    private List<String> uris = new ArrayList<>();

    /** 连接超时（毫秒），由 Duration 解析，默认 5s */
    private Duration connectionTimeout = Duration.ofSeconds(5);

    /** Socket 读取超时（毫秒），默认 30s */
    private Duration socketTimeout = Duration.ofSeconds(30);

    /** 用户名（可选，ES 8.x 开启 xpack.security 时需要） */
    private String username;

    /** 密码（可选） */
    private String password;

    /**
     * 低层 REST 客户端（被 {@link ElasticsearchClient} 包装使用）。
     *
     * <p>不在此处 ping ES，仅构建 client；连接发生在实际调用时，
     * 因此 ES 未启动时应用启动不受影响。</p>
     *
     * @return RestClient
     */
    @Bean
    public RestClient restClient() {
        if (uris == null || uris.isEmpty()) {
            log.warn("Elasticsearch uris 未配置，使用默认 http://localhost:9200");
            uris = new ArrayList<>();
            uris.add("http://localhost:9200");
        }

        HttpHost[] hosts = uris.stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(hosts)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(Math.toIntExact(connectionTimeout.toMillis()))
                        .setSocketTimeout(Math.toIntExact(socketTimeout.toMillis())));

        // 启用 basic auth（仅当配置了 username/password）
        if (username != null && !username.isBlank()
                && password != null && !password.isBlank()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        log.info("Elasticsearch RestClient 已构建: uris={}, connectTimeout={}ms, socketTimeout={}ms, authEnabled={}",
                uris, connectionTimeout.toMillis(), socketTimeout.toMillis(),
                username != null && !username.isBlank());
        return builder.build();
    }

    /**
     * Elasticsearch Java API Client（推荐使用的客户端）。
     *
     * <p>通过 {@link RestClientTransport} 桥接低层 {@link RestClient} 与
     * {@link ElasticsearchClient}，使用 Jackson 作为 JSON 序列化器。</p>
     *
     * @param restClient 低层 REST 客户端
     * @return ElasticsearchClient
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();
        ElasticsearchTransport transport = new RestClientTransport(restClient, jsonpMapper);
        ElasticsearchClient client = new ElasticsearchClient(transport);
        log.info("ElasticsearchClient (Java API Client 8.x) 已构建");
        return client;
    }
}
