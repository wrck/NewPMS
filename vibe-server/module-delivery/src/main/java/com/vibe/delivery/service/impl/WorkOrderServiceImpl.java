package com.vibe.delivery.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.delivery.bo.GpsLocation;
import com.vibe.delivery.bo.ProjectTaskLookup;
import com.vibe.delivery.constant.DeliveryConstant;
import com.vibe.delivery.dto.WorkOrderCheckinDTO;
import com.vibe.delivery.dto.WorkOrderCheckoutDTO;
import com.vibe.delivery.dto.WorkOrderConfirmDTO;
import com.vibe.delivery.dto.WorkOrderCreateDTO;
import com.vibe.delivery.dto.WorkOrderQueryDTO;
import com.vibe.delivery.entity.WorkOrderEntity;
import com.vibe.delivery.mapper.ProjectTaskLookupMapper;
import com.vibe.delivery.mapper.WorkOrderMapper;
import com.vibe.delivery.service.WorkOrderIssueService;
import com.vibe.delivery.service.WorkOrderPhotoService;
import com.vibe.delivery.service.WorkOrderService;
import com.vibe.delivery.service.WorkOrderStepService;
import com.vibe.delivery.util.SiteInfoUtils;
import com.vibe.delivery.vo.WorkOrderIssueVO;
import com.vibe.delivery.vo.WorkOrderPhotoVO;
import com.vibe.delivery.vo.WorkOrderStepVO;
import com.vibe.delivery.vo.WorkOrderVO;
import com.vibe.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 工单服务实现
 *
 * <p>核心业务：</p>
 * <ul>
 *   <li>创建工单：校验任务存在，初始化施工步骤</li>
 *   <li>签到：GPS Haversine 距离校验 + 拍照防作弊 + 状态机 CREATED → CHECKED_IN</li>
 *   <li>签退：记录签退时间/位置</li>
 *   <li>工程师完成：校验全部步骤完成 + 已签退，状态 IN_PROGRESS → COMPLETED</li>
 *   <li>PM 确认：状态 COMPLETED → CONFIRMED，自动推进 project_task 状态与项目进度</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    /** 默认施工步骤模板（任务未配置步骤时使用） */
    private static final List<String> DEFAULT_STEPS = Arrays.asList(
            "设备开箱检查", "设备上架固定", "电源线连接", "上联光纤连接",
            "设备加电初始化", "基础配置导入", "连通性验证", "拍照记录整体");

    private final WorkOrderMapper workOrderMapper;
    private final ProjectTaskLookupMapper projectTaskLookupMapper;
    private final WorkOrderStepService workOrderStepService;
    private final WorkOrderPhotoService workOrderPhotoService;
    private final WorkOrderIssueService workOrderIssueService;
    private final MinioUtils minioUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(WorkOrderCreateDTO dto) {
        // 校验任务存在
        ProjectTaskLookup task = projectTaskLookupMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw BusinessException.of(ResultCode.TASK_NOT_FOUND);
        }
        Long engineerId = dto.getEngineerId() != null ? dto.getEngineerId() : task.getAssigneeId();
        if (engineerId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "任务未指派工程师，无法创建工单");
        }

        WorkOrderEntity entity = new WorkOrderEntity();
        entity.setTaskId(dto.getTaskId());
        entity.setEngineerId(engineerId);
        entity.setStatus(DeliveryConstant.WORK_ORDER_STATUS_CREATED);
        entity.setPhotoCount(0);
        entity.setRemark(dto.getRemark());
        workOrderMapper.insert(entity);

        // 初始化施工步骤
        List<String> steps = (dto.getSteps() != null && !dto.getSteps().isEmpty())
                ? dto.getSteps() : DEFAULT_STEPS;
        workOrderStepService.initSteps(entity.getId(), steps);

        // 推进任务状态 PENDING/ASSIGNED → IN_PROGRESS
        if (DeliveryConstant.TASK_STATUS_PENDING.equals(task.getStatus())
                || DeliveryConstant.TASK_STATUS_ASSIGNED.equals(task.getStatus())) {
            projectTaskLookupMapper.updateTaskStatus(task.getId(),
                    DeliveryConstant.TASK_STATUS_IN_PROGRESS, LocalDate.now(),
                    UserContextHolder.getUserId());
        }

        log.info("[WorkOrder] 创建工单: id={}, taskId={}, engineerId={}",
                entity.getId(), dto.getTaskId(), engineerId);
        return entity.getId();
    }

    @Override
    public PageResult<WorkOrderVO> page(WorkOrderQueryDTO query) {
        IPage<WorkOrderVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<WorkOrderVO> result = workOrderMapper.selectWorkOrderPage(page, query);
        // 填充步骤进度
        if (result.getRecords() != null) {
            for (WorkOrderVO vo : result.getRecords()) {
                fillStepProgress(vo);
                fillCheckinPhotoUrl(vo);
            }
        }
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public WorkOrderVO getDetail(Long id) {
        WorkOrderVO vo = workOrderMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.of(ResultCode.WORK_ORDER_NOT_FOUND);
        }
        fillStepProgress(vo);
        fillCheckinPhotoUrl(vo);
        // 详情附带步骤/照片/异常列表
        vo.setSteps(workOrderStepService.listByWorkOrder(id));
        vo.setPhotos(workOrderPhotoService.listByWorkOrder(id));
        vo.setIssues(workOrderIssueService.listByWorkOrder(id));
        return vo;
    }

    @Override
    public List<WorkOrderVO> listMyWorkOrders(String status) {
        Long userId = UserContextHolder.getUserId();
        List<WorkOrderVO> list = workOrderMapper.selectByEngineer(userId, status);
        if (list != null) {
            for (WorkOrderVO vo : list) {
                fillStepProgress(vo);
                fillCheckinPhotoUrl(vo);
            }
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderVO checkin(Long workOrderId, WorkOrderCheckinDTO dto, MultipartFile photoFile) {
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw BusinessException.of(ResultCode.WORK_ORDER_NOT_FOUND);
        }
        if (!DeliveryConstant.WORK_ORDER_STATUS_CREATED.equals(workOrder.getStatus())) {
            throw BusinessException.stateNotAllowed("当前工单状态不允许签到");
        }

        // GPS 定位校验（Haversine 距离）
        ProjectTaskLookup task = projectTaskLookupMapper.selectById(workOrder.getTaskId());
        String siteInfo = task != null ? task.getSiteInfo() : null;
        SiteInfoUtils.CheckinCheckResult check = SiteInfoUtils.checkLocation(dto.getLocation(), siteInfo);
        if (!check.passed) {
            log.warn("[WorkOrder] 签到 GPS 校验失败: workOrderId={}, userId={}, message={}",
                    workOrderId, UserContextHolder.getUserId(), check.message);
            throw new BusinessException(ResultCode.STATE_NOT_ALLOWED, "签到失败：" + check.message);
        }

        // 上传签到照片（防作弊）
        String checkinPhotoObjectName = null;
        if (photoFile != null && !photoFile.isEmpty()) {
            String dir = String.format(DeliveryConstant.CHECKIN_PHOTO_DIR_FORMAT, workOrderId);
            String original = photoFile.getOriginalFilename() == null ? "checkin.jpg" : photoFile.getOriginalFilename();
            String suffix = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) {
                suffix = original.substring(dot);
            }
            checkinPhotoObjectName = dir + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
            try {
                // 使用字节数组方式上传，自定义 objectName
                minioUtils.upload(photoFile.getBytes(),
                        checkinPhotoObjectName, photoFile.getContentType());
            } catch (Exception e) {
                log.error("[WorkOrder] 签到照片上传失败: workOrderId={}", workOrderId, e);
                throw new BusinessException(ResultCode.MINIO_ERROR, "签到照片上传失败: " + e.getMessage());
            }
        }

        // 记录签到时间与位置
        GpsLocation location = dto.getLocation();
        if (location != null && check.distanceMeters != null) {
            location.setDistanceMeters(check.distanceMeters);
        }
        WorkOrderEntity update = new WorkOrderEntity();
        update.setId(workOrder.getId());
        update.setVersion(workOrder.getVersion());
        update.setStatus(DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN);
        update.setCheckinTime(LocalDateTime.now());
        update.setCheckinLocation(location);
        update.setCheckinPhoto(checkinPhotoObjectName);
        if (StringUtils.hasText(dto.getRemark())) {
            update.setRemark(dto.getRemark());
        }
        workOrderMapper.updateById(update);

        log.info("[WorkOrder] 签到成功: workOrderId={}, engineerId={}, distance={}m",
                workOrderId, workOrder.getEngineerId(), check.distanceMeters);
        return getDetail(workOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderVO checkout(Long workOrderId, WorkOrderCheckoutDTO dto, MultipartFile photoFile) {
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw BusinessException.of(ResultCode.WORK_ORDER_NOT_FOUND);
        }
        String status = workOrder.getStatus();
        if (!DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN.equals(status)
                && !DeliveryConstant.WORK_ORDER_STATUS_IN_PROGRESS.equals(status)) {
            throw BusinessException.stateNotAllowed("当前工单状态不允许签退");
        }

        WorkOrderEntity update = new WorkOrderEntity();
        update.setId(workOrder.getId());
        update.setVersion(workOrder.getVersion());
        update.setCheckoutTime(LocalDateTime.now());
        update.setCheckoutLocation(dto.getLocation());
        // 计算总工时（小时，保留 2 位小数）
        if (workOrder.getCheckinTime() != null) {
            long minutes = Duration.between(workOrder.getCheckinTime(), update.getCheckoutTime()).toMinutes();
            BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            update.setTotalDuration(hours);
        }
        if (StringUtils.hasText(dto.getRemark())) {
            update.setRemark(dto.getRemark());
        }
        workOrderMapper.updateById(update);

        log.info("[WorkOrder] 签退成功: workOrderId={}", workOrderId);
        return getDetail(workOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderVO engineerComplete(Long workOrderId, String remark) {
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw BusinessException.of(ResultCode.WORK_ORDER_NOT_FOUND);
        }
        String status = workOrder.getStatus();
        if (!DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN.equals(status)
                && !DeliveryConstant.WORK_ORDER_STATUS_IN_PROGRESS.equals(status)) {
            throw BusinessException.stateNotAllowed("当前工单状态不允许标记完成");
        }

        // 校验：必须已签退
        if (workOrder.getCheckoutTime() == null) {
            throw BusinessException.stateNotAllowed("请先签退再标记完成");
        }
        // 校验：所有施工步骤必须已完成
        int[] progress = workOrderStepService.calculateProgress(workOrderId);
        if (progress[1] == 0) {
            throw BusinessException.stateNotAllowed("工单无施工步骤，无法完成");
        }
        if (progress[0] < progress[1]) {
            throw BusinessException.stateNotAllowed(
                    String.format("仍有 %d 个步骤未完成，无法标记完成", progress[1] - progress[0]));
        }

        WorkOrderEntity update = new WorkOrderEntity();
        update.setId(workOrder.getId());
        update.setVersion(workOrder.getVersion());
        update.setStatus(DeliveryConstant.WORK_ORDER_STATUS_COMPLETED);
        if (StringUtils.hasText(remark)) {
            update.setRemark(remark);
        }
        workOrderMapper.updateById(update);

        log.info("[WorkOrder] 工程师标记完成: workOrderId={}, engineerId={}",
                workOrderId, workOrder.getEngineerId());
        return getDetail(workOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderVO pmConfirm(Long workOrderId, WorkOrderConfirmDTO dto) {
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw BusinessException.of(ResultCode.WORK_ORDER_NOT_FOUND);
        }
        if (!DeliveryConstant.WORK_ORDER_STATUS_COMPLETED.equals(workOrder.getStatus())) {
            throw BusinessException.stateNotAllowed("仅已完成待确认状态的工单可执行确认");
        }

        WorkOrderEntity update = new WorkOrderEntity();
        update.setId(workOrder.getId());
        update.setVersion(workOrder.getVersion());
        update.setStatus(DeliveryConstant.WORK_ORDER_STATUS_CONFIRMED);
        if (dto != null && StringUtils.hasText(dto.getRemark())) {
            update.setRemark(dto.getRemark());
        }
        workOrderMapper.updateById(update);

        // 自动推进项目任务状态为 CONFIRMED
        ProjectTaskLookup task = projectTaskLookupMapper.selectById(workOrder.getTaskId());
        if (task != null) {
            projectTaskLookupMapper.updateTaskStatus(task.getId(),
                    DeliveryConstant.TASK_STATUS_CONFIRMED, LocalDate.now(),
                    UserContextHolder.getUserId());
            // 自动同步项目进度百分比
            syncProjectProgress(task.getProjectId());
        }

        log.info("[WorkOrder] PM 确认完成: workOrderId={}, taskId={}, pmUserId={}",
                workOrderId, workOrder.getTaskId(), UserContextHolder.getUserId());
        return getDetail(workOrderId);
    }

    /* ============ 私有方法 ============ */

    /**
     * 同步项目进度百分比 = 已完成任务数 / 总任务数 × 100
     */
    private void syncProjectProgress(Long projectId) {
        if (projectId == null) {
            return;
        }
        int total = projectTaskLookupMapper.countTotalByProject(projectId);
        if (total <= 0) {
            return;
        }
        int completed = projectTaskLookupMapper.countCompletedByProject(projectId);
        int progressPct = (int) Math.round(completed * 100.0 / total);
        projectTaskLookupMapper.updateProjectProgress(projectId, progressPct, UserContextHolder.getUserId());
        log.info("[WorkOrder] 项目进度同步: projectId={}, {}/{} = {}%",
                projectId, completed, total, progressPct);
    }

    private void fillStepProgress(WorkOrderVO vo) {
        int[] progress = workOrderStepService.calculateProgress(vo.getId());
        vo.setCompletedStepCount(progress[0]);
        vo.setTotalStepCount(progress[1]);
    }

    /**
     * 为签到照片 objectName 生成预签名 URL
     */
    private void fillCheckinPhotoUrl(WorkOrderVO vo) {
        if (vo != null && StringUtils.hasText(vo.getCheckinPhotoUrl())) {
            try {
                vo.setCheckinPhotoUrl(minioUtils.getPresignedDownloadUrl(vo.getCheckinPhotoUrl()));
            } catch (Exception e) {
                log.warn("签到照片预签名 URL 生成失败: {}", e.getMessage());
            }
        }
    }
}
