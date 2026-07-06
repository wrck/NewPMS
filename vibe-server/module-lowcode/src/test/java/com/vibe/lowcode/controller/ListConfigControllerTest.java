package com.vibe.lowcode.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.common.base.PageQuery;
import com.vibe.common.result.PageResult;
import com.vibe.lowcode.dto.LowcodeInstantiateDTO;
import com.vibe.lowcode.dto.LowcodeListConfigDTO;
import com.vibe.lowcode.service.LowcodeListConfigService;
import com.vibe.lowcode.vo.LowcodeListConfigVO;
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
 * 低代码列表配置 Controller MockMvc 测试（Task E3.2）
 *
 * <p>采用 standalone MockMvc + Mockito Service Mock，覆盖：</p>
 * <ul>
 *   <li>分页查询：GET /api/v1/lowcode/lists</li>
 *   <li>详情：GET /api/v1/lowcode/lists/{id}</li>
 *   <li>创建：POST /api/v1/lowcode/lists</li>
 *   <li>更新：PUT /api/v1/lowcode/lists/{id}</li>
 *   <li>删除：DELETE /api/v1/lowcode/lists/{id}</li>
 *   <li>复制：POST /api/v1/lowcode/lists/{id}/copy</li>
 *   <li>导入：POST /api/v1/lowcode/lists/import</li>
 *   <li>基于模板实例化：POST /api/v1/lowcode/lists/templates/{tid}/instantiate</li>
 *   <li>导出：GET /api/v1/lowcode/lists/{id}/export</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("低代码列表配置 ListConfigController 测试")
@ExtendWith(MockitoExtension.class)
class ListConfigControllerTest {

    @Mock
    private LowcodeListConfigService listConfigService;

    @InjectMocks
    private ListConfigController listConfigController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(listConfigController).build();
    }

    @Test
    @DisplayName("GET /lowcode/lists 分页查询返回 200 与 PageResult")
    void should_page_query_return_200() throws Exception {
        LowcodeListConfigVO vo = buildVo(1L, "customer_list", "客户列表");
        PageResult<LowcodeListConfigVO> pageResult = PageResult.of(
                Collections.singletonList(vo), 1L, 1L, 20L);

        when(listConfigService.page(any(PageQuery.class), eq("customer")))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/lowcode/lists")
                        .param("page", "1")
                        .param("size", "20")
                        .param("keyword", "customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].configCode").value("customer_list"))
                .andExpect(jsonPath("$.data.records[0].configName").value("客户列表"));
    }

    @Test
    @DisplayName("GET /lowcode/lists/{id} 详情返回 200 与 VO")
    void should_detail_return_200() throws Exception {
        LowcodeListConfigVO vo = buildVo(7L, "edit_list", "编辑列表");
        when(listConfigService.getById(7L)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/lowcode/lists/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.configCode").value("edit_list"));
    }

    @Test
    @DisplayName("POST /lowcode/lists 创建返回 200 与新 ID")
    void should_create_return_200_with_new_id() throws Exception {
        LowcodeListConfigDTO dto = new LowcodeListConfigDTO();
        dto.setConfigCode("new_list");
        dto.setConfigName("新列表");
        dto.setSchemaJson("{\"type\":\"object\"}");

        when(listConfigService.create(any(LowcodeListConfigDTO.class))).thenReturn(101L);

        mockMvc.perform(post("/api/v1/lowcode/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(101));

        verify(listConfigService).create(any(LowcodeListConfigDTO.class));
    }

    @Test
    @DisplayName("PUT /lowcode/lists/{id} 更新返回 200")
    void should_update_return_200() throws Exception {
        LowcodeListConfigDTO dto = new LowcodeListConfigDTO();
        dto.setConfigCode("edit_list");
        dto.setConfigName("更新后名称");
        dto.setSchemaJson("{\"type\":\"object\"}");

        doNothing().when(listConfigService).update(eq(5L), any(LowcodeListConfigDTO.class));

        mockMvc.perform(put("/api/v1/lowcode/lists/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(listConfigService).update(eq(5L), any(LowcodeListConfigDTO.class));
    }

    @Test
    @DisplayName("DELETE /lowcode/lists/{id} 删除返回 200")
    void should_delete_return_200() throws Exception {
        doNothing().when(listConfigService).delete(9L);

        mockMvc.perform(delete("/api/v1/lowcode/lists/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(listConfigService).delete(9L);
    }

    @Test
    @DisplayName("POST /lowcode/lists/{id}/copy 复制返回 200 与新 ID")
    void should_copy_return_200_with_new_id() throws Exception {
        when(listConfigService.copy(11L)).thenReturn(12L);

        mockMvc.perform(post("/api/v1/lowcode/lists/11/copy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(12));

        verify(listConfigService).copy(11L);
    }

    @Test
    @DisplayName("POST /lowcode/lists/import 导入返回 200 与新 ID")
    void should_import_return_200_with_new_id() throws Exception {
        LowcodeListConfigDTO dto = new LowcodeListConfigDTO();
        dto.setConfigCode("imported_list");
        dto.setConfigName("导入列表");
        dto.setSchemaJson("{\"type\":\"object\"}");

        when(listConfigService.importJson(any(LowcodeListConfigDTO.class))).thenReturn(21L);

        mockMvc.perform(post("/api/v1/lowcode/lists/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(21));

        verify(listConfigService).importJson(any(LowcodeListConfigDTO.class));
    }

    @Test
    @DisplayName("POST /lowcode/lists/templates/{tid}/instantiate 实例化返回 200 与新 ID")
    void should_instantiate_from_template_return_200() throws Exception {
        LowcodeInstantiateDTO dto = new LowcodeInstantiateDTO();
        dto.setConfigName("实例化列表");

        when(listConfigService.instantiateFromTemplate(eq(99L), eq("实例化列表")))
                .thenReturn(31L);

        mockMvc.perform(post("/api/v1/lowcode/lists/templates/99/instantiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(31));

        verify(listConfigService).instantiateFromTemplate(eq(99L), eq("实例化列表"));
    }

    @Test
    @DisplayName("GET /lowcode/lists/{id}/export 导出返回 application/json 与内容")
    void should_export_return_json_content() throws Exception {
        String schemaJson = "{\"type\":\"object\",\"columns\":[{\"field\":\"name\"}]}";
        when(listConfigService.exportJson(13L)).thenReturn(schemaJson);

        mockMvc.perform(get("/api/v1/lowcode/lists/13/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(schemaJson));

        verify(listConfigService).exportJson(13L);
    }

    /* ============ 测试辅助方法 ============ */

    private LowcodeListConfigVO buildVo(Long id, String code, String name) {
        LowcodeListConfigVO vo = new LowcodeListConfigVO();
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
