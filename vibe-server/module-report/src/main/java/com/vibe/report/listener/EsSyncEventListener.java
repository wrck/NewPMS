package com.vibe.report.listener;

import com.vibe.es.ElasticSearchService;
import com.vibe.es.index.EsIndexConstant;
import com.vibe.es.index.VibeDeviceIndex;
import com.vibe.es.index.VibeProjectIndex;
import com.vibe.es.index.VibeWorkOrderIndex;
import com.vibe.event.DomainEventConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 领域事件 → ElasticSearch 同步监听器
 *
 * <p>监听 RabbitMQ 队列 {@code vibe.es.sync.queue}，消费领域事件总线上的全部事件，
 * 按事件类型更新对应 ES 索引，实现 MySQL → ES 的近实时增量同步。</p>
 *
 * <p>本监听器是 module-common 中已存在 {@code EntityEventListener} 的扩展版本，
 * 迁移至 module-report（更贴近检索场景）并新增任务类事件的索引同步。</p>
 *
 * <p>事件路由：</p>
 * <ul>
 *   <li>{@code PROJECT_CREATED} → 写入 {@code vibe_project} 索引（新增文档）</li>
 *   <li>{@code PROJECT_STATUS_CHANGED} → 更新 {@code vibe_project} 索引（status/phase）</li>
 *   <li>{@code DEVICE_STATUS_CHANGED} → 更新 {@code vibe_device} 索引（status）</li>
 *   <li>{@code TASK_ASSIGNED} → 写入 {@code vibe_work_order} 索引（任务派发占位文档）</li>
 *   <li>{@code TASK_COMPLETED} → 更新 {@code vibe_work_order} 索引（status=COMPLETED）</li>
 *   <li>{@code WORK_ORDER_COMPLETED} → 更新 {@code vibe_work_order} 索引（status=CONFIRMED/actualEnd）</li>
 *   <li>其他事件：仅记日志，不影响业务</li>
 * </ul>
 *
 * <p>容错策略：</p>
 * <ul>
 *   <li>ES 不可达时仅记日志，消息正常 ACK，避免无限重试堆积</li>
 *   <li>反序列化使用 {@code Map<String,Object>}，避免 DomainEvent 抽象类无法实例化问题，
 *       且兼容多事件类型混合消费</li>
 *   <li>消费失败不影响主业务（ES 同步是最终一致性的旁路）</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsSyncEventListener {

    private final ElasticSearchService<Object> elasticSearchService;

    /**
     * 消费领域事件，同步更新 ES 索引。
     *
     * <p>使用 {@code Map<String,Object>} 接收消息体，由 {@code Jackson2JsonMessageConverter}
     * 反序列化为通用 Map，按 eventType 字段分发处理。</p>
     *
     * @param eventMap 事件体（JSON 反序列化为 Map）
     */
    @RabbitListener(queues = DomainEventConstant.ES_SYNC_QUEUE)
    public void onDomainEvent(@Payload Map<String, Object> eventMap) {
        if (eventMap == null) {
            log.warn("收到空领域事件，ACK 丢弃");
            return;
        }
        String eventType = asString(eventMap.get("eventType"));
        String businessKey = asString(eventMap.get("businessKey"));
        String eventId = asString(eventMap.get("eventId"));
        log.info("收到领域事件（ES 同步）: eventId={}, eventType={}, businessKey={}",
                eventId, eventType, businessKey);
        if (eventType == null || eventType.isBlank()) {
            log.warn("领域事件 eventType 为空，ACK 丢弃: eventMap={}", eventMap);
            return;
        }
        try {
            dispatch(eventType, eventMap);
        } catch (Exception e) {
            // ES 同步失败仅记日志，不抛异常（ACK 丢弃），保证业务不受影响
            log.error("领域事件 ES 同步失败（ACK 丢弃）: eventId={}, eventType={}, error={}",
                    eventId, eventType, e.getMessage(), e);
        }
    }

    /**
     * 按事件类型分发到对应的 ES 索引更新逻辑。
     */
    private void dispatch(String eventType, Map<String, Object> eventMap) {
        switch (eventType) {
            case DomainEventConstant.EVENT_PROJECT_CREATED ->
                    handleProjectCreated(eventMap);
            case DomainEventConstant.EVENT_PROJECT_STATUS_CHANGED ->
                    handleProjectStatusChanged(eventMap);
            case DomainEventConstant.EVENT_DEVICE_STATUS_CHANGED ->
                    handleDeviceStatusChanged(eventMap);
            case DomainEventConstant.EVENT_TASK_ASSIGNED ->
                    handleTaskAssigned(eventMap);
            case DomainEventConstant.EVENT_TASK_COMPLETED ->
                    handleTaskCompleted(eventMap);
            case DomainEventConstant.EVENT_WORK_ORDER_COMPLETED ->
                    handleWorkOrderCompleted(eventMap);
            default ->
                    log.debug("领域事件类型 {} 不需要 ES 同步，跳过", eventType);
        }
    }

    /**
     * 处理项目立项事件：写入 vibe_project 索引。
     */
    private void handleProjectCreated(Map<String, Object> eventMap) {
        Long projectId = asLong(eventMap.get("projectId"));
        if (projectId == null) {
            log.warn("PROJECT_CREATED 事件缺少 projectId，跳过: eventMap={}", eventMap);
            return;
        }
        VibeProjectIndex idx = new VibeProjectIndex();
        idx.setId(projectId);
        idx.setName(asString(eventMap.get("projectName")));
        idx.setCustomerName(asString(eventMap.get("customerName")));
        idx.setPmId(asLong(eventMap.get("pmId")));
        idx.setCreatedAt(LocalDateTime.now());
        boolean ok = elasticSearchService.index(EsIndexConstant.INDEX_VIBE_PROJECT,
                String.valueOf(projectId), idx);
        log.info("项目立项事件 ES 同步完成: projectId={}, success={}", projectId, ok);
    }

    /**
     * 处理项目状态变更事件：更新 vibe_project 索引的 status/phase。
     */
    private void handleProjectStatusChanged(Map<String, Object> eventMap) {
        Long projectId = asLong(eventMap.get("projectId"));
        if (projectId == null) {
            log.warn("PROJECT_STATUS_CHANGED 事件缺少 projectId，跳过: eventMap={}", eventMap);
            return;
        }
        VibeProjectIndex idx = new VibeProjectIndex();
        idx.setId(projectId);
        idx.setStatus(asString(eventMap.get("toStatus")));
        idx.setPhase(asString(eventMap.get("currentPhase")));
        boolean ok = elasticSearchService.index(EsIndexConstant.INDEX_VIBE_PROJECT,
                String.valueOf(projectId), idx);
        log.info("项目状态变更事件 ES 同步完成: projectId={}, status={}, success={}",
                projectId, idx.getStatus(), ok);
    }

    /**
     * 处理设备状态变更事件：更新 vibe_device 索引的 status。
     */
    private void handleDeviceStatusChanged(Map<String, Object> eventMap) {
        Long deviceId = asLong(eventMap.get("deviceId"));
        if (deviceId == null) {
            log.warn("DEVICE_STATUS_CHANGED 事件缺少 deviceId，跳过: eventMap={}", eventMap);
            return;
        }
        VibeDeviceIndex idx = new VibeDeviceIndex();
        idx.setId(deviceId);
        idx.setSn(asString(eventMap.get("sn")));
        idx.setStatus(asString(eventMap.get("toStatus")));
        idx.setProjectId(asLong(eventMap.get("projectId")));
        boolean ok = elasticSearchService.index(EsIndexConstant.INDEX_VIBE_DEVICE,
                String.valueOf(deviceId), idx);
        log.info("设备状态变更事件 ES 同步完成: deviceId={}, status={}, success={}",
                deviceId, idx.getStatus(), ok);
    }

    /**
     * 处理任务派发事件：写入 vibe_work_order 索引占位文档。
     *
     * <p>任务派发时尚未生成工单，但可在工单索引中预创建一个以 taskId 为 ID 的占位文档，
     * 便于工单列表查询时按任务维度检索。后续 WorkOrderCompletedEvent 会更新该文档或新建工单文档。</p>
     */
    private void handleTaskAssigned(Map<String, Object> eventMap) {
        Long taskId = asLong(eventMap.get("taskId"));
        if (taskId == null) {
            log.warn("TASK_ASSIGNED 事件缺少 taskId，跳过: eventMap={}", eventMap);
            return;
        }
        VibeWorkOrderIndex idx = new VibeWorkOrderIndex();
        idx.setId(taskId);
        idx.setTaskName(asString(eventMap.get("taskName")));
        idx.setProjectId(asLong(eventMap.get("projectId")));
        idx.setStatus("ASSIGNED");
        boolean ok = elasticSearchService.index(EsIndexConstant.INDEX_VIBE_WORK_ORDER,
                "task-" + taskId, idx);
        log.info("任务派发事件 ES 同步完成: taskId={}, success={}", taskId, ok);
    }

    /**
     * 处理任务完成事件：更新 vibe_work_order 索引（status=COMPLETED）。
     *
     * <p>使用 taskId 关联到任务派发时创建的占位文档（ID 形如 {@code task-{taskId}}）。</p>
     */
    private void handleTaskCompleted(Map<String, Object> eventMap) {
        Long taskId = asLong(eventMap.get("taskId"));
        if (taskId == null) {
            log.warn("TASK_COMPLETED 事件缺少 taskId，跳过: eventMap={}", eventMap);
            return;
        }
        VibeWorkOrderIndex idx = new VibeWorkOrderIndex();
        idx.setId(taskId);
        idx.setTaskName(asString(eventMap.get("taskName")));
        idx.setProjectId(asLong(eventMap.get("projectId")));
        idx.setStatus("COMPLETED");
        idx.setActualEnd(LocalDateTime.now());
        boolean ok = elasticSearchService.index(EsIndexConstant.INDEX_VIBE_WORK_ORDER,
                "task-" + taskId, idx);
        log.info("任务完成事件 ES 同步完成: taskId={}, success={}", taskId, ok);
    }

    /**
     * 处理工单完成事件：更新 vibe_work_order 索引的 status/actualEnd。
     */
    private void handleWorkOrderCompleted(Map<String, Object> eventMap) {
        Long workOrderId = asLong(eventMap.get("workOrderId"));
        if (workOrderId == null) {
            log.warn("WORK_ORDER_COMPLETED 事件缺少 workOrderId，跳过: eventMap={}", eventMap);
            return;
        }
        VibeWorkOrderIndex idx = new VibeWorkOrderIndex();
        idx.setId(workOrderId);
        idx.setProjectId(asLong(eventMap.get("projectId")));
        idx.setEngineerId(asLong(eventMap.get("engineerId")));
        idx.setEngineerName(asString(eventMap.get("engineerName")));
        idx.setStatus("CONFIRMED");
        Object actualEnd = eventMap.get("actualEnd");
        if (actualEnd instanceof java.util.Map<?, ?> ts
                && ts.get("nano") != null && ts.get("epochSecond") != null) {
            // LocalDateTime 序列化为 {epochSecond, nano} 形式（默认）
            idx.setActualEnd(LocalDateTime.ofEpochSecond(
                    ((Number) ts.get("epochSecond")).longValue(),
                    ((Number) ts.get("nano")).intValue(),
                    java.time.ZoneOffset.UTC));
        } else {
            idx.setActualEnd(LocalDateTime.now());
        }
        boolean ok = elasticSearchService.index(EsIndexConstant.INDEX_VIBE_WORK_ORDER,
                String.valueOf(workOrderId), idx);
        log.info("工单完成事件 ES 同步完成: workOrderId={}, success={}", workOrderId, ok);
    }

    /* ============ 类型转换工具方法 ============ */

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Long asLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
