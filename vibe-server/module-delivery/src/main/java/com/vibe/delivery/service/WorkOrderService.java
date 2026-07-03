package com.vibe.delivery.service;

import com.vibe.common.result.PageResult;
import com.vibe.delivery.dto.WorkOrderCheckinDTO;
import com.vibe.delivery.dto.WorkOrderCheckoutDTO;
import com.vibe.delivery.dto.WorkOrderConfirmDTO;
import com.vibe.delivery.dto.WorkOrderCreateDTO;
import com.vibe.delivery.dto.WorkOrderQueryDTO;
import com.vibe.delivery.vo.WorkOrderVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 工单服务
 *
 * <p>核心能力：创建工单 / 现场签到（GPS 校验+拍照）/ 签退 / 工程师完成 / PM 确认 / 查询。</p>
 *
 * @author vibe
 */
public interface WorkOrderService {

    /**
     * 创建工单（关联 project_task_id 与 engineer_id），并按传入步骤或默认模板初始化施工步骤
     *
     * @return 工单ID
     */
    Long createWorkOrder(WorkOrderCreateDTO dto);

    /**
     * 分页查询工单列表（ENGINEER 仅看自己的工单，由 @DataPermission 拦截）
     */
    PageResult<WorkOrderVO> page(WorkOrderQueryDTO query);

    /**
     * 工单详情（含步骤/照片/异常列表）
     */
    WorkOrderVO getDetail(Long id);

    /**
     * 移动端：按工程师查询工单列表（我的任务）
     */
    List<WorkOrderVO> listMyWorkOrders(String status);

    /**
     * 现场签到：
     * <ol>
     *   <li>GPS 定位校验（Haversine 距离，超出范围阻止签到）</li>
     *   <li>上传签到照片到 MinIO（防作弊）</li>
     *   <li>记录签到时间与位置 JSON</li>
     *   <li>状态 CREATED → CHECKED_IN</li>
     * </ol>
     *
     * @param workOrderId 工单ID
     * @param dto         签到 DTO（含 GPS）
     * @param photoFile   签到照片（可选，前端可只传 GPS）
     * @return 签到后的工单视图
     */
    WorkOrderVO checkin(Long workOrderId, WorkOrderCheckinDTO dto, MultipartFile photoFile);

    /**
     * 签退：记录签退时间与位置
     *
     * @param workOrderId 工单ID
     * @param dto         签退 DTO（含 GPS）
     * @param photoFile   签退照片（可选）
     * @return 签退后的工单视图
     */
    WorkOrderVO checkout(Long workOrderId, WorkOrderCheckoutDTO dto, MultipartFile photoFile);

    /**
     * 工程师标记完成：
     * <ul>
     *   <li>要求所有施工步骤已完成</li>
     *   <li>要求已签退</li>
     *   <li>状态 IN_PROGRESS → COMPLETED</li>
     * </ul>
     */
    WorkOrderVO engineerComplete(Long workOrderId, String remark);

    /**
     * PM 确认完成：
     * <ul>
     *   <li>状态 COMPLETED → CONFIRMED</li>
     *   <li>自动推进 project_task 状态为 CONFIRMED</li>
     *   <li>自动同步项目进度百分比</li>
     * </ul>
     */
    WorkOrderVO pmConfirm(Long workOrderId, WorkOrderConfirmDTO dto);
}
