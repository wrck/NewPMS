package com.vibe.report.service;

import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.CockpitStatVO;
import com.vibe.report.vo.RiskProjectVO;

import java.util.List;

/**
 * 管理驾驶舱服务
 *
 * <p>提供核心指标卡片、项目阶段分布、项目趋势、风险项目等全局数据。</p>
 *
 * @author vibe
 */
public interface ManagementCockpitService {

    /**
     * 获取核心指标卡片（含环比数据）
     */
    CockpitStatVO getStats();

    /**
     * 获取项目阶段分布饼图
     */
    List<ChartDataVO> getProjectPhaseDistribution();

    /**
     * 获取近12月项目趋势折线图
     */
    List<ChartDataVO> getProjectTrend();

    /**
     * 获取风险项目列表
     */
    List<RiskProjectVO> getRiskProjects();
}
