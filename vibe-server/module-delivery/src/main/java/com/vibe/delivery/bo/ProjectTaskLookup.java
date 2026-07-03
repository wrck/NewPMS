package com.vibe.delivery.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目任务轻量查询 BO（仅 module-delivery 内部使用）
 *
 * <p>对应 project_task 表的最小字段集合：id / projectId / taskName / assigneeId / status / siteInfo。</p>
 *
 * @author vibe
 */
@Data
public class ProjectTaskLookup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long id;

    /** 项目ID */
    private Long projectId;

    /** 任务名称 */
    private String taskName;

    /** 执行人ID（自有工程师 user_id） */
    private Long assigneeId;

    /** 任务状态 */
    private String status;

    /** 计划开始 */
    private LocalDate plannedStart;

    /** 计划结束 */
    private LocalDate plannedEnd;

    /** 实际开始 */
    private LocalDate actualStart;

    /** 实际结束 */
    private LocalDate actualEnd;

    /**
     * 关联站点信息（JSON 解析为字符串透传，由业务层解析）。
     * <p>约定 site_info 中可包含：</p>
     * <pre>
     * {
     *   "siteName": "XX银行核心机房",
     *   "address": "北京市朝阳区XX大厦",
     *   "contactName": "王经理",
     *   "contactPhone": "138xxxx1234",
     *   "expectedLatitude": 39.9042,
     *   "expectedLongitude": 116.4074,
     *   "allowedRadiusMeters": 500
     * }
     * </pre>
     */
    private String siteInfo;
}
