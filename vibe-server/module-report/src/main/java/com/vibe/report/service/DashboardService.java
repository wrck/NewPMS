package com.vibe.report.service;

import com.vibe.report.vo.DashboardVO;

/**
 * 工作台首页服务
 *
 * <p>根据当前登录用户角色返回差异化首页数据：</p>
 * <ul>
 *   <li>DIRECTOR / SUPER_ADMIN → 全局总览 + 审批待办 + 核心指标</li>
 *   <li>PM → 我的项目 + 待派单 + 待审核交付物</li>
 *   <li>ENGINEER → 今日任务 + 工时统计</li>
 *   <li>AGENT_ADMIN / AGENT_ENGINEER → 任务概况 + 待接单 + 进行中</li>
 * </ul>
 *
 * @author vibe
 */
public interface DashboardService {

    /**
     * 获取当前登录用户的工作台首页数据
     *
     * @return 按角色填充的首页 VO
     */
    DashboardVO getDashboard();
}
