/**
 * 集成管理模块
 *
 * <p>本模块为外部系统连接配置与调用日志管理。</p>
 *
 * <p>Phase 1（MVP）实现：</p>
 * <ul>
 *   <li>集成配置 CRUD：管理 ERP/NMS/飞书/钉钉/OA/物流等外部系统的连接信息</li>
 *   <li>调用日志查询：记录与外部系统的调用历史，便于审计与故障排查</li>
 *   <li>测试连接：手工触发连接性测试，更新最近调用状态</li>
 * </ul>
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
 * <p>包结构：controller / service / entity / dto / vo / constant</p>
 *
 * @author vibe
 */
package com.vibe.integration;
