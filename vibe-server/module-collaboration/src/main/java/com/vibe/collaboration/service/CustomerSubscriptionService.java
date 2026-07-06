package com.vibe.collaboration.service;

import com.vibe.collaboration.dto.CustomerSubscriptionDTO;
import com.vibe.collaboration.vo.CustomerSubscriptionVO;

import java.util.List;

/**
 * 客户订阅服务
 *
 * <p>提供客户订阅的 CRUD 基础方法与按客户/事件类型查询的业务方法。</p>
 *
 * @author vibe
 */
public interface CustomerSubscriptionService {

    /* ============ CRUD 基础方法 ============ */

    /**
     * 查询订阅详情。
     */
    CustomerSubscriptionVO getDetail(Long id);

    /**
     * 新增订阅。
     */
    Long create(CustomerSubscriptionDTO dto);

    /**
     * 更新订阅。
     */
    void update(CustomerSubscriptionDTO dto);

    /**
     * 删除订阅。
     */
    void delete(Long id);

    /* ============ 业务方法 ============ */

    /**
     * 查询指定客户的全部订阅。
     */
    List<CustomerSubscriptionVO> listByCustomerId(Long customerId);

    /**
     * 按客户ID + 事件类型查询订阅。
     */
    CustomerSubscriptionVO getByCustomerIdAndEventType(Long customerId, String eventType);

    /**
     * 批量保存指定客户的订阅（upsert：存在则更新，不存在则新增）。
     *
     * @param customerId 客户ID
     * @param dtos       订阅列表（customerId 由本方法统一填充）
     */
    void updateSubscriptions(Long customerId, List<CustomerSubscriptionDTO> dtos);
}
