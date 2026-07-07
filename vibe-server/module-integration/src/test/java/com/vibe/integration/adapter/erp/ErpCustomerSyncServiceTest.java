package com.vibe.integration.adapter.erp;

import com.vibe.integration.adapter.erp.dto.ErpCustomerDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ERP 客户同步服务单元测试（Task 3 SubTask 3.5）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>syncCustomer：未启用返回 null、启用时调用 Feign 并填充 syncedAt、Feign 返回 null</li>
 *   <li>syncAllCustomers：未启用返回空列表、单页全量同步、分页同步、增量同步</li>
 * </ul>
 *
 * <p>注意：@CircuitBreaker / @Retry 注解在纯单元测试（无 Spring AOP）下不会生效，
 * 因此本测试只验证业务逻辑，不验证熔断/重试行为。</p>
 *
 * @author vibe
 */
@DisplayName("ERP 客户同步服务 ErpCustomerSyncService 测试")
@ExtendWith(MockitoExtension.class)
class ErpCustomerSyncServiceTest {

    @Mock
    private ErpCustomerFeignClient erpCustomerFeignClient;

    @InjectMocks
    private ErpCustomerSyncService erpCustomerSyncService;

    /**
     * 设置 erpEnabled 字段（@Value 注入的字段在单元测试中需要手动设置）。
     */
    private void setErpEnabled(boolean enabled) {
        ReflectionTestUtils.setField(erpCustomerSyncService, "erpEnabled", enabled);
    }

    @Nested
    @DisplayName("syncCustomer 同步单个客户")
    class SyncCustomerTest {

        @Test
        @DisplayName("未启用时返回 null 且不调用 Feign")
        void should_return_null_when_not_enabled() {
            setErpEnabled(false);

            ErpCustomerDTO result = erpCustomerSyncService.syncCustomer(100L);

            assertNull(result);
            verify(erpCustomerFeignClient, never()).getCustomerById(100L);
        }

        @Test
        @DisplayName("启用时调用 Feign 并填充 syncedAt")
        void should_call_feign_and_set_synced_at_when_enabled() {
            setErpEnabled(true);
            ErpCustomerDTO dto = new ErpCustomerDTO();
            dto.setCustomerId(100L);
            dto.setCustomerName("客户A");
            when(erpCustomerFeignClient.getCustomerById(100L)).thenReturn(dto);

            ErpCustomerDTO result = erpCustomerSyncService.syncCustomer(100L);

            assertNotNull(result);
            assertEquals(100L, result.getCustomerId());
            assertEquals("客户A", result.getCustomerName());
            assertNotNull(result.getSyncedAt());
        }

        @Test
        @DisplayName("Feign 返回 null 时直接返回 null（不填充 syncedAt）")
        void should_return_null_when_feign_returns_null() {
            setErpEnabled(true);
            when(erpCustomerFeignClient.getCustomerById(100L)).thenReturn(null);

            ErpCustomerDTO result = erpCustomerSyncService.syncCustomer(100L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("syncAllCustomers 同步全部客户")
    class SyncAllCustomersTest {

        @Test
        @DisplayName("未启用时返回空列表且不调用 Feign")
        void should_return_empty_list_when_not_enabled() {
            setErpEnabled(false);

            List<ErpCustomerDTO> result = erpCustomerSyncService.syncAllCustomers(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(erpCustomerFeignClient, never()).listCustomers(any(), any(), any());
        }

        @Test
        @DisplayName("单页全量同步：batch.size < size 时停止翻页")
        void should_sync_single_page_when_batch_smaller_than_size() {
            setErpEnabled(true);
            ErpCustomerDTO dto1 = new ErpCustomerDTO();
            dto1.setCustomerId(1L);
            ErpCustomerDTO dto2 = new ErpCustomerDTO();
            dto2.setCustomerId(2L);
            when(erpCustomerFeignClient.listCustomers(null, 1, 100))
                    .thenReturn(List.of(dto1, dto2));

            List<ErpCustomerDTO> result = erpCustomerSyncService.syncAllCustomers(null);

            assertEquals(2, result.size());
            assertNotNull(result.get(0).getSyncedAt());
            assertNotNull(result.get(1).getSyncedAt());
        }

        @Test
        @DisplayName("分页同步：batch.size == size 时继续翻页直到空批次")
        void should_sync_multiple_pages_until_empty_batch() {
            setErpEnabled(true);
            // 第一页：满页 100 条
            List<ErpCustomerDTO> page1 = buildBatch(100, 1);
            // 第二页：50 条（不足 100，停止翻页）
            List<ErpCustomerDTO> page2 = buildBatch(50, 101);
            when(erpCustomerFeignClient.listCustomers(null, 1, 100)).thenReturn(page1);
            when(erpCustomerFeignClient.listCustomers(null, 2, 100)).thenReturn(page2);

            List<ErpCustomerDTO> result = erpCustomerSyncService.syncAllCustomers(null);

            assertEquals(150, result.size());
        }

        @Test
        @DisplayName("增量同步：传递 updatedAfter 参数")
        void should_sync_incrementally_with_updated_after() {
            setErpEnabled(true);
            LocalDateTime updatedAfter = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
            ErpCustomerDTO dto = new ErpCustomerDTO();
            dto.setCustomerId(1L);
            when(erpCustomerFeignClient.listCustomers(updatedAfter.toString(), 1, 100))
                    .thenReturn(List.of(dto));

            List<ErpCustomerDTO> result = erpCustomerSyncService.syncAllCustomers(updatedAfter);

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getSyncedAt());
        }

        @Test
        @DisplayName("Feign 返回空列表时停止翻页并返回空结果")
        void should_stop_when_feign_returns_empty_list() {
            setErpEnabled(true);
            when(erpCustomerFeignClient.listCustomers(null, 1, 100))
                    .thenReturn(Collections.emptyList());

            List<ErpCustomerDTO> result = erpCustomerSyncService.syncAllCustomers(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Feign 返回 null 时停止翻页并返回空结果")
        void should_stop_when_feign_returns_null() {
            setErpEnabled(true);
            when(erpCustomerFeignClient.listCustomers(null, 1, 100))
                    .thenReturn(null);

            List<ErpCustomerDTO> result = erpCustomerSyncService.syncAllCustomers(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    /* ============ 测试辅助方法 ============ */

    /**
     * 构建指定大小的批次，customerId 从 startId 开始递增。
     */
    private List<ErpCustomerDTO> buildBatch(int size, long startId) {
        return java.util.stream.LongStream.range(0, size)
                .mapToObj(i -> {
                    ErpCustomerDTO dto = new ErpCustomerDTO();
                    dto.setCustomerId(startId + i);
                    return dto;
                })
                .toList();
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
