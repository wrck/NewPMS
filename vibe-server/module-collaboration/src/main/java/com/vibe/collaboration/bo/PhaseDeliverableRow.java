package com.vibe.collaboration.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 阶段交付物中间 BO
 *
 * <p>Mapper 查询 project_phase 的 deliverables JSON 字段后返回的中间对象，
 * 由 Service 层解析 JSON 字符串并构建 {@link com.vibe.collaboration.vo.DocumentVO} 列表。</p>
 *
 * @author vibe
 */
@Data
public class PhaseDeliverableRow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 阶段ID */
    private Long phaseId;

    /** 阶段编码 */
    private String phaseCode;

    /** 阶段名称 */
    private String phaseName;

    /** 交付物清单 JSON 字符串 */
    private String deliverables;

    /** 阶段实际结束日期 */
    private LocalDate actualEnd;

    /** 阶段最后更新时间 */
    private LocalDateTime updateTime;
}
