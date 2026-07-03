/**
 * 客户协作模块
 *
 * <p>面向 CUSTOMER 角色的 H5 端能力，提供项目进度查看与文档下载。</p>
 *
 * <p>已实现：</p>
 * <ul>
 *   <li>客户 H5 项目整体进度查看（含阶段时间线、当前阶段名称）</li>
 *   <li>客户文档下载（从 project_phase.deliverables 提取，生成 MinIO 预签名 URL）</li>
 * </ul>
 *
 * <p>数据隔离：所有接口通过 {@code UserContextHolder.getTenantId()} 获取客户ID，
 * 并校验项目归属，CUSTOMER 角色仅能访问自己关联的项目。</p>
 *
 * <p>包结构：controller / service / mapper / vo / bo</p>
 *
 * @author vibe
 */
package com.vibe.collaboration;
