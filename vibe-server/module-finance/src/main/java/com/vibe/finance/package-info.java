/**
 * 财务核算模块（设计文档 2.8）
 *
 * <p>实现内容：</p>
 * <ul>
 *   <li>项目预算（人工/差旅/代理商/其他）+ 审批 + 调整 + 预算 vs 实际对比</li>
 *   <li>成本归集（人工成本/差旅费用/代理商费用/其他费用 → 项目维度）</li>
 *   <li>代理商结算（工作量确认单 → 费用计算 → 对账 → 审批流 → 付款跟踪）</li>
 *   <li>利润分析（项目级利润 / 毛利率 / 多维度分析 / 趋势分析）</li>
 * </ul>
 *
 * <p>包结构：controller / service / mapper / entity / dto / vo / constant</p>
 *
 * @author vibe
 */
package com.vibe.finance;
