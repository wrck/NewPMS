package com.vibe.delivery.constant;

/**
 * 交付管理模块常量
 *
 * <p>定义工单/步骤/异常问题相关状态、默认签到允许半径、照片处理参数等。</p>
 *
 * @author vibe
 */
public final class DeliveryConstant {

    private DeliveryConstant() {
    }

    /* ============ 工单状态 ============ */
    /** 已创建（未签到） */
    public static final String WORK_ORDER_STATUS_CREATED = "CREATED";
    /** 已签到（进行中） */
    public static final String WORK_ORDER_STATUS_CHECKED_IN = "CHECKED_IN";
    /** 进行中（已开始施工步骤） */
    public static final String WORK_ORDER_STATUS_IN_PROGRESS = "IN_PROGRESS";
    /** 工程师已标记完成（待 PM 确认） */
    public static final String WORK_ORDER_STATUS_COMPLETED = "COMPLETED";
    /** PM 已确认 */
    public static final String WORK_ORDER_STATUS_CONFIRMED = "CONFIRMED";

    /* ============ 步骤状态 ============ */
    /** 待完成 */
    public static final String STEP_STATUS_WAITING = "WAITING";
    /** 已完成 */
    public static final String STEP_STATUS_COMPLETED = "COMPLETED";
    /** 已跳过 */
    public static final String STEP_STATUS_SKIPPED = "SKIPPED";

    /* ============ 异常问题状态 ============ */
    public static final String ISSUE_STATUS_OPEN = "OPEN";
    public static final String ISSUE_STATUS_PROCESSING = "PROCESSING";
    public static final String ISSUE_STATUS_RESOLVED = "RESOLVED";
    public static final String ISSUE_STATUS_CLOSED = "CLOSED";

    /* ============ 异常严重程度 ============ */
    public static final String ISSUE_SEVERITY_MINOR = "MINOR";
    public static final String ISSUE_SEVERITY_MAJOR = "MAJOR";
    public static final String ISSUE_SEVERITY_BLOCKING = "BLOCKING";

    /* ============ 默认签到允许半径（米），site_info 未配置时使用 ============ */
    public static final double DEFAULT_CHECKIN_RADIUS_METERS = 1000.0d;

    /* ============ 照片处理参数 ============ */
    /** 压缩质量 0.85 */
    public static final float PHOTO_COMPRESS_QUALITY = 0.85f;
    /** 长边上限 2048px */
    public static final int PHOTO_MAX_LONG_EDGE = 2048;
    /** 缩略图宽 */
    public static final int THUMBNAIL_WIDTH = 320;
    /** 缩略图高 */
    public static final int THUMBNAIL_HEIGHT = 320;

    /* ============ MinIO 目录 ============ */
    public static final String PHOTO_DIR_FORMAT = "work-order/%d/photos";
    public static final String CHECKIN_PHOTO_DIR_FORMAT = "work-order/%d/checkin";

    /* ============ 项目任务状态（同步用，来源于 project_task 表） ============ */
    public static final String TASK_STATUS_PENDING = "PENDING";
    public static final String TASK_STATUS_ASSIGNED = "ASSIGNED";
    public static final String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_STATUS_COMPLETED = "COMPLETED";
    public static final String TASK_STATUS_CONFIRMED = "CONFIRMED";
}
