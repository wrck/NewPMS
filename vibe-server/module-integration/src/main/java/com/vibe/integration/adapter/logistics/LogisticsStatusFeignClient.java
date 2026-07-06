package com.vibe.integration.adapter.logistics;

import com.vibe.integration.adapter.logistics.dto.LogisticsStatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 物流状态 Feign 客户端
 *
 * <p>对接物流系统 {@code ${integration.logistics.url}} 接口，拉取运单状态。</p>
 *
 * @author vibe
 */
@FeignClient(name = "logistics", url = "${integration.logistics.url}")
public interface LogisticsStatusFeignClient {

    /**
     * 按运单号拉取物流状态。
     *
     * @param trackingNo 运单号
     * @return 物流状态
     */
    @GetMapping("/trackings/{trackingNo}")
    LogisticsStatusDTO pullStatus(@PathVariable("trackingNo") String trackingNo);

    /**
     * 批量拉取物流状态。
     *
     * @param trackingNos 运单号列表
     * @return 物流状态列表
     */
    @PostMapping("/trackings/batch")
    List<LogisticsStatusDTO> batchPullStatus(@RequestBody List<String> trackingNos);
}
