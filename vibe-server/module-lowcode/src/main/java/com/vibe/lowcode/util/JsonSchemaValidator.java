package com.vibe.lowcode.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JSON Schema 校验工具（基于 json-schema-validator 1.0.87，Draft 7 规范）
 *
 * <p>用于低代码配置导入时校验 JSON Schema 合法性：</p>
 * <ul>
 *   <li>{@link #isValid(String)}：校验 schema 字符串本身是否为合法的 JSON Schema Draft 7</li>
 *   <li>{@link #validate(String)}：返回 schema 校验错误信息列表（空列表表示合法）</li>
 *   <li>{@link #validateData(String, String)}：用 schema 校验数据 JSON，返回错误信息列表</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
public final class JsonSchemaValidator {

    /** JSON Schema 工厂（Draft 7） */
    private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    /** JSON 解析器 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonSchemaValidator() {}

    /**
     * 校验 schema 字符串是否为合法的 JSON Schema Draft 7。
     *
     * <p>通过尝试加载 schema 判断其合法性：能成功加载即视为合法（语法层面）。</p>
     *
     * @param schemaJson JSON Schema 字符串
     * @return true-合法；false-不合法或为空
     */
    public static boolean isValid(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return false;
        }
        try {
            JsonSchema schema = FACTORY.getSchema(schemaJson);
            return schema != null;
        } catch (Exception e) {
            log.debug("[JsonSchemaValidator] Schema 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 校验 schema 字符串，返回错误信息列表。
     *
     * <p>校验 schema 本身是否为合法 JSON Schema Draft 7。
     * 空列表表示合法。</p>
     *
     * @param schemaJson JSON Schema 字符串
     * @return 错误信息列表（空列表表示合法）
     */
    public static List<String> validate(String schemaJson) {
        List<String> errors = new ArrayList<>();
        if (schemaJson == null || schemaJson.isBlank()) {
            errors.add("JSON Schema 不能为空");
            return errors;
        }
        try {
            // 1. 尝试加载 schema（语法层面校验）
            JsonSchema schema = FACTORY.getSchema(schemaJson);
            // 2. 用空对象做一次校验，确保 schema 可执行（语义层面校验）
            JsonNode testNode = OBJECT_MAPPER.readTree("{\"_self_test\":true}");
            Set<ValidationMessage> messages = schema.validate(testNode);
            // 注：testNode 与 schema 不匹配产生的错误属于数据校验错误，不代表 schema 本身非法
            // 仅当 schema 语法/结构错误时才会抛异常，此处仅确认 schema 可执行
            return errors;
        } catch (Exception e) {
            errors.add("JSON Schema Draft 7 校验失败：" + e.getMessage());
            return errors;
        }
    }

    /**
     * 用 schema 校验数据 JSON，返回错误信息列表。
     *
     * @param schemaJson JSON Schema 字符串
     * @param dataJson   待校验数据 JSON 字符串
     * @return 错误信息列表（空列表表示校验通过）
     */
    public static List<String> validateData(String schemaJson, String dataJson) {
        List<String> errors = new ArrayList<>();
        if (schemaJson == null || schemaJson.isBlank()) {
            errors.add("JSON Schema 不能为空");
            return errors;
        }
        if (dataJson == null || dataJson.isBlank()) {
            errors.add("待校验数据不能为空");
            return errors;
        }
        try {
            JsonSchema schema = FACTORY.getSchema(schemaJson);
            JsonNode dataNode = OBJECT_MAPPER.readTree(dataJson);
            Set<ValidationMessage> messages = schema.validate(dataNode);
            for (ValidationMessage msg : messages) {
                errors.add(msg.getMessage());
            }
            return errors;
        } catch (Exception e) {
            errors.add("数据校验失败：" + e.getMessage());
            return errors;
        }
    }
}
