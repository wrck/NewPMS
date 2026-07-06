package com.vibe.lowcode.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.common.base.PageQuery;
import com.vibe.common.result.PageResult;
import com.vibe.lowcode.dto.LowcodeFormConfigDTO;
import com.vibe.lowcode.dto.LowcodeInstantiateDTO;
import com.vibe.lowcode.service.LowcodeFormConfigService;
import com.vibe.lowcode.vo.LowcodeFormConfigVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 低代码表单配置 Controller MockMvc 测试（Task E3.2）
 *
 * <p>采用 standalone MockMvc（不加载完整 Spring 上下文），通过 Mockito 注入 Service Mock：
 * <ul>
 *   <li>分页查询：GET /api/v1/lowcode/forms</li>
 *   <li>详情：GET /api/v1/lowcode/forms/{id}</li>
 *   <li>创建：POST /api/v1/lowcode/forms</li>
 *   <li>更新：PUT /api/v1/lowcode/forms/{id}</li>
 *   <li>删除：DELETE /api/v1/lowcode/forms/{id}</li>
 *   <li>复制：POST /api/v1/lowcode/forms/{id}/copy</li>
 *   <li>导入：POST /api/v1/lowcode/forms/import</li>
 *   <li>基于模板实例化：POST /api/v1/lowcode/forms/templates/{tid}/instantiate</li>
 * </ul>
 *
 * <p>注：@PreAuthorize 在 standalone MockMvc 中不会触发，需要 SecurityInterceptor 才生效。
 * 此测试聚焦于 Controller 路由、参数绑定、JSON 序列化是否正确。</p>
 *
 * @author vibe
 */
@DisplayName("低代码表单配置 FormConfigController 测试")
@ExtendWith(MockitoExtension.class)
class FormConfigControllerTest {

    @Mock
    private LowcodeFormConfigService formConfigService;

    @InjectMocks
    private FormConfigController formConfigController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(formConfigController).build();
    }

    @Test
    @DisplayName("GET /lowcode/forms 分页查询返回 200 与 PageResult")
    void should_page_query_return_200() throws Exception {
        LowcodeFormConfigVO vo = buildVo(1L, "customer_form", "客户表单");
        PageResult<LowcodeFormConfigVO> pageResult = PageResult.of(
                Collections.singletonList(vo), 1L, 1L, 20L);

        when(formConfigService.page(any(PageQuery.class), eq("customer")))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/lowcode/forms")
                        .param("page", "1")
                        .param("size", "20")
                        .param("keyword", "customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].configCode").value("customer_form"))
                .andExpect(jsonPath("$.data.records[0].configName").value("客户表单"));
    }

    @Test
    @DisplayName("GET /lowcode/forms/{id} 详情返回 200 与 VO")
    void should_detail_return_200() throws Exception {
        LowcodeFormConfigVO vo = buildVo(7L, "edit_form", "编辑表单");
        when(formConfigService.getById(7L)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/lowcode/forms/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.configCode").value("edit_form"));
    }

    @Test
    @DisplayName("POST /lowcode/forms 创建返回 200 与新 ID")
    void should_create_return_200_with_new_id() throws Exception {
        LowcodeFormConfigDTO dto = new LowcodeFormConfigDTO();
        dto.setConfigCode("new_form");
        dto.setConfigName("新表单");
        dto.setSchemaJson("{\"type\":\"object\"}");

        when(formConfigService.create(any(LowcodeFormConfigDTO.class))).thenReturn(101L);

        mockMvc.perform(post("/api/v1/lowcode/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(101));

        verify(formConfigService).create(any(LowcodeFormConfigDTO.class));
    }

    @Test
    @DisplayName("POST /lowcode/forms 缺少 configCode 触发校验失败（400）")
    void should_create_return_400_when_config_code_blank() throws Exception {
        LowcodeFormConfigDTO dto = new LowcodeFormConfigDTO();
        dto.setConfigCode(""); // NotBlank 校验失败
        dto.setConfigName("X");
        dto.setSchemaJson("{}");

        // @Valid 注解触发 MethodArgumentNotValidException，Spring 默认返回 400
        // service 不应被调用
        mockMvc.perform(post("/api/v1/lowcode/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(formConfigService, never()).create(any(LowcodeFormConfigDTO.class));
    }

    @Test
    @DisplayName("PUT /lowcode/forms/{id} 更新返回 200")
    void should_update_return_200() throws Exception {
        LowcodeFormConfigDTO dto = new LowcodeFormConfigDTO();
        dto.setConfigCode("edit_form");
        dto.setConfigName("更新后名称");
        dto.setSchemaJson("{\"type\":\"object\"}");

        doNothing().when(formConfigService).update(eq(5L), any(LowcodeFormConfigDTO.class));

        mockMvc.perform(put("/api/v1/lowcode/forms/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(formConfigService).update(eq(5L), any(LowcodeFormConfigDTO.class));
    }

    @Test
    @DisplayName("DELETE /lowcode/forms/{id} 删除返回 200")
    void should_delete_return_200() throws Exception {
        doNothing().when(formConfigService).delete(9L);

        mockMvc.perform(delete("/api/v1/lowcode/forms/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(formConfigService).delete(9L);
    }

    @Test
    @DisplayName("POST /lowcode/forms/{id}/copy 复制返回 200 与新 ID")
    void should_copy_return_200_with_new_id() throws Exception {
        when(formConfigService.copy(11L)).thenReturn(12L);

        mockMvc.perform(post("/api/v1/lowcode/forms/11/copy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(12));

        verify(formConfigService).copy(11L);
    }

    @Test
    @DisplayName("POST /lowcode/forms/import 导入返回 200 与新 ID")
    void should_import_return_200_with_new_id() throws Exception {
        LowcodeFormConfigDTO dto = new LowcodeFormConfigDTO();
        dto.setConfigCode("imported_form");
        dto.setConfigName("导入表单");
        dto.setSchemaJson("{\"type\":\"object\"}");

        when(formConfigService.importJson(any(LowcodeFormConfigDTO.class))).thenReturn(21L);

        mockMvc.perform(post("/api/v1/lowcode/forms/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(21));

        verify(formConfigService).importJson(any(LowcodeFormConfigDTO.class));
    }

    @Test
    @DisplayName("POST /lowcode/forms/templates/{tid}/instantiate 实例化返回 200 与新 ID")
    void should_instantiate_from_template_return_200() throws Exception {
        LowcodeInstantiateDTO dto = new LowcodeInstantiateDTO();
        dto.setConfigName("实例化表单");

        when(formConfigService.instantiateFromTemplate(eq(99L), eq("实例化表单")))
                .thenReturn(31L);

        mockMvc.perform(post("/api/v1/lowcode/forms/templates/99/instantiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(31));

        verify(formConfigService).instantiateFromTemplate(eq(99L), eq("实例化表单"));
    }

    @Test
    @DisplayName("GET /lowcode/forms/{id}/export 导出返回 application/json 与内容")
    void should_export_return_json_content() throws Exception {
        String schemaJson = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        when(formConfigService.exportJson(13L)).thenReturn(schemaJson);

        mockMvc.perform(get("/api/v1/lowcode/forms/13/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(schemaJson));

        verify(formConfigService).exportJson(13L);
    }

    /* ============ 测试辅助方法 ============ */

    private LowcodeFormConfigVO buildVo(Long id, String code, String name) {
        LowcodeFormConfigVO vo = new LowcodeFormConfigVO();
        vo.setId(id);
        vo.setConfigCode(code);
        vo.setConfigName(name);
        vo.setSchemaJson("{\"type\":\"object\"}");
        vo.setStatus(1);
        vo.setVersion(1);
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }
}
