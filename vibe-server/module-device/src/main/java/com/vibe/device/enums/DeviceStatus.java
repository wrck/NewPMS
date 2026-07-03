package com.vibe.device.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 设备状态机枚举
 *
 * <p>正常流转：IN_FACTORY → SHIPPED → RECEIVED → PRE_CONFIG → INSTALLED → DEBUGGED → ONLINE</p>
 * <p>异常分支：任意正常状态 → DAMAGED / LOST / REPAIR；RECEIVED → RETURNED；
 * ONLINE → REPLACED / EOL；REPAIR 可恢复到正常状态。</p>
 *
 * <pre>
 * IN_FACTORY(在库)
 *     │ 出库发运（分配项目）
 *     ▼
 * SHIPPED(已发运)
 *     │ 到货签收
 *     ▼
 * RECEIVED(已到货)
 *     │ 预配置（Phase 1 可选）
 *     ▼
 * PRE_CONFIG(已预配)
 *     │ 现场安装上架
 *     ▼
 * INSTALLED(已安装)
 *     │ 调试通过
 *     ▼
 * DEBUGGED(已调试)
 *     │ 割接上线
 *     ▼
 * ONLINE(在网运行)
 * </pre>
 *
 * @author vibe
 */
@Getter
public enum DeviceStatus {

    /** 在库（出厂入库） */
    IN_FACTORY("在库"),
    /** 已发运 */
    SHIPPED("已发运"),
    /** 已到货 */
    RECEIVED("已到货"),
    /** 已预配 */
    PRE_CONFIG("已预配"),
    /** 已安装 */
    INSTALLED("已安装"),
    /** 已调试 */
    DEBUGGED("已调试"),
    /** 在网运行 */
    ONLINE("在网运行"),
    /** 损坏 */
    DAMAGED("损坏"),
    /** 遗失 */
    LOST("遗失"),
    /** 已退货 */
    RETURNED("已退货"),
    /** 返修中 */
    REPAIR("返修中"),
    /** 已替换 */
    REPLACED("已替换"),
    /** 退网/报废 */
    EOL("退网/报废");

    /** 状态展示名 */
    private final String displayName;

    /** 正常流转状态集合 */
    private static final Set<DeviceStatus> NORMAL_STATES = EnumSet.of(
            IN_FACTORY, SHIPPED, RECEIVED, PRE_CONFIG, INSTALLED, DEBUGGED, ONLINE);

    /** 终态集合（不可再向前流转） */
    private static final Set<DeviceStatus> TERMINAL_STATES = EnumSet.of(
            DAMAGED, LOST, RETURNED, REPLACED, EOL);

    DeviceStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 是否为正常流转状态
     */
    public boolean isNormal() {
        return NORMAL_STATES.contains(this);
    }

    /**
     * 是否为终态
     */
    public boolean isTerminal() {
        return TERMINAL_STATES.contains(this);
    }

    /**
     * 校验状态流转是否合法。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>相同状态：非法（无意义）</li>
     *   <li>终态 → 任意：非法（终态不可变更，仅 EOL 可由人工强制归档）</li>
     *   <li>正常状态 → DAMAGED / LOST / REPAIR：合法（异常登记）</li>
     *   <li>正常状态 → EOL：合法（退网/报废）</li>
     *   <li>RECEIVED → PRE_CONFIG / INSTALLED / RETURNED：合法</li>
     *   <li>PRE_CONFIG → INSTALLED：合法</li>
     *   <li>REPAIR → INSTALLED / DEBUGGED / ONLINE：合法（返修恢复）</li>
     *   <li>RETURNED → IN_FACTORY：合法（退库重新入库）</li>
     *   <li>ONLINE → REPLACED：合法</li>
     *   <li>正向主流程一步流转：合法</li>
     * </ul>
     *
     * @param from 变更前状态
     * @param to   变更后状态
     * @return true 合法；false 非法
     */
    public static boolean canTransition(DeviceStatus from, DeviceStatus to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        // 终态不可再流转（避免已报废/已替换设备被重新激活）
        if (from.isTerminal()) {
            return false;
        }
        // 任意正常状态 → DAMAGED / LOST / REPAIR / EOL：异常登记或退网
        if (from.isNormal() && (to == DAMAGED || to == LOST || to == REPAIR || to == EOL)) {
            return true;
        }
        // 正向主流程
        switch (from) {
            case IN_FACTORY:
                return to == SHIPPED;
            case SHIPPED:
                return to == RECEIVED;
            case RECEIVED:
                return to == PRE_CONFIG || to == INSTALLED || to == RETURNED;
            case PRE_CONFIG:
                return to == INSTALLED;
            case INSTALLED:
                return to == DEBUGGED;
            case DEBUGGED:
                return to == ONLINE;
            case ONLINE:
                return to == REPLACED;
            case REPAIR:
                // 返修完成后可恢复到安装/调试/在网
                return to == INSTALLED || to == DEBUGGED || to == ONLINE;
            case RETURNED:
                // 退货后重新入库
                return to == IN_FACTORY;
            default:
                return false;
        }
    }

    /**
     * 解析状态字符串（忽略大小写），非法时返回 null。
     */
    public static DeviceStatus parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return DeviceStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
