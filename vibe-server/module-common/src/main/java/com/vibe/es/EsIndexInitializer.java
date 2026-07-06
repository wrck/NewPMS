package com.vibe.es;

import com.vibe.es.index.EsIndexConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * ES 索引初始化器
 *
 * <p>应用启动时检查并创建 3 个核心索引（{@code vibe_project}/{@code vibe_device}/{@code vibe_work_order}），
 * 若索引已存在则跳过。索引 mapping 集中声明在本类的常量中，便于版本演进。</p>
 *
 * <p>容错策略：ES 不可达时仅记日志，不影响应用启动；后续调用会由 {@link ElasticSearchService}
 * 统一捕获异常并降级到 MySQL。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class EsIndexInitializer implements ApplicationRunner {

    private final ElasticSearchService<?> elasticSearchService;

    /** 项目索引 mapping（text 字段使用 ik_max_word 中文分词，keyword 字段用于聚合/精确匹配） */
    private static final String PROJECT_MAPPING = """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
              },
              "mappings": {
                "properties": {
                  "id":         { "type": "long" },
                  "name":        { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "projectCode":{ "type": "keyword" },
                  "customerName":{ "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "productLine":{ "type": "keyword" },
                  "region":     { "type": "keyword" },
                  "status":     { "type": "keyword" },
                  "pmId":       { "type": "long" },
                  "phase":      { "type": "keyword" },
                  "createdAt":  { "type": "date", "format": "yyyy-MM-dd'T'HH:mm:ss||epoch_millis" }
                }
              }
            }
            """;

    /** 设备索引 mapping */
    private static final String DEVICE_MAPPING = """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
              },
              "mappings": {
                "properties": {
                  "id":         { "type": "long" },
                  "sn":         { "type": "keyword" },
                  "macAddress": { "type": "keyword" },
                  "modelName":  { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "projectId":  { "type": "long" },
                  "projectName":{ "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "status":     { "type": "keyword" },
                  "warehouse":  { "type": "keyword" },
                  "region":     { "type": "keyword" },
                  "installedAt":{ "type": "date", "format": "yyyy-MM-dd'T'HH:mm:ss||epoch_millis" }
                }
              }
            }
            """;

    /** 工单索引 mapping */
    private static final String WORK_ORDER_MAPPING = """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
              },
              "mappings": {
                "properties": {
                  "id":          { "type": "long" },
                  "taskName":    { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "projectId":   { "type": "long" },
                  "projectName": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "engineerId":  { "type": "long" },
                  "engineerName":{ "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "status":      { "type": "keyword" },
                  "plannedStart":{ "type": "date", "format": "yyyy-MM-dd'T'HH:mm:ss||epoch_millis" },
                  "plannedEnd":  { "type": "date", "format": "yyyy-MM-dd'T'HH:mm:ss||epoch_millis" },
                  "actualEnd":   { "type": "date", "format": "yyyy-MM-dd'T'HH:mm:ss||epoch_millis" }
                }
              }
            }
            """;

    @Override
    public void run(ApplicationArguments args) {
        log.info("ES 索引初始化开始...");
        tryCreate(EsIndexConstant.INDEX_VIBE_PROJECT, PROJECT_MAPPING);
        tryCreate(EsIndexConstant.INDEX_VIBE_DEVICE, DEVICE_MAPPING);
        tryCreate(EsIndexConstant.INDEX_VIBE_WORK_ORDER, WORK_ORDER_MAPPING);
        log.info("ES 索引初始化完成");
    }

    /**
     * 尝试创建索引（ES 不可达时仅记日志）。
     *
     * @param indexName  索引名
     * @param mappingJson mapping JSON
     */
    private void tryCreate(String indexName, String mappingJson) {
        try {
            if (elasticSearchService.indexExists(indexName)) {
                log.info("ES 索引已存在，跳过创建: index={}", indexName);
                return;
            }
            boolean ok = elasticSearchService.createIndex(indexName, mappingJson);
            log.info("ES 索引创建结果: index={}, success={}", indexName, ok);
        } catch (Exception e) {
            log.warn("ES 索引初始化失败（ES 可能未启动，业务降级 MySQL）: index={}, error={}",
                    indexName, e.getMessage());
        }
    }
}
