package com.vibe.delivery.service.impl;

import com.vibe.common.exception.BusinessException;
import com.vibe.delivery.bo.GpsLocation;
import com.vibe.delivery.bo.ProjectTaskLookup;
import com.vibe.delivery.constant.DeliveryConstant;
import com.vibe.delivery.dto.WorkOrderCheckinDTO;
import com.vibe.delivery.dto.WorkOrderCreateDTO;
import com.vibe.delivery.entity.WorkOrderEntity;
import com.vibe.delivery.mapper.ProjectTaskLookupMapper;
import com.vibe.delivery.mapper.WorkOrderMapper;
import com.vibe.delivery.service.WorkOrderIssueService;
import com.vibe.delivery.service.WorkOrderPhotoService;
import com.vibe.delivery.service.WorkOrderStepService;
import com.vibe.delivery.vo.WorkOrderVO;
import com.vibe.utils.MinioUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单服务实现单元测试
 *
 * <p>覆盖工单创建、签到 GPS Haversine 距离校验、工程师完成（步骤校验）、PM 确认等核心业务逻辑。</p>
 *
 * @author vibe
 */
@DisplayName("工单服务 WorkOrderServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceImplTest {

    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private ProjectTaskLookupMapper projectTaskLookupMapper;
    @Mock
    private WorkOrderStepService workOrderStepService;
    @Mock
    private WorkOrderPhotoService workOrderPhotoService;
    @Mock
    private WorkOrderIssueService workOrderIssueService;
    @Mock
    private MinioUtils minioUtils;

    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    /** 天安门坐标（测试基准点） */
    private static final double TAM_LAT = 39.9042;
    private static final double TAM_LON = 116.4074;

    /** 站点信息 JSON：期望坐标=天安门，允许半径 500 米 */
    private static final String SITE_INFO_500M = String.format(
            "{\"expectedLatitude\":%s,\"expectedLongitude\":%s,\"allowedRadiusMeters\":500}",
            TAM_LAT, TAM_LON);

    @Nested
    @DisplayName("创建工单 createWorkOrder")
    class CreateWorkOrderTest {

        @Test
        @DisplayName("任务不存在抛 TASK_NOT_FOUND")
        void should_throw_when_task_not_found() {
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(null);

            WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
            dto.setTaskId(100L);
            dto.setEngineerId(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.createWorkOrder(dto),
                    "任务不存在应抛 BusinessException");
            assertEquals(40403, ex.getCode(), "应为 TASK_NOT_FOUND 错误码");
            verify(workOrderMapper, never()).insert(any(WorkOrderEntity.class));
        }

        @Test
        @DisplayName("任务未指派工程师且 DTO 未指定抛 PARAM_INVALID")
        void should_throw_when_no_engineer() {
            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_PENDING);
            task.setAssigneeId(null);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);

            WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
            dto.setTaskId(100L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.createWorkOrder(dto),
                    "无工程师应抛 BusinessException");
            assertEquals(40000, ex.getCode(), "应为 PARAM_INVALID 错误码");
        }

        @Test
        @DisplayName("DTO 指定 engineerId 优先使用")
        void should_use_dto_engineer_id_when_provided() {
            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_PENDING);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);

            WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
            dto.setTaskId(100L);
            dto.setEngineerId(200L);

            workOrderService.createWorkOrder(dto);

            ArgumentCaptor<WorkOrderEntity> captor = ArgumentCaptor.forClass(WorkOrderEntity.class);
            verify(workOrderMapper).insert(captor.capture());
            assertEquals(200L, captor.getValue().getEngineerId(),
                    "应优先使用 DTO 指定的工程师ID");
        }

        @Test
        @DisplayName("DTO 未指定 engineerId 时使用 task.assigneeId")
        void should_use_task_assignee_id_when_dto_not_provided() {
            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_PENDING);
            task.setAssigneeId(300L);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);

            WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
            dto.setTaskId(100L);

            workOrderService.createWorkOrder(dto);

            ArgumentCaptor<WorkOrderEntity> captor = ArgumentCaptor.forClass(WorkOrderEntity.class);
            verify(workOrderMapper).insert(captor.capture());
            assertEquals(300L, captor.getValue().getEngineerId(),
                    "应回退使用任务指派的工程师ID");
        }

        @Test
        @DisplayName("新建工单初始状态为 CREATED，照片数为 0")
        void should_set_created_status_and_zero_photos() {
            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_PENDING);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);

            WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
            dto.setTaskId(100L);
            dto.setEngineerId(1L);

            workOrderService.createWorkOrder(dto);

            ArgumentCaptor<WorkOrderEntity> captor = ArgumentCaptor.forClass(WorkOrderEntity.class);
            verify(workOrderMapper).insert(captor.capture());
            assertAll("工单初始值",
                    () -> assertEquals(DeliveryConstant.WORK_ORDER_STATUS_CREATED,
                            captor.getValue().getStatus(), "初始状态应为 CREATED"),
                    () -> assertEquals(0, captor.getValue().getPhotoCount(),
                            "初始照片数应为 0")
            );
        }

        @Test
        @DisplayName("PENDING 任务创建工单后推进到 IN_PROGRESS")
        void should_advance_task_to_in_progress_when_pending() {
            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_PENDING);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);

            WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
            dto.setTaskId(100L);
            dto.setEngineerId(1L);

            workOrderService.createWorkOrder(dto);

            verify(projectTaskLookupMapper).updateTaskStatus(eq(100L),
                    eq(DeliveryConstant.TASK_STATUS_IN_PROGRESS), any(), any());
        }

        @Test
        @DisplayName("CONFIRMED 任务创建工单不推进状态")
        void should_not_advance_task_when_already_confirmed() {
            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_CONFIRMED);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);

            WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
            dto.setTaskId(100L);
            dto.setEngineerId(1L);

            workOrderService.createWorkOrder(dto);

            verify(projectTaskLookupMapper, never()).updateTaskStatus(
                    anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("自定义步骤列表传入 initSteps")
        void should_use_custom_steps_when_provided() {
            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_CONFIRMED);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);

            List<String> customSteps = Arrays.asList("步骤A", "步骤B");
            WorkOrderCreateDTO dto = new WorkOrderCreateDTO();
            dto.setTaskId(100L);
            dto.setEngineerId(1L);
            dto.setSteps(customSteps);

            workOrderService.createWorkOrder(dto);

            verify(workOrderStepService).initSteps(any(), eq(customSteps));
        }
    }

    @Nested
    @DisplayName("签到 checkin GPS 校验")
    class CheckinGpsTest {

        @Test
        @DisplayName("工单不存在抛 WORK_ORDER_NOT_FOUND")
        void should_throw_when_work_order_not_found() {
            when(workOrderMapper.selectById(1L)).thenReturn(null);

            WorkOrderCheckinDTO dto = new WorkOrderCheckinDTO();
            dto.setLocation(new GpsLocation(TAM_LON, TAM_LAT, null, null, null));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.checkin(1L, dto, null),
                    "工单不存在应抛 BusinessException");
            assertEquals(40405, ex.getCode(), "应为 WORK_ORDER_NOT_FOUND 错误码");
        }

        @Test
        @DisplayName("非 CREATED 状态拒绝签到")
        void should_reject_when_status_not_created() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            WorkOrderCheckinDTO dto = new WorkOrderCheckinDTO();
            dto.setLocation(new GpsLocation(TAM_LON, TAM_LAT, null, null, null));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.checkin(1L, dto, null),
                    "非 CREATED 状态应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为 STATE_NOT_ALLOWED 错误码");
            verify(workOrderMapper, never()).updateById(any(WorkOrderEntity.class));
        }

        @Test
        @DisplayName("GPS 定位缺失拒绝签到")
        void should_reject_when_gps_missing() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CREATED);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            WorkOrderCheckinDTO dto = new WorkOrderCheckinDTO();
            dto.setLocation(new GpsLocation(null, null, null, null, null));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.checkin(1L, dto, null),
                    "GPS 缺失应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为 STATE_NOT_ALLOWED 错误码");
            assertTrue(ex.getMessage().contains("GPS"), "错误消息应包含 GPS");
            verify(workOrderMapper, never()).updateById(any(WorkOrderEntity.class));
        }

        @Test
        @DisplayName("超出允许半径拒绝签到")
        void should_reject_when_distance_exceeds_radius() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CREATED);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_IN_PROGRESS);
            task.setSiteInfo(SITE_INFO_500M);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);

            // 纬度差 0.009° ≈ 1000m，超出 500m 半径
            WorkOrderCheckinDTO dto = new WorkOrderCheckinDTO();
            dto.setLocation(new GpsLocation(TAM_LON, TAM_LAT + 0.009, null, null, null));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.checkin(1L, dto, null),
                    "超出半径应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为 STATE_NOT_ALLOWED 错误码");
            assertTrue(ex.getMessage().contains("超出"), "错误消息应包含'超出'");
            verify(workOrderMapper, never()).updateById(any(WorkOrderEntity.class));
        }

        @Test
        @DisplayName("在允许半径内签到成功，状态变为 CHECKED_IN")
        void should_checkin_successfully_when_within_radius() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CREATED);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_IN_PROGRESS);
            task.setSiteInfo(SITE_INFO_500M);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);
            // mock getDetail 依赖
            WorkOrderVO vo = new WorkOrderVO();
            vo.setId(1L);
            when(workOrderMapper.selectVoById(1L)).thenReturn(vo);
            when(workOrderStepService.calculateProgress(1L)).thenReturn(new int[]{0, 8});

            WorkOrderCheckinDTO dto = new WorkOrderCheckinDTO();
            dto.setLocation(new GpsLocation(TAM_LON, TAM_LAT, null, null, null));

            workOrderService.checkin(1L, dto, null);

            ArgumentCaptor<WorkOrderEntity> captor = ArgumentCaptor.forClass(WorkOrderEntity.class);
            verify(workOrderMapper).updateById(captor.capture());
            assertEquals(DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN,
                    captor.getValue().getStatus(), "状态应更新为 CHECKED_IN");
            assertNotNull(captor.getValue().getCheckinTime(), "签到时间不应为空");
            assertNotNull(captor.getValue().getCheckinLocation(), "签到位置不应为空");
        }

        @Test
        @DisplayName("站点未配置坐标时放行签到")
        void should_allow_checkin_when_site_no_coordinates() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CREATED);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_IN_PROGRESS);
            task.setSiteInfo(null);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);
            WorkOrderVO vo = new WorkOrderVO();
            vo.setId(1L);
            when(workOrderMapper.selectVoById(1L)).thenReturn(vo);
            when(workOrderStepService.calculateProgress(1L)).thenReturn(new int[]{0, 8});

            WorkOrderCheckinDTO dto = new WorkOrderCheckinDTO();
            dto.setLocation(new GpsLocation(TAM_LON, TAM_LAT, null, null, null));

            workOrderService.checkin(1L, dto, null);

            verify(workOrderMapper).updateById(any(WorkOrderEntity.class));
        }
    }

    @Nested
    @DisplayName("工程师完成 engineerComplete")
    class EngineerCompleteTest {

        @Test
        @DisplayName("非 CHECKED_IN/IN_PROGRESS 状态拒绝完成")
        void should_reject_when_status_not_allowed() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CREATED);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.engineerComplete(1L, null),
                    "非允许状态应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为 STATE_NOT_ALLOWED 错误码");
        }

        @Test
        @DisplayName("未签退拒绝完成")
        void should_reject_when_not_checked_out() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN);
            wo.setCheckoutTime(null);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.engineerComplete(1L, null),
                    "未签退应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为 STATE_NOT_ALLOWED 错误码");
            assertTrue(ex.getMessage().contains("签退"), "错误消息应包含'签退'");
        }

        @Test
        @DisplayName("无施工步骤拒绝完成")
        void should_reject_when_no_steps() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN);
            wo.setCheckoutTime(LocalDateTime.now());
            when(workOrderMapper.selectById(1L)).thenReturn(wo);
            when(workOrderStepService.calculateProgress(1L)).thenReturn(new int[]{0, 0});

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.engineerComplete(1L, null),
                    "无步骤应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为 STATE_NOT_ALLOWED 错误码");
            assertTrue(ex.getMessage().contains("步骤"), "错误消息应包含'步骤'");
        }

        @Test
        @DisplayName("步骤未完成拒绝完成（提示剩余数）")
        void should_reject_when_steps_incomplete() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN);
            wo.setCheckoutTime(LocalDateTime.now());
            when(workOrderMapper.selectById(1L)).thenReturn(wo);
            when(workOrderStepService.calculateProgress(1L)).thenReturn(new int[]{2, 5});

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.engineerComplete(1L, null),
                    "步骤未完成应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为 STATE_NOT_ALLOWED 错误码");
            assertTrue(ex.getMessage().contains("3"), "错误消息应包含剩余步骤数 3");
        }

        @Test
        @DisplayName("全部步骤完成且已签退，状态变为 COMPLETED")
        void should_complete_when_all_steps_done() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN);
            wo.setCheckoutTime(LocalDateTime.now());
            when(workOrderMapper.selectById(1L)).thenReturn(wo);
            // engineerComplete 校验 + getDetail 均调用 calculateProgress
            when(workOrderStepService.calculateProgress(1L)).thenReturn(new int[]{5, 5});
            WorkOrderVO vo = new WorkOrderVO();
            vo.setId(1L);
            when(workOrderMapper.selectVoById(1L)).thenReturn(vo);

            workOrderService.engineerComplete(1L, "完成");

            ArgumentCaptor<WorkOrderEntity> captor = ArgumentCaptor.forClass(WorkOrderEntity.class);
            verify(workOrderMapper).updateById(captor.capture());
            assertEquals(DeliveryConstant.WORK_ORDER_STATUS_COMPLETED,
                    captor.getValue().getStatus(), "状态应更新为 COMPLETED");
        }
    }

    @Nested
    @DisplayName("PM 确认 pmConfirm")
    class PmConfirmTest {

        @Test
        @DisplayName("非 COMPLETED 状态拒绝确认")
        void should_reject_when_status_not_completed() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_IN_PROGRESS);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workOrderService.pmConfirm(1L, null),
                    "非 COMPLETED 状态应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为 STATE_NOT_ALLOWED 错误码");
        }

        @Test
        @DisplayName("COMPLETED 状态确认成功，状态变为 CONFIRMED 并推进任务状态")
        void should_confirm_when_status_completed() {
            WorkOrderEntity wo = buildWorkOrder(1L, DeliveryConstant.WORK_ORDER_STATUS_COMPLETED);
            wo.setTaskId(100L);
            when(workOrderMapper.selectById(1L)).thenReturn(wo);

            ProjectTaskLookup task = buildTask(100L, 1L, DeliveryConstant.TASK_STATUS_IN_PROGRESS);
            when(projectTaskLookupMapper.selectById(100L)).thenReturn(task);
            when(projectTaskLookupMapper.countTotalByProject(1L)).thenReturn(5);
            when(projectTaskLookupMapper.countCompletedByProject(1L)).thenReturn(3);

            WorkOrderVO vo = new WorkOrderVO();
            vo.setId(1L);
            when(workOrderMapper.selectVoById(1L)).thenReturn(vo);
            when(workOrderStepService.calculateProgress(1L)).thenReturn(new int[]{5, 5});

            workOrderService.pmConfirm(1L, null);

            ArgumentCaptor<WorkOrderEntity> captor = ArgumentCaptor.forClass(WorkOrderEntity.class);
            verify(workOrderMapper).updateById(captor.capture());
            assertEquals(DeliveryConstant.WORK_ORDER_STATUS_CONFIRMED,
                    captor.getValue().getStatus(), "状态应更新为 CONFIRMED");
            // 验证任务状态推进为 CONFIRMED
            verify(projectTaskLookupMapper).updateTaskStatus(eq(100L),
                    eq(DeliveryConstant.TASK_STATUS_CONFIRMED), any(), any());
            // 验证项目进度同步
            verify(projectTaskLookupMapper).updateProjectProgress(eq(1L), eq(60), any());
        }
    }

    /* ============ 辅助方法 ============ */

    /**
     * 构造测试用项目任务
     */
    private ProjectTaskLookup buildTask(Long id, Long projectId, String status) {
        ProjectTaskLookup task = new ProjectTaskLookup();
        task.setId(id);
        task.setProjectId(projectId);
        task.setTaskName("测试任务");
        task.setAssigneeId(1L);
        task.setStatus(status);
        return task;
    }

    /**
     * 构造测试用工单实体
     */
    private WorkOrderEntity buildWorkOrder(Long id, String status) {
        WorkOrderEntity wo = new WorkOrderEntity();
        wo.setId(id);
        wo.setTaskId(100L);
        wo.setEngineerId(1L);
        wo.setStatus(status);
        wo.setVersion(1);
        return wo;
    }
}
