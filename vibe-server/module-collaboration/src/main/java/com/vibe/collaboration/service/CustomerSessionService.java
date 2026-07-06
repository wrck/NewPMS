package com.vibe.collaboration.service;

import com.vibe.common.base.PageQuery;
import com.vibe.common.result.PageResult;
import com.vibe.collaboration.dto.CustomerSessionDTO;
import com.vibe.collaboration.vo.CustomerSessionVO;

import java.util.List;

/**
 * 客户会话服务
 *
 * <p>提供客户会话的 CRUD 基础方法与按客户/token 查询的业务方法，
 * 支持会话分页查询与强制下线（删除会话即 token 失效）。</p>
 *
 * @author vibe
 */
public interface CustomerSessionService {

    /* ============ CRUD 基础方法 ============ */

    /**
     * 查询会话详情。
     */
    CustomerSessionVO getDetail(Long id);

    /**
     * 新增会话（主要由登录流程调用）。
     */
    Long create(CustomerSessionDTO dto);

    /**
     * 更新会话。
     */
    void update(CustomerSessionDTO dto);

    /**
     * 删除会话（逻辑删除，等同于强制下线：token 不再可查即失效）。
     */
    void delete(Long id);

    /* ============ 业务方法 ============ */

    /**
     * 查询指定客户的全部会话。
     */
    List<CustomerSessionVO> listByCustomerId(Long customerId);

    /**
     * 按 token 查询会话（用于登录态校验，仅返回未删除记录）。
     */
    CustomerSessionVO getByToken(String loginToken);

    /**
     * 分页查询指定客户的会话列表。
     */
    PageResult<CustomerSessionVO> pageByCustomerId(Long customerId, PageQuery query);

    /**
     * 强制下线：将指定会话状态置为 REVOKED 并逻辑删除，使 token 立即失效。
     *
     * @param id 会话ID
     */
    void forceOffline(Long id);
}
