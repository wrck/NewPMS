/**
 * 集成管理模块（Phase 2/3 占位）
 *
 * <p>本模块为 Phase 2/3 阶段实现，MVP（Phase 1）阶段仅保留空骨架占位。</p>
 *
 * <p>Phase 2/3 计划实现（适配器模式 + 事件驱动）：</p>
 * <ul>
 *   <li>CRM/ERP 集成（SAP/Oracle/用友）：销售订单入站自动创建项目、设备发货通知、客主数据同步、项目结项开票回写</li>
 *   <li>NMS 网管集成（eSight/iMaster NCE）：设备上线事件、告警关联、新设备注册、配置下发</li>
 *   <li>IM 平台集成（飞书/钉钉/企微）：任务派发、割接审批、验收签核、风险预警、进度日报通知</li>
 *   <li>物流平台集成：物流轨迹更新、签收通知、创建发货单</li>
 *   <li>OA/财务系统集成：审批流对接、组织架构同步、费用报销同步</li>
 *   <li>统一适配器接口 {@code ExternalSystemAdapter}、熔断降级（Resilience4j）、重试机制</li>
 * </ul>
 *
 * <p>包结构将在 Phase 2/3 实施时按需创建：
 * controller / service / adapter / entity / dto / vo / bo / enums / constant / event</p>
 *
 * @author vibe
 */
package com.vibe.integration;
