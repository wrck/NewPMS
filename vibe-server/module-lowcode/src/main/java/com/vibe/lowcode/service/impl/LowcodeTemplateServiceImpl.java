package com.vibe.lowcode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.base.PageQuery;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.lowcode.constant.LowcodeConstant;
import com.vibe.lowcode.dto.LowcodeTemplateDTO;
import com.vibe.lowcode.entity.LowcodeTemplateEntity;
import com.vibe.lowcode.mapper.LowcodeTemplateMapper;
import com.vibe.lowcode.service.LowcodeTemplateService;
import com.vibe.lowcode.util.JsonSchemaValidator;
import com.vibe.lowcode.vo.LowcodeTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码模板 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class LowcodeTemplateServiceImpl implements LowcodeTemplateService {

    private final LowcodeTemplateMapper templateMapper;

    @Override
    public PageResult<LowcodeTemplateVO> page(PageQuery query, String keyword) {
        LambdaQueryWrapper<LowcodeTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(LowcodeTemplateEntity::getTemplateCode, keyword)
                    .or().like(LowcodeTemplateEntity::getTemplateName, keyword));
        }
        wrapper.orderByDesc(LowcodeTemplateEntity::getCreateTime);

        Page<LowcodeTemplateEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<LowcodeTemplateEntity> result = templateMapper.selectPage(page, wrapper);

        List<LowcodeTemplateVO> records = new ArrayList<>();
        for (LowcodeTemplateEntity e : result.getRecords()) {
            LowcodeTemplateVO vo = new LowcodeTemplateVO();
            BeanUtils.copyProperties(e, vo);
            records.add(vo);
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public LowcodeTemplateVO getById(Long id) {
        LowcodeTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("低代码模板");
        }
        LowcodeTemplateVO vo = new LowcodeTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(LowcodeTemplateDTO dto) {
        // 校验 templateCode 唯一
        Long count = templateMapper.selectCount(
                new LambdaQueryWrapper<LowcodeTemplateEntity>()
                        .eq(LowcodeTemplateEntity::getTemplateCode, dto.getTemplateCode()));
        if (count > 0) {
            throw BusinessException.conflict("模板编码已存在：" + dto.getTemplateCode());
        }
        // 校验 JSON Schema Draft 7
        validateSchema(dto.getSchemaJson());
        // 校验模板类型
        validateTemplateType(dto.getTemplateType());

        LowcodeTemplateEntity entity = new LowcodeTemplateEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setUsageCount(0);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        templateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, LowcodeTemplateDTO dto) {
        LowcodeTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("低代码模板");
        }
        // 校验 templateCode 唯一（排除自身）
        Long count = templateMapper.selectCount(
                new LambdaQueryWrapper<LowcodeTemplateEntity>()
                        .eq(LowcodeTemplateEntity::getTemplateCode, dto.getTemplateCode())
                        .ne(LowcodeTemplateEntity::getId, id));
        if (count > 0) {
            throw BusinessException.conflict("模板编码已存在：" + dto.getTemplateCode());
        }
        validateSchema(dto.getSchemaJson());
        validateTemplateType(dto.getTemplateType());

        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        templateMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LowcodeTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("低代码模板");
        }
        templateMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        LowcodeTemplateEntity source = templateMapper.selectById(id);
        if (source == null) {
            throw BusinessException.notFound("低代码模板");
        }
        LowcodeTemplateEntity copy = new LowcodeTemplateEntity();
        BeanUtils.copyProperties(source, copy);
        copy.setId(null);
        copy.setTemplateCode(source.getTemplateCode() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setTemplateName(source.getTemplateName() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setUsageCount(0);
        copy.setCreatorId(UserContextHolder.getUserId());
        templateMapper.insert(copy);
        return copy.getId();
    }

    @Override
    public String exportJson(Long id) {
        LowcodeTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("低代码模板");
        }
        return entity.getSchemaJson();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importJson(LowcodeTemplateDTO dto) {
        // 校验 JSON Schema Draft 7
        validateSchema(dto.getSchemaJson());
        validateTemplateType(dto.getTemplateType());
        // 校验 templateCode 唯一
        Long count = templateMapper.selectCount(
                new LambdaQueryWrapper<LowcodeTemplateEntity>()
                        .eq(LowcodeTemplateEntity::getTemplateCode, dto.getTemplateCode()));
        if (count > 0) {
            throw BusinessException.conflict("模板编码已存在：" + dto.getTemplateCode());
        }

        LowcodeTemplateEntity entity = new LowcodeTemplateEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setUsageCount(0);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        templateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long instantiateFromTemplate(Long templateId, String configName) {
        // 模板本身不支持实例化（由各配置 Service 调用）
        // 此方法仅校验模板存在，返回模板 ID
        LowcodeTemplateEntity template = templateMapper.selectById(templateId);
        if (template == null) {
            throw BusinessException.notFound("低代码模板");
        }
        if (LowcodeConstant.STATUS_DISABLED.equals(template.getStatus())) {
            throw BusinessException.stateNotAllowed("模板已禁用，无法实例化");
        }
        return templateId;
    }

    /** 校验 JSON Schema Draft 7 合法性 */
    private void validateSchema(String schemaJson) {
        if (!JsonSchemaValidator.isValid(schemaJson)) {
            List<String> errors = JsonSchemaValidator.validate(schemaJson);
            String detail = errors.isEmpty() ? "JSON Schema 格式不合法" : String.join("; ", errors);
            throw BusinessException.of(40000, "JSON Schema Draft 7 校验失败：" + detail);
        }
    }

    /** 校验模板类型合法性 */
    private void validateTemplateType(String templateType) {
        if (!LowcodeConstant.TEMPLATE_TYPE_FORM.equals(templateType)
                && !LowcodeConstant.TEMPLATE_TYPE_LIST.equals(templateType)
                && !LowcodeConstant.TEMPLATE_TYPE_TAB.equals(templateType)
                && !LowcodeConstant.TEMPLATE_TYPE_RELATION.equals(templateType)) {
            throw BusinessException.of(40000, "模板类型必须为 FORM/LIST/TAB/RELATION");
        }
    }
}
