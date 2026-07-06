package com.vibe.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * ElasticSearch 通用服务
 *
 * <p>封装索引创建、批量写入、单条写入、全文检索、聚合查询、按 ID 删除等通用操作。
 * 业务模块通过注入本服务即可完成对 ES 索引的基础操作，无需重复编写客户端调用代码。</p>
 *
 * <p>容错策略：所有 ES 调用均捕获异常并记日志，仅返回安全默认值（空列表/0/false），
 * 避免因 ES 不可用导致业务接口 500 错误。检索失败时由 Controller 兜底回退 MySQL。</p>
 *
 * @param <T> 索引文档类型
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchService<T> {

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 创建索引（带 mapping JSON）。
     *
     * <p>{@code mappingJson} 为完整的索引创建请求体 JSON，可包含 {@code mappings} 与 {@code settings}，
     * 形如：</p>
     * <pre>
     * {
     *   "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
     *   "mappings": {
     *     "properties": {
     *       "name": { "type": "text", "analyzer": "ik_max_word" },
     *       "status": { "type": "keyword" }
     *     }
     *   }
     * }
     * </pre>
     *
     * <p>若索引已存在，ES 会返回 resource_already_exists_exception，本方法捕获后视为成功。</p>
     *
     * @param indexName   索引名
     * @param mappingJson 映射/设置 JSON（可为 null 仅创建空索引）
     * @return true 表示创建成功或已存在
     */
    public boolean createIndex(String indexName, String mappingJson) {
        try {
            CreateIndexRequest.Builder requestBuilder = new CreateIndexRequest.Builder()
                    .index(indexName);
            if (mappingJson != null && !mappingJson.isBlank()) {
                // 直接以 JSON 形式注入 mappings + settings
                requestBuilder = requestBuilder.withJson(new StringReader(mappingJson));
                // 显式覆盖 index 名（withJson 可能未设置）
                requestBuilder = requestBuilder.index(indexName);
            }
            CreateIndexResponse response = elasticsearchClient.indices()
                    .create(requestBuilder.build());
            log.info("ES 索引创建成功: index={}, acknowledged={}",
                    indexName, response.acknowledged());
            return Boolean.TRUE.equals(response.acknowledged());
        } catch (Exception e) {
            // 索引已存在时 ES 返回 resource_already_exists_exception
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("resource_already_exists_exception")
                    || msg.contains("already exists")) {
                log.info("ES 索引已存在，跳过创建: index={}", indexName);
                return true;
            }
            log.error("ES 索引创建失败: index={}, error={}", indexName, msg, e);
            return false;
        }
    }

    /**
     * 批量写入文档（按 ID upsert）。
     *
     * @param indexName 索引名
     * @param docs      文档列表
     * @return 成功写入条数
     */
    public int bulkIndex(String indexName, List<T> docs) {
        if (docs == null || docs.isEmpty()) {
            return 0;
        }
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (T doc : docs) {
                Object id = extractId(doc);
                // 显式使用 IndexOperation.Builder<Object> 避免 BulkOperation.Builder.index(lambda)
                // 中通配符捕获导致的类型不兼容问题（TDocument 与 T 无法一致）
                IndexOperation.Builder<Object> idxBuilder = new IndexOperation.Builder<Object>()
                        .index(indexName)
                        .document(doc);
                if (id != null) {
                    idxBuilder.id(String.valueOf(id));
                }
                bulkBuilder.operations(op -> op.index(idxBuilder.build()));
            }
            BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());
            int success = 0;
            List<BulkResponseItem> items = response.items();
            if (items != null) {
                for (BulkResponseItem item : items) {
                    String r = item.result();
                    if (r != null && !r.isBlank()) {
                        success++;
                    }
                }
            }
            if (response.errors()) {
                log.warn("ES 批量写入部分失败: index={}, total={}, success={}, errors=true",
                        indexName, docs.size(), success);
            } else {
                log.info("ES 批量写入成功: index={}, total={}, success={}",
                        indexName, docs.size(), success);
            }
            return success;
        } catch (Exception e) {
            log.error("ES 批量写入异常: index={}, size={}, error={}",
                    indexName, docs.size(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 单条写入文档（按 ID upsert）。
     *
     * @param indexName 索引名
     * @param id        文档 ID（可为 null，由 ES 自动生成）
     * @param doc       文档对象
     * @return true 表示写入成功
     */
    public boolean index(String indexName, String id, T doc) {
        if (doc == null) {
            return false;
        }
        try {
            var response = elasticsearchClient.index(idx -> {
                idx.index(indexName).document(doc);
                if (id != null && !id.isBlank()) {
                    idx.id(id);
                }
                return idx;
            });
            log.debug("ES 单条写入成功: index={}, id={}, result={}",
                    indexName, id, response.result());
            return true;
        } catch (Exception e) {
            log.error("ES 单条写入异常: index={}, id={}, error={}",
                    indexName, id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 全文检索（基于 query DSL JSON）。
     *
     * @param indexName 索引名
     * @param query     查询 DSL JSON（如 {@code {"query":{"multi_match":{"query":"abc","fields":["name","customerName"]}}}}）
     * @param from      分页起始偏移
     * @param size      分页大小
     * @param docClass  文档类型（用于反序列化）
     * @return 命中文档列表
     */
    public List<T> search(String indexName, String query, int from, int size, Class<T> docClass) {
        try {
            SearchResponse<T> response = elasticsearchClient.search(s -> {
                s.index(indexName).from(from).size(size);
                if (query != null && !query.isBlank()) {
                    // 直接以 JSON 形式注入 query
                    s.withJson(new StringReader(query));
                }
                return s;
            }, docClass);
            List<Hit<T>> hits = response.hits().hits();
            List<T> result = new ArrayList<>(hits == null ? 0 : hits.size());
            if (hits != null) {
                for (Hit<T> hit : hits) {
                    if (hit.source() != null) {
                        result.add(hit.source());
                    }
                }
            }
            log.debug("ES 检索完成: index={}, from={}, size={}, hits={}",
                    indexName, from, size, result.size());
            return result;
        } catch (Exception e) {
            log.error("ES 检索异常: index={}, query={}, error={}",
                    indexName, query, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 聚合查询（基于 aggregation DSL JSON）。
     *
     * @param indexName        索引名
     * @param aggregationJson 聚合 DSL JSON（如 {@code {"size":0,"aggs":{"by_status":{"terms":{"field":"status"}}}}}）
     * @return 原始聚合结果 JSON（透传 ES 响应字符串，由业务自行解析）
     */
    public String aggregate(String indexName, String aggregationJson) {
        try {
            SearchResponse<Void> response = elasticsearchClient.search(s -> {
                s.index(indexName).size(0);
                if (aggregationJson != null && !aggregationJson.isBlank()) {
                    s.withJson(new StringReader(aggregationJson));
                }
                return s;
            }, Void.class);
            // 透传 ES 原始 JSON 响应（含 aggregations 字段）
            return response.toString();
        } catch (Exception e) {
            log.error("ES 聚合查询异常: index={}, error={}", indexName, e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * 按 ID 删除文档。
     *
     * @param indexName 索引名
     * @param id         文档 ID
     * @return true 表示删除成功（包括 NotFound 也视为成功，幂等）
     */
    public boolean deleteById(String indexName, String id) {
        try {
            var response = elasticsearchClient.delete(d -> d
                    .index(indexName)
                    .id(id));
            log.debug("ES 删除完成: index={}, id={}, result={}",
                    indexName, id, response.result());
            return true;
        } catch (Exception e) {
            log.error("ES 删除异常: index={}, id={}, error={}",
                    indexName, id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查索引是否存在。
     *
     * @param indexName 索引名
     * @return true 表示存在
     */
    public boolean indexExists(String indexName) {
        try {
            return elasticsearchClient.indices()
                    .exists(e -> e.index(indexName)).value();
        } catch (Exception e) {
            log.warn("ES 索引存在性检查失败: index={}, error={}",
                    indexName, e.getMessage());
            return false;
        }
    }

    /**
     * 提取文档的 ID 字段（优先使用 id 字段）。
     *
     * @param doc 文档对象
     * @return ID 字符串（无法提取时返回 null）
     */
    private Object extractId(T doc) {
        try {
            // 优先尝试反射读取 id 字段
            java.lang.reflect.Field idField = doc.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object id = idField.get(doc);
            return id != null ? id : null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 无 id 字段时返回 null，由 ES 自动生成 _id
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
