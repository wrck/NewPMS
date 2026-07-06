package com.vibe.integration.adapter.oa;

import com.vibe.integration.adapter.oa.dto.OaApprovalDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OA 审批 Feign 客户端
 *
 * <p>对接 OA 系统 {@code ${integration.oa.url}} 接口，联动项目立项/验收/割接审批流程。</p>
 *
 * @author vibe
 */
@FeignClient(name = "oa-approval", url = "${integration.oa.url}")
public interface OaApprovalFeignClient {

    /**
     * 启动 OA 审批流程。
     *
     * @param dto OA 审批 DTO
     * @return 启动结果（含 OA 流程实例 ID）
     */
    @PostMapping("/processes/start")
    OaApprovalDTO startApproval(@RequestBody OaApprovalDTO dto);

    /**
     * 查询 OA 审批流程状态。
     *
     * @param oaProcessId OA 流程实例 ID
     * @return 审批状态详情
     */
    @GetMapping("/processes/{oaProcessId}")
    OaApprovalDTO queryStatus(@PathVariable("oaProcessId") String oaProcessId);

    /**
     * 取消 OA 审批流程。
     *
     * @param oaProcessId OA 流程实例 ID
     * @param reason      取消原因
     */
    @PostMapping("/processes/{oaProcessId}/cancel")
    OaApprovalDTO cancelApproval(@PathVariable("oaProcessId") String oaProcessId,
                                 @RequestBody String reason);
}
