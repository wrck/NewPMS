package com.vibe.report.service.impl;

import com.vibe.report.dto.DeviceReportQueryDTO;
import com.vibe.report.dto.FinanceReportQueryDTO;
import com.vibe.report.dto.ProjectReportQueryDTO;
import com.vibe.report.dto.ResourceReportQueryDTO;
import com.vibe.report.mapper.DeviceReportMapper;
import com.vibe.report.mapper.FinanceReportMapper;
import com.vibe.report.mapper.ProjectReportMapper;
import com.vibe.report.mapper.ResourceReportMapper;
import com.vibe.report.vo.DeviceReportVO;
import com.vibe.report.vo.FinanceReportVO;
import com.vibe.report.vo.ProjectReportVO;
import com.vibe.report.vo.ResourceReportVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务报表服务实现单元测试（Task 3 SubTask 3.6）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>getProjectReport：4 段查询条件透传、6 个 VO 字段组装、空查询兜底</li>
 *   <li>getDeviceReport：productLine 透传、5 个 VO 字段组装</li>
 *   <li>getResourceReport：engineerId 透传、3 个 VO 字段组装</li>
 *   <li>getFinanceReport：customerId 透传、5 个 VO 字段组装</li>
 *   <li>@Cacheable 注解配置校验：cacheNames=reportDetail、SpEL key 正确</li>
 * </ul>
 *
 * <p>说明：纯单元测试下 @Cacheable 切面未激活，缓存命中需在集成测试中验证；
 * 这里通过反射校验注解配置，并验证每次调用都执行 Mapper（即未走缓存）。</p>
 *
 * @author vibe
 */
@DisplayName("业务报表服务 BusinessReportServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class BusinessReportServiceImplTest {

    @Mock
    private ProjectReportMapper projectReportMapper;
    @Mock
    private DeviceReportMapper deviceReportMapper;
    @Mock
    private ResourceReportMapper resourceReportMapper;
    @Mock
    private FinanceReportMapper financeReportMapper;

    @InjectMocks
    private BusinessReportServiceImpl businessReportService;

    /* ============ 项目报表 ============ */

    @Nested
    @DisplayName("getProjectReport 项目报表")
    class ProjectReportTest {

        @Test
        @DisplayName("正常组装：6 个字段全部填充，4 段查询条件透传给 Mapper")
        void should_assemble_project_report_with_all_sections() {
            ProjectReportQueryDTO query = new ProjectReportQueryDTO();
            query.setStatus("EXECUTE");
            query.setPmId(10L);
            query.setProductLine("PL-A");
            query.setRegion("EAST");

            ProjectReportVO.Summary summary = new ProjectReportVO.Summary();
            summary.setTotal(10L);
            when(projectReportMapper.projectReportSummary("EXECUTE", 10L, "PL-A", "EAST"))
                    .thenReturn(summary);
            when(projectReportMapper.projectReportByStatus("EXECUTE", 10L, "PL-A", "EAST"))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportByProductLine("EXECUTE", 10L, "PL-A", "EAST"))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportByRegion("EXECUTE", 10L, "PL-A", "EAST"))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportByPm("EXECUTE", 10L, "PL-A", "EAST"))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportDetail("EXECUTE", 10L, "PL-A", "EAST"))
                    .thenReturn(Collections.emptyList());

            ProjectReportVO vo = businessReportService.getProjectReport(query);

            assertNotNull(vo);
            assertAll("字段填充",
                    () -> assertEquals(10L, vo.getSummary().getTotal()),
                    () -> assertNotNull(vo.getByStatus()),
                    () -> assertNotNull(vo.getByProductLine()),
                    () -> assertNotNull(vo.getByRegion()),
                    () -> assertNotNull(vo.getByPm()),
                    () -> assertNotNull(vo.getDetail())
            );
            // 4 段查询条件被透传（每个 mapper 都收到全部 4 个参数）
            verify(projectReportMapper).projectReportSummary("EXECUTE", 10L, "PL-A", "EAST");
            verify(projectReportMapper).projectReportByStatus("EXECUTE", 10L, "PL-A", "EAST");
            verify(projectReportMapper).projectReportByProductLine("EXECUTE", 10L, "PL-A", "EAST");
            verify(projectReportMapper).projectReportByRegion("EXECUTE", 10L, "PL-A", "EAST");
            verify(projectReportMapper).projectReportByPm("EXECUTE", 10L, "PL-A", "EAST");
            verify(projectReportMapper).projectReportDetail("EXECUTE", 10L, "PL-A", "EAST");
        }

        @Test
        @DisplayName("query 为 null 时各过滤参数均传 null")
        void should_pass_null_filters_when_query_is_null() {
            when(projectReportMapper.projectReportSummary(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(new ProjectReportVO.Summary());
            when(projectReportMapper.projectReportByStatus(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportByProductLine(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportByRegion(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportByPm(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportDetail(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(Collections.emptyList());

            ProjectReportVO vo = businessReportService.getProjectReport(null);

            assertNotNull(vo);
            verify(projectReportMapper).projectReportSummary(null, null, null, null);
            verify(projectReportMapper).projectReportDetail(null, null, null, null);
        }

        @Test
        @DisplayName("Mapper 返回 null 时 VO 各字段保持 null（不抛 NPE）")
        void should_keep_null_fields_when_mapper_returns_null() {
            ProjectReportQueryDTO query = new ProjectReportQueryDTO();
            when(projectReportMapper.projectReportSummary(any(), any(), any(), any())).thenReturn(null);
            when(projectReportMapper.projectReportByStatus(any(), any(), any(), any())).thenReturn(null);
            when(projectReportMapper.projectReportByProductLine(any(), any(), any(), any())).thenReturn(null);
            when(projectReportMapper.projectReportByRegion(any(), any(), any(), any())).thenReturn(null);
            when(projectReportMapper.projectReportByPm(any(), any(), any(), any())).thenReturn(null);
            when(projectReportMapper.projectReportDetail(any(), any(), any(), any())).thenReturn(null);

            ProjectReportVO vo = businessReportService.getProjectReport(query);

            assertNotNull(vo);
            assertNull(vo.getSummary());
            assertNull(vo.getByStatus());
            assertNull(vo.getByPm());
            assertNull(vo.getDetail());
        }
    }

    /* ============ 设备报表 ============ */

    @Nested
    @DisplayName("getDeviceReport 设备报表")
    class DeviceReportTest {

        @Test
        @DisplayName("正常组装：5 个字段全部填充，productLine 透传")
        void should_assemble_device_report_with_5_sections() {
            DeviceReportQueryDTO query = new DeviceReportQueryDTO();
            query.setProductLine("PL-B");

            DeviceReportVO.Summary summary = new DeviceReportVO.Summary();
            summary.setTotal(50L);
            when(deviceReportMapper.deviceReportSummary("PL-B")).thenReturn(summary);
            when(deviceReportMapper.deviceReportByStatus("PL-B")).thenReturn(Collections.emptyList());
            when(deviceReportMapper.deviceReportByProductLine("PL-B")).thenReturn(Collections.emptyList());
            when(deviceReportMapper.deviceReportBomCompletion()).thenReturn(Collections.emptyList());
            when(deviceReportMapper.deviceReportInventory()).thenReturn(Collections.emptyList());

            DeviceReportVO vo = businessReportService.getDeviceReport(query);

            assertNotNull(vo);
            assertAll("字段填充",
                    () -> assertEquals(50L, vo.getSummary().getTotal()),
                    () -> assertNotNull(vo.getStatusDistribution()),
                    () -> assertNotNull(vo.getProductLineDistribution()),
                    () -> assertNotNull(vo.getBomCompletion()),
                    () -> assertNotNull(vo.getInventoryStatus())
            );
            verify(deviceReportMapper).deviceReportSummary("PL-B");
            verify(deviceReportMapper).deviceReportByStatus("PL-B");
            verify(deviceReportMapper).deviceReportByProductLine("PL-B");
            verify(deviceReportMapper).deviceReportBomCompletion();
            verify(deviceReportMapper).deviceReportInventory();
        }

        @Test
        @DisplayName("query 为 null 时 productLine 传 null")
        void should_pass_null_product_line_when_query_null() {
            when(deviceReportMapper.deviceReportSummary(isNull())).thenReturn(new DeviceReportVO.Summary());
            when(deviceReportMapper.deviceReportByStatus(isNull())).thenReturn(Collections.emptyList());
            when(deviceReportMapper.deviceReportByProductLine(isNull())).thenReturn(Collections.emptyList());
            when(deviceReportMapper.deviceReportBomCompletion()).thenReturn(Collections.emptyList());
            when(deviceReportMapper.deviceReportInventory()).thenReturn(Collections.emptyList());

            DeviceReportVO vo = businessReportService.getDeviceReport(null);

            assertNotNull(vo);
            verify(deviceReportMapper).deviceReportSummary(null);
            verify(deviceReportMapper).deviceReportByStatus(null);
            verify(deviceReportMapper).deviceReportByProductLine(null);
        }
    }

    /* ============ 资源报表 ============ */

    @Nested
    @DisplayName("getResourceReport 资源报表")
    class ResourceReportTest {

        @Test
        @DisplayName("正常组装：3 个字段全部填充，engineerId 透传")
        void should_assemble_resource_report() {
            ResourceReportQueryDTO query = new ResourceReportQueryDTO();
            query.setEngineerId(88L);

            ResourceReportVO.Summary summary = new ResourceReportVO.Summary();
            summary.setTotalEngineers(20L);
            when(resourceReportMapper.resourceReportSummary(88L)).thenReturn(summary);
            when(resourceReportMapper.resourceReportByEngineer(88L)).thenReturn(Collections.emptyList());
            when(resourceReportMapper.resourceReportByProject()).thenReturn(Collections.emptyList());

            ResourceReportVO vo = businessReportService.getResourceReport(query);

            assertNotNull(vo);
            assertAll("字段填充",
                    () -> assertEquals(20L, vo.getSummary().getTotalEngineers()),
                    () -> assertNotNull(vo.getByEngineer()),
                    () -> assertNotNull(vo.getByProject())
            );
            verify(resourceReportMapper).resourceReportSummary(88L);
            verify(resourceReportMapper).resourceReportByEngineer(88L);
            verify(resourceReportMapper).resourceReportByProject();
        }

        @Test
        @DisplayName("query 为 null 时 engineerId 传 null")
        void should_pass_null_engineer_id_when_query_null() {
            when(resourceReportMapper.resourceReportSummary(isNull())).thenReturn(new ResourceReportVO.Summary());
            when(resourceReportMapper.resourceReportByEngineer(isNull())).thenReturn(Collections.emptyList());
            when(resourceReportMapper.resourceReportByProject()).thenReturn(Collections.emptyList());

            ResourceReportVO vo = businessReportService.getResourceReport(null);

            assertNotNull(vo);
            verify(resourceReportMapper).resourceReportSummary(null);
            verify(resourceReportMapper).resourceReportByEngineer(null);
        }
    }

    /* ============ 财务报表 ============ */

    @Nested
    @DisplayName("getFinanceReport 财务报表")
    class FinanceReportTest {

        @Test
        @DisplayName("正常组装：5 个字段全部填充，customerId 透传")
        void should_assemble_finance_report() {
            FinanceReportQueryDTO query = new FinanceReportQueryDTO();
            query.setCustomerId(7L);

            FinanceReportVO.Summary summary = new FinanceReportVO.Summary();
            summary.setTotalRevenue(new BigDecimal("100000"));
            when(financeReportMapper.financeReportSummary(7L)).thenReturn(summary);
            when(financeReportMapper.financeReportByCustomer(7L)).thenReturn(Collections.emptyList());
            when(financeReportMapper.financeReportByRegion()).thenReturn(Collections.emptyList());
            when(financeReportMapper.financeReportByProductLine()).thenReturn(Collections.emptyList());
            when(financeReportMapper.financeReportAgentSettlement()).thenReturn(Collections.emptyList());

            FinanceReportVO vo = businessReportService.getFinanceReport(query);

            assertNotNull(vo);
            assertAll("字段填充",
                    () -> assertEquals(new BigDecimal("100000"), vo.getSummary().getTotalRevenue()),
                    () -> assertNotNull(vo.getByCustomer()),
                    () -> assertNotNull(vo.getByRegion()),
                    () -> assertNotNull(vo.getByProductLine()),
                    () -> assertNotNull(vo.getAgentSettlement())
            );
            verify(financeReportMapper).financeReportSummary(7L);
            verify(financeReportMapper).financeReportByCustomer(7L);
            verify(financeReportMapper).financeReportByRegion();
            verify(financeReportMapper).financeReportByProductLine();
            verify(financeReportMapper).financeReportAgentSettlement();
        }

        @Test
        @DisplayName("query 为 null 时 customerId 传 null")
        void should_pass_null_customer_id_when_query_null() {
            when(financeReportMapper.financeReportSummary(isNull())).thenReturn(new FinanceReportVO.Summary());
            when(financeReportMapper.financeReportByCustomer(isNull())).thenReturn(Collections.emptyList());
            when(financeReportMapper.financeReportByRegion()).thenReturn(Collections.emptyList());
            when(financeReportMapper.financeReportByProductLine()).thenReturn(Collections.emptyList());
            when(financeReportMapper.financeReportAgentSettlement()).thenReturn(Collections.emptyList());

            FinanceReportVO vo = businessReportService.getFinanceReport(null);

            assertNotNull(vo);
            verify(financeReportMapper).financeReportSummary(null);
            verify(financeReportMapper).financeReportByCustomer(null);
        }
    }

    /* ============ @Cacheable 注解配置校验 ============ */

    @Nested
    @DisplayName("@Cacheable 缓存注解配置")
    class CacheableConfigTest {

        /**
         * 纯单元测试环境下 @Cacheable 切面不生效，每次调用都会进入方法体执行 Mapper。
         * 此测试验证两次调用确实每次都触发 Mapper（未走缓存），印证当前环境为「未启用缓存」的纯净逻辑。
         */
        @Test
        @DisplayName("同一 query 连续调用两次均触发 Mapper（单测环境无缓存）")
        void should_invoke_mapper_each_time_in_unit_test() {
            ProjectReportQueryDTO query = new ProjectReportQueryDTO();
            query.setStatus("EXECUTE");
            when(projectReportMapper.projectReportSummary(eq("EXECUTE"), any(), any(), any()))
                    .thenReturn(new ProjectReportVO.Summary());
            when(projectReportMapper.projectReportByStatus(any(), any(), any(), any())).thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportByProductLine(any(), any(), any(), any())).thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportByRegion(any(), any(), any(), any())).thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportByPm(any(), any(), any(), any())).thenReturn(Collections.emptyList());
            when(projectReportMapper.projectReportDetail(any(), any(), any(), any())).thenReturn(Collections.emptyList());

            businessReportService.getProjectReport(query);
            businessReportService.getProjectReport(query);

            // 验证 Mapper 被调用 2 次（无缓存），证明单测下 @Cacheable 切面未激活
            verify(projectReportMapper, org.mockito.Mockito.times(2))
                    .projectReportSummary(eq("EXECUTE"), any(), any(), any());
        }

        @Test
        @DisplayName("4 个方法均标注 @Cacheable，且 cacheNames=reportDetail")
        void should_all_methods_be_annotated_with_cacheable() throws NoSuchMethodException {
            Method m1 = BusinessReportServiceImpl.class.getMethod("getProjectReport", ProjectReportQueryDTO.class);
            Method m2 = BusinessReportServiceImpl.class.getMethod("getDeviceReport", DeviceReportQueryDTO.class);
            Method m3 = BusinessReportServiceImpl.class.getMethod("getResourceReport", ResourceReportQueryDTO.class);
            Method m4 = BusinessReportServiceImpl.class.getMethod("getFinanceReport", FinanceReportQueryDTO.class);

            assertAll("4 个报表方法均带 @Cacheable(cacheNames=reportDetail)",
                    () -> assertTrue(m1.isAnnotationPresent(Cacheable.class), "getProjectReport 缺少 @Cacheable"),
                    () -> assertTrue(m2.isAnnotationPresent(Cacheable.class), "getDeviceReport 缺少 @Cacheable"),
                    () -> assertTrue(m3.isAnnotationPresent(Cacheable.class), "getResourceReport 缺少 @Cacheable"),
                    () -> assertTrue(m4.isAnnotationPresent(Cacheable.class), "getFinanceReport 缺少 @Cacheable"),
                    () -> assertEquals("reportDetail", m1.getAnnotation(Cacheable.class).cacheNames()[0]),
                    () -> assertEquals("reportDetail", m2.getAnnotation(Cacheable.class).cacheNames()[0]),
                    () -> assertEquals("reportDetail", m3.getAnnotation(Cacheable.class).cacheNames()[0]),
                    () -> assertEquals("reportDetail", m4.getAnnotation(Cacheable.class).cacheNames()[0])
            );
        }

        @Test
        @DisplayName("项目报表 SpEL key 包含 status/pmId/productLine/region 4 个维度")
        void should_project_report_key_include_all_dimensions() throws NoSuchMethodException {
            Method m = BusinessReportServiceImpl.class.getMethod("getProjectReport", ProjectReportQueryDTO.class);
            Cacheable c = m.getAnnotation(Cacheable.class);
            String key = c.key();
            assertAll("SpEL key 应包含 4 个查询维度",
                    () -> assertTrue(key.contains("status"), "key 缺少 status：" + key),
                    () -> assertTrue(key.contains("pmId"), "key 缺少 pmId：" + key),
                    () -> assertTrue(key.contains("productLine"), "key 缺少 productLine：" + key),
                    () -> assertTrue(key.contains("region"), "key 缺少 region：" + key),
                    () -> assertTrue(key.contains("'projectReport:'"), "key 前缀应为 projectReport:")
            );
        }

        @Test
        @DisplayName("资源报表 SpEL key 包含 engineerId 维度")
        void should_resource_report_key_include_engineer_id() throws NoSuchMethodException {
            Method m = BusinessReportServiceImpl.class.getMethod("getResourceReport", ResourceReportQueryDTO.class);
            Cacheable c = m.getAnnotation(Cacheable.class);
            assertTrue(c.key().contains("engineerId"), "key 缺少 engineerId：" + c.key());
            assertTrue(c.key().contains("'resourceReport:'"), "key 前缀应为 resourceReport:");
        }
    }
}
