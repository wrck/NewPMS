package com.vibe.es;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

/**
 * ES 查询 DSL 构造工具
 *
 * <p>为项目/设备/工单列表检索场景构造 query DSL JSON，封装 bool + must(multi_match) + filter(term) 模式，
 * 供 Controller 在 useEs=true 时调用 {@link ElasticSearchService#search} 使用。</p>
 *
 * <p>所有字段名与 {@code com.vibe.es.index} 下索引 POJO 的属性名一致（ES 默认按属性名建字段）。</p>
 *
 * @author vibe
 */
public final class EsQueryHelper {

    private EsQueryHelper() {
    }

    /**
     * 构造项目检索 DSL（支持 keyword 多字段匹配 + status/pmId/region/productLine 过滤）。
     *
     * @param keyword     关键字（匹配 name/customerName/projectCode，可为 null）
     * @param status      项目状态（可为 null）
     * @param pmId        项目经理ID（可为 null）
     * @param region      区域（可为 null）
     * @param productLine 产品线（可为 null）
     * @return query DSL JSON 字符串
     */
    public static String buildProjectQuery(String keyword, String status, Long pmId,
                                            String region, String productLine) {
        JSONObject bool = new JSONObject();
        JSONArray must = new JSONArray();
        if (keyword != null && !keyword.isBlank()) {
            JSONArray fields = new JSONArray();
            fields.add("name");
            fields.add("customerName");
            fields.add("projectCode");
            JSONObject mm = new JSONObject()
                    .set("query", keyword)
                    .set("fields", fields);
            must.add(new JSONObject().set("multi_match", mm));
        }
        bool.set("must", must);
        JSONArray filter = new JSONArray();
        addTerm(filter, "status", status);
        addTerm(filter, "pmId", pmId);
        addTerm(filter, "region", region);
        addTerm(filter, "productLine", productLine);
        bool.set("filter", filter);
        return new JSONObject().set("query", new JSONObject().set("bool", bool)).toString();
    }

    /**
     * 构造设备检索 DSL（支持 keyword 匹配 sn/macAddress/modelName + status/projectId 过滤）。
     *
     * @param keyword   关键字（匹配 sn/macAddress/modelName，可为 null）
     * @param status    设备状态（可为 null）
     * @param projectId 项目ID（可为 null）
     * @return query DSL JSON 字符串
     */
    public static String buildDeviceQuery(String keyword, String status, Long projectId) {
        JSONObject bool = new JSONObject();
        JSONArray must = new JSONArray();
        if (keyword != null && !keyword.isBlank()) {
            JSONArray fields = new JSONArray();
            fields.add("sn");
            fields.add("macAddress");
            fields.add("modelName");
            JSONObject mm = new JSONObject()
                    .set("query", keyword)
                    .set("fields", fields);
            must.add(new JSONObject().set("multi_match", mm));
        }
        bool.set("must", must);
        JSONArray filter = new JSONArray();
        addTerm(filter, "status", status);
        addTerm(filter, "projectId", projectId);
        return new JSONObject().set("query", new JSONObject().set("bool", bool)).toString();
    }

    /**
     * 构造工单检索 DSL（支持 keyword 匹配 taskName/projectName/engineerName + status/projectId/engineerId 过滤）。
     *
     * @param keyword    关键字（可为 null）
     * @param status     工单状态（可为 null）
     * @param projectId  项目ID（可为 null）
     * @param engineerId 工程师ID（可为 null）
     * @return query DSL JSON 字符串
     */
    public static String buildWorkOrderQuery(String keyword, String status, Long projectId, Long engineerId) {
        JSONObject bool = new JSONObject();
        JSONArray must = new JSONArray();
        if (keyword != null && !keyword.isBlank()) {
            JSONArray fields = new JSONArray();
            fields.add("taskName");
            fields.add("projectName");
            fields.add("engineerName");
            JSONObject mm = new JSONObject()
                    .set("query", keyword)
                    .set("fields", fields);
            must.add(new JSONObject().set("multi_match", mm));
        }
        bool.set("must", must);
        JSONArray filter = new JSONArray();
        addTerm(filter, "status", status);
        addTerm(filter, "projectId", projectId);
        addTerm(filter, "engineerId", engineerId);
        return new JSONObject().set("query", new JSONObject().set("bool", bool)).toString();
    }

    /**
     * 向 filter 数组添加 term 条件（值为 null 时跳过）。
     */
    private static void addTerm(JSONArray filter, String field, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String s && s.isBlank()) {
            return;
        }
        filter.add(new JSONObject().set("term", new JSONObject().set(field, value)));
    }
}
