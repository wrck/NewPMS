package com.vibe.es.index;

/**
 * ElasticSearch 索引名常量
 *
 * <p>集中声明系统使用的 ES 索引名，避免硬编码散落各业务类。</p>
 *
 * @author vibe
 */
public final class EsIndexConstant {

    private EsIndexConstant() {
    }

    /** 项目索引 */
    public static final String INDEX_VIBE_PROJECT = "vibe_project";
    /** 设备索引 */
    public static final String INDEX_VIBE_DEVICE = "vibe_device";
    /** 工单索引 */
    public static final String INDEX_VIBE_WORK_ORDER = "vibe_work_order";
}
