package com.vibe.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 用户状态字段 Jackson 序列化器。
 *
 * <p>将数据库存储的字符串状态（ACTIVE/DISABLED）序列化为数字（1/0），
 * 与前端 vibe-web 的 {@code status: 1 | 0} 类型对齐。</p>
 *
 * <ul>
 *   <li>"ACTIVE" → 1</li>
 *   <li>"DISABLED" → 0</li>
 *   <li>null/其他 → 0</li>
 * </ul>
 *
 * @author vibe
 */
public class UserStatusSerializer extends JsonSerializer<String> {

    /** 启用状态字符串常量（与 SystemConstant.USER_STATUS_ACTIVE 保持一致） */
    private static final String STATUS_ACTIVE = "ACTIVE";

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNumber(0);
            return;
        }
        if (STATUS_ACTIVE.equalsIgnoreCase(value)) {
            gen.writeNumber(1);
        } else {
            gen.writeNumber(0);
        }
    }
}
