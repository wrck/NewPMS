package com.vibe.lowcode.constant;

/**
 * 低代码配置模块常量
 *
 * @author vibe
 */
public final class LowcodeConstant {

    private LowcodeConstant() {}

    /** 配置状态：启用 */
    public static final Integer STATUS_ENABLED = 1;
    /** 配置状态：禁用 */
    public static final Integer STATUS_DISABLED = 0;

    /** 模板类型：表单 */
    public static final String TEMPLATE_TYPE_FORM = "FORM";
    /** 模板类型：列表 */
    public static final String TEMPLATE_TYPE_LIST = "LIST";
    /** 模板类型：标签页 */
    public static final String TEMPLATE_TYPE_TAB = "TAB";
    /** 模板类型：关联页 */
    public static final String TEMPLATE_TYPE_RELATION = "RELATION";

    /** 复制配置时新名称后缀 */
    public static final String COPY_NAME_SUFFIX = "_copy";

    /** 实例化时新名称前缀 */
    public static final String INSTANTIATE_NAME_PREFIX = "基于模板:";
}
