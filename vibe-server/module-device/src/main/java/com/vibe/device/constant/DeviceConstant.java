package com.vibe.device.constant;

/**
 * 设备模块常量。
 *
 * @author vibe
 */
public final class DeviceConstant {

    private DeviceConstant() {
    }

    /* ============ 角色编码 ============ */
    /** 超级管理员 */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    /** 设备管理员 */
    public static final String ROLE_DEVICE_ADMIN = "DEVICE_ADMIN";
    /** 项目经理 */
    public static final String ROLE_PM = "PM";
    /** 实施工程师 */
    public static final String ROLE_ENGINEER = "ENGINEER";

    /** 设备管理角色（可写） */
    public static final String[] DEVICE_MANAGE_ROLES = {ROLE_SUPER_ADMIN, ROLE_DEVICE_ADMIN};
    /** 设备只读角色 */
    public static final String[] DEVICE_READ_ROLES = {ROLE_SUPER_ADMIN, ROLE_DEVICE_ADMIN, ROLE_PM, ROLE_ENGINEER};

    /* ============ 默认状态 ============ */
    /** 设备初始状态 */
    public static final String DEFAULT_DEVICE_STATUS = "IN_FACTORY";

    /* ============ 出入库操作类型 ============ */
    public static final String ACTION_IN = "IN";
    public static final String ACTION_OUT = "OUT";
    public static final String ACTION_RETURN = "RETURN";
    public static final String ACTION_TRANSFER = "TRANSFER";

    /* ============ 备件状态 ============ */
    public static final Integer SPARE_PART_ENABLED = 1;
    public static final Integer SPARE_PART_DISABLED = 0;

    /* ============ 操作日志模块名 ============ */
    public static final String MODULE_DEVICE_MODEL = "设备型号库";
    public static final String MODULE_DEVICE_INSTANCE = "设备实例";
    public static final String MODULE_DEVICE_BOM = "项目设备清单";
    public static final String MODULE_DEVICE_INVENTORY = "设备出入库";
    public static final String MODULE_SPARE_PART = "备件管理";
    public static final String MODULE_WAREHOUSE = "仓库管理";
    public static final String MODULE_DEVICE_STATUS = "设备状态流转";

    /* ============ Excel 导入 ============ */
    /** 批量导入单次最大行数 */
    public static final int IMPORT_MAX_ROWS = 5000;
}
