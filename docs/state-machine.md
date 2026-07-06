# 状态机转换矩阵

> 文档版本：V1.0
> 更新日期：2026-07-06
> 基线文档：`系统设计文档.md` 2.2 / 2.3 / 2.5 / 2.6 / 2.7 / 2.8 节
> 配套文档：[需求总览](./requirement-overview.md) | [开发规范](./development-guide.md) | [API 变更清单](./api-change-log.md)

---

## 目录

- [总览](#总览)
- [一、项目状态机（Project）](#一项目状态机project)
- [二、设备状态机（Device）](#二设备状态机device)
- [三、转包任务状态机（OutsourceTask）](#三转包任务状态机outsourcetask)
- [四、验收任务状态机（AcceptanceTask）](#四验收任务状态机acceptancetask)
- [五、割接方案状态机（CutoverPlan）](#五割接方案状态机cutoverplan)
- [六、工作量确认状态机（WorkloadConfirm）](#六工作量确认状态机workloadconfirm)
- [附：状态机实现与校验规范](#附状态机实现与校验规范)

---

## 总览

本系统涉及六类核心状态机，覆盖项目、设备、转包任务、验收任务、割接方案、工作量确认六大业务对象。每类状态机包含：

- **状态节点表**：状态码 / 中文名 / 说明
- **流转矩阵**：from → to / 触发条件 / 操作角色
- **异常分支**：取消、超期、退回、回退等
- **ASCII 状态图**：可视化流转

### 状态机校验原则

1. 所有状态流转必须通过 Service 层显式方法调用，不允许直接 update status 字段
2. 非法流转在 Service 层抛出 `BusinessException.stateNotAllowed(...)`，返回错误码 **40901**（业务冲突 - 状态不允许）
3. 关键业务表使用 `@Version` 乐观锁，并发冲突返回错误码 **40911**（`OPTIMISTIC_LOCK_CONFLICT`，详见 [API 变更清单](./api-change-log.md)）
4. 状态变更必须通过 `SysLogService.record(...)` 记录操作人 / 前后值 / 时间（详见 [开发规范 - 日志规范](./development-guide.md#四日志规范)）

### 状态色映射（前端统一）

| 状态语义       | 颜色       | Tag 背景色   |
| ------------ | ---------- | ------------ |
| 未开始/待处理  | 灰 #8C8C8C | #F5F5F5      |
| 进行中/执行中  | 蓝 #1677FF | #E6F4FF      |
| 待审核/待确认  | 橙 #FAAD14 | #FFFBE6      |
| 已完成/已通过  | 绿 #52C41A | #F6FFED      |
| 超期/异常/驳回 | 红 #FF4D4F | #FFF2F0      |
| 暂停/挂起     | 黄 #FADB14 | #FFFBE6      |
| 已归档/已取消  | 灰 #BFBFBF | #F5F5F5      |
| 代理商代施    | 紫 #722ED1 | #F9F0FF      |

---

## 一、项目状态机（Project）

> 数据来源：`系统设计文档.md` 2.2 项目管理模块
> 实体：`com.vibe.project.entity.ProjectEntity`
> 字段：`status`（VARCHAR），可选值 `INIT/PLAN/EXECUTE/ACCEPT/CLOSE/ARCHIVED/ON_HOLD/CANCELLED`

### 1.1 状态节点表

| 状态码     | 中文名   | 说明                                          |
| ---------- | -------- | --------------------------------------------- |
| INIT       | 立项     | 项目已创建，基本信息待完善                     |
| PLAN       | 规划中   | 进行阶段规划、任务分解、里程碑设定             |
| EXECUTE    | 执行中   | 任务派发与现场作业进行中                      |
| ACCEPT     | 验收中   | 所有实施任务完成，进入验收阶段                 |
| CLOSE      | 已结项   | 终验通过，客户签核完成                        |
| ARCHIVED   | 已归档   | 复盘完成，文档归档，只读状态                   |
| ON_HOLD    | 暂停     | 任意阶段可标记暂停，阻塞所有任务派发           |
| CANCELLED  | 已取消   | 项目终止，不可恢复                            |

### 1.2 流转矩阵

| From         | To           | 触发条件                       | 操作角色     | 说明                          |
| ------------ | ------------ | ------------------------------ | ------------ | ----------------------------- |
| INIT         | PLAN         | PM 完成项目基本信息填写        | PM           | 必须关联客户与 PM             |
| PLAN         | EXECUTE      | 项目计划制定完成，开始派单      | PM           | 至少 1 个任务派发              |
| EXECUTE      | ACCEPT       | 所有实施任务完成，进入验收阶段 | PM           | 系统校验所有任务为 CONFIRMED   |
| ACCEPT       | CLOSE        | 终验通过，客户签核完成          | PM + CUSTOMER | 客户 H5 签核结果 PASS         |
| CLOSE        | ARCHIVED     | 复盘完成，文档归档             | PM / DIRECTOR | 竣工文档必须已上传            |
| 任意阶段     | ON_HOLD      | 手动暂停（含原因备注）         | PM / DIRECTOR | 暂停所有任务派发              |
| ON_HOLD      | 原状态       | 手动恢复                       | PM / DIRECTOR | 恢复后回到暂停前状态          |
| 任意阶段     | CANCELLED    | 手动取消（含原因）             | PM / DIRECTOR | 不可恢复，需归档处理          |

### 1.3 异常分支

- `ON_HOLD`（暂停）：可从任意活跃状态进入，恢复时回到原状态
- `CANCELLED`（取消）：终态，不可恢复，需归档处理
- `EXECUTE` 阶段任务超期：触发 `project_issue` 自动登记，状态本身不变，仅告警

### 1.4 ASCII 状态图

```
                  ┌──────────────────────────────────────────────────────┐
                  │                                                       │
                  ▼                                                       │
   INIT ──────▶ PLAN ──────▶ EXECUTE ──────▶ ACCEPT ──────▶ CLOSE ──────▶ ARCHIVED
                  │             │              │              │              │
                  │             │              │              │              │
                  │             │              │              │              │
                  └─────────────┴──────────────┴──────────────┘              │
                              │                                             │
                              ▼                                             │
                          ON_HOLD ◀─────────────────────────────────────────┘
                              │
                              ▼
                          CANCELLED  (终态，不可恢复)
```

---

## 二、设备状态机（Device）

> 数据来源：`系统设计文档.md` 2.3 设备资产管理模块
> 实体：`com.vibe.device.entity.DeviceInstanceEntity`
> 字段：`status`（VARCHAR）

### 2.1 状态节点表

| 状态码      | 中文名   | 说明                                  |
| ----------- | -------- | ------------------------------------- |
| IN_FACTORY  | 在库     | 原厂仓库入库登记后                    |
| SHIPPED     | 已发运   | 出库分配到项目，已发运                |
| RECEIVED    | 已到货   | 客户/仓库签收                         |
| PRE_CONFIG  | 已预配   | 预配置完成，配置文件已下发            |
| INSTALLED   | 已安装   | 现场安装上架                          |
| DEBUGGED    | 已调试   | 调试通过，连通性与配置验证通过        |
| ONLINE      | 在网运行 | 割接上线，业务流量接入                |
| REPAIR      | 返修中   | 设备故障返修                          |
| REPLACED    | 已替换   | 由备件替换                            |
| EOL         | 退网/报废 | 退网或报废，终态                      |
| DAMAGED     | 损坏     | 异常分支（任意阶段可进入）            |
| LOST        | 遗失     | 异常分支（任意阶段可进入）            |
| RETURNED    | 已退货   | 到货后客户退货                        |

### 2.2 流转矩阵

| From        | To          | 触发条件                       | 操作角色     | 说明                          |
| ----------- | ----------- | ------------------------------ | ------------ | ----------------------------- |
| IN_FACTORY  | SHIPPED     | 出库分配到项目                 | DEVICE_ADMIN | 必须关联项目 ID               |
| SHIPPED     | RECEIVED    | 客户/仓库签收                  | DEVICE_ADMIN / AGENT_ADMIN | 物流签收回调或手动确认        |
| RECEIVED    | PRE_CONFIG  | 预配置完成                     | DEVICE_ADMIN / ENGINEER | 配置模板已下发                |
| PRE_CONFIG  | INSTALLED   | 现场安装上架                   | ENGINEER     | 关联工单步骤完成               |
| INSTALLED   | DEBUGGED    | 调试通过                       | ENGINEER     | 测试记录上传                   |
| DEBUGGED    | ONLINE      | 割接上线                       | PM / ENGINEER | 关联割接方案 COMPLETED        |
| ONLINE      | REPAIR      | 设备故障返修                   | PM / DEVICE_ADMIN | 关联 problem 报告             |
| ONLINE      | REPLACED    | 由备件替换                     | PM / DEVICE_ADMIN | 触发备件领用                  |
| ONLINE      | EOL         | 退网/报废                      | PM / DEVICE_ADMIN | 终态                          |
| ONLINE      | DAMAGED     | 损坏（异常分支）               | 任意角色      | 触发 problem 上报              |
| 任意阶段    | DAMAGED     | 损坏（异常分支）               | 任意角色      | 触发 problem 上报              |
| 任意阶段    | LOST        | 遗失（异常分支）               | 任意角色      | 触发 problem 上报              |
| RECEIVED    | RETURNED    | 到货后客户退货                 | PM / DEVICE_ADMIN | 退回原厂仓库                  |

### 2.3 异常分支

- `DAMAGED`（损坏）：任意阶段可进入，必须关联 `project_issue` 记录
- `LOST`（遗失）：任意阶段可进入，必须关联 `project_issue` 记录
- `RETURNED`（退货）：仅 `RECEIVED` 状态可进入，设备退回原厂仓库
- `REPAIR`（返修）：`ONLINE` 状态进入返修流程，修复后回到 `INSTALLED` 重新调试
- `REPLACED`（已替换）：触发备件领用，原设备状态保留为 `REPLACED`，新设备进入正常流转

### 2.4 ASCII 状态图

```
IN_FACTORY ──▶ SHIPPED ──▶ RECEIVED ──▶ PRE_CONFIG ──▶ INSTALLED ──▶ DEBUGGED ──▶ ONLINE
                                  │                                                  │
                                  │                                                  ├──▶ REPAIR ──▶ (返回 INSTALLED)
                                  │                                                  ├──▶ REPLACED (终态)
                                  │                                                  └──▶ EOL (终态)
                                  │
                                  └──▶ RETURNED (退货，退回原厂仓库)

异常分支（任意阶段可进入）：
  任意状态 ──▶ DAMAGED ──▶ (返修后重新流转)
  任意状态 ──▶ LOST    (终态)
```

---

## 三、转包任务状态机（OutsourceTask）

> 数据来源：`系统设计文档.md` 2.5 代理商管理模块
> 实体：`com.vibe.agent.entity.OutsourceTaskEntity`
> 字段：`status`（VARCHAR），可选值 `PENDING/ACCEPTED/REJECTED/IN_PROGRESS/SUBMITTED/CONFIRMED/RETURNED/OVERDUE`

### 3.1 状态节点表

| 状态码       | 中文名   | 说明                                       |
| ------------ | -------- | ------------------------------------------ |
| PENDING      | 待接单   | PM 创建转包任务，等待代理商接单            |
| ACCEPTED     | 已接单   | 代理商确认接单，已指派代理商工程师          |
| REJECTED     | 已拒绝   | 代理商拒绝接单（终态，需 PM 重新指派）     |
| IN_PROGRESS  | 执行中   | 代理商工程师开始施工                       |
| SUBMITTED    | 待审核   | 代理商提交交付物，等待 PM 审核             |
| CONFIRMED    | 已确认   | PM 审核通过，任务完成（终态）              |
| RETURNED     | 已退回   | PM 审核退回，代理商补充后重新提交          |
| OVERDUE      | 已超期   | 超过截止日期未完成，触发预警升级           |

### 3.2 流转矩阵

| From        | To           | 触发条件                       | 操作角色           | 说明                          |
| ----------- | ------------ | ------------------------------ | ------------------ | ----------------------------- |
| PENDING     | ACCEPTED     | 代理商接单并指派工程师          | AGENT_ADMIN        | 必须填写代理商工程师 ID        |
| PENDING     | REJECTED     | 代理商拒绝接单（含原因）       | AGENT_ADMIN        | 终态，需 PM 重新指派代理商     |
| ACCEPTED    | IN_PROGRESS  | 代理商工程师开始施工            | AGENT_ENGINEER     | 关联工单签到                   |
| IN_PROGRESS | SUBMITTED    | 代理商提交交付物                | AGENT_ENGINEER     | 必传：施工照片、测试记录、签收单 |
| SUBMITTED   | CONFIRMED    | PM 审核通过                    | PM                 | 终态，触发代理商工作量确认流程  |
| SUBMITTED   | RETURNED     | PM 审核退回（含原因）          | PM                 | 退回原因记录在 reject_reason   |
| RETURNED    | IN_PROGRESS  | 代理商补充后重新进入执行        | AGENT_ENGINEER     | submit_count 自增              |
| IN_PROGRESS | OVERDUE      | 超过截止日期未完成              | 系统（定时任务）    | 触发预警升级至 PM 上级         |
| SUBMITTED   | OVERDUE      | 审核超期（PM 长时间未审核）    | 系统（定时任务）    | 触发 PM 告警                   |

### 3.3 异常分支

- `REJECTED`（已拒绝）：终态，需 PM 重新创建转包任务或重新指派其他代理商
- `OVERDUE`（已超期）：触发预警升级至 PM 上级，状态本身不阻塞后续流转（仍可进入 SUBMITTED）
- 交付物被退回次数 ≥ 3 次：自动触发代理商质量评分扣分

### 3.4 ASCII 状态图

```
                  ┌──────────────┐
                  │   PENDING    │ ◀── PM 创建转包任务
                  └──────┬───────┘
                         │
              ┌──────────┴──────────┐
              │                     │
       代理商接单                代理商拒绝
              │                     │
              ▼                     ▼
        ┌──────────┐          ┌──────────┐
        │ ACCEPTED │          │ REJECTED │ (终态)
        └────┬─────┘          └──────────┘
             │ 开始施工
             ▼
       ┌──────────────┐ 提交交付物 ┌────────────┐
       │ IN_PROGRESS  │ ─────────▶ │ SUBMITTED  │
       └──────┬───────┘            └─────┬──────┘
              │                          │
              │ 超期               PM 审核 │
              ▼                          ├──────────▶ CONFIRMED (终态)
        ┌──────────┐                     │
        │ OVERDUE  │                     │ PM 退回
        └──────────┘                     ▼
                                    ┌──────────┐  代理商补充
                                    │ RETURNED │ ──────────▶ IN_PROGRESS
                                    └──────────┘
```

---

## 四、验收任务状态机（AcceptanceTask）

> 数据来源：`系统设计文档.md` 2.7 验收管理模块 + 实体 `com.vibe.acceptance.entity.AcceptanceTaskEntity`
> 字段：`status`（VARCHAR），可选值 `DRAFT/APPLIED/INTERNAL_AUDITED/CUSTOMER_SIGNING/COMPLETED/REJECTED`

### 4.1 状态节点表

| 状态码             | 中文名       | 说明                                       |
| ------------------ | ------------ | ------------------------------------------ |
| DRAFT              | 草稿         | PM 编辑验收任务，未提交                     |
| APPLIED            | 已申请       | PM 提交验收申请，待内部技术审核             |
| INTERNAL_AUDITED   | 内部审核通过 | 技术主管审核通过，待发起客户签核            |
| CUSTOMER_SIGNING   | 客户签核中   | 已发送客户签核链接，等待客户在线签核        |
| COMPLETED          | 已完成       | 客户签核通过，验收完成（终态）             |
| REJECTED           | 已驳回       | 内部或客户驳回（含原因），需 PM 整改重提    |

### 4.2 流转矩阵

| From              | To                | 触发条件                       | 操作角色            | 说明                          |
| ----------------- | ----------------- | ------------------------------ | ------------------- | ----------------------------- |
| DRAFT             | APPLIED           | PM 提交验收申请                | PM                  | 必须附测试记录                |
| APPLIED           | INTERNAL_AUDITED  | 内部技术审核通过               | 技术主管            | 记录 internalAuditUserId/Time |
| APPLIED           | REJECTED          | 内部技术审核驳回（含原因）     | 技术主管            | 记录驳回原因                  |
| INTERNAL_AUDITED  | CUSTOMER_SIGNING  | 发起客户签核，生成签核链接     | PM                  | 生成 customerSignLink         |
| CUSTOMER_SIGNING  | COMPLETED         | 客户签核通过                   | CUSTOMER            | 记录 customerSignUser/Time    |
| CUSTOMER_SIGNING  | COMPLETED         | 客户有条件通过（CONDITIONAL_PASS） | CUSTOMER       | 触发遗留问题登记               |
| CUSTOMER_SIGNING  | REJECTED          | 客户签核驳回                   | CUSTOMER            | 必须填写驳回原因               |
| REJECTED          | APPLIED           | PM 整改后重新提交             | PM                  | 关联遗留问题闭环               |

### 4.3 异常分支

- `REJECTED`（已驳回）：非终态，PM 整改后可重新提交至 `APPLIED`
- 客户有条件通过（`CONDITIONAL_PASS`）：状态进入 `COMPLETED`，但触发遗留问题登记与整改跟踪
- 客户签核超期（默认 7 天）：触发 PM 告警，状态本身不变

### 4.4 代施验收特殊流程

代施项目的验收任务在 `APPLIED` 之前增加前置步骤：

```
代理商提交自测报告 → 原厂技术抽检复核 → 合格后方可进入 APPLIED
```

### 4.5 ASCII 状态图

```
   ┌────────┐  PM 提交   ┌─────────┐ 技术审核通过 ┌──────────────────┐ 发起签核 ┌──────────────────┐ 客户签核通过 ┌───────────┐
   │ DRAFT  │ ─────────▶ │ APPLIED │ ──────────▶ │ INTERNAL_AUDITED │ ───────▶ │ CUSTOMER_SIGNING │ ──────────▶ │ COMPLETED │
   └────────┘            └────┬────┘             └──────────────────┘          └────────┬─────────┘             └───────────┘
                              │                                                        │                          ▲
                              │ 驳回                                                    │ 驳回                      │
                              ▼                                                        ▼                          │
                          ┌──────────┐                                          ┌──────────┐  整改后重提          │
                          │ REJECTED │ ◀─────────────────────────────────────── │ REJECTED │ ────────────────────┘
                          └──────────┘                                          └──────────┘
```

---

## 五、割接方案状态机（CutoverPlan）

> 数据来源：`系统设计文档.md` 2.6 交付管理模块 + 实体 `com.vibe.delivery.entity.CutoverPlanEntity` + Service `CutoverPlanService`
> 字段：`status`（VARCHAR），可选值 `DRAFT/PENDING_INTERNAL_APPROVAL/INTERNAL_APPROVED/INTERNAL_REJECTED/PENDING_CUSTOMER_APPROVAL/CUSTOMER_APPROVED/CUSTOMER_REJECTED/EXECUTING/COMPLETED/ABORTED`

### 5.1 状态节点表

| 状态码                       | 中文名         | 说明                                       |
| ---------------------------- | -------------- | ------------------------------------------ |
| DRAFT                        | 草稿           | PM 编辑割接方案，未提交                    |
| PENDING_INTERNAL_APPROVAL    | 待内部审批     | PM 提交，待技术主管与总监审批              |
| INTERNAL_APPROVED            | 内部审批通过   | 内部审批通过，待发起客户审批               |
| INTERNAL_REJECTED            | 内部审批驳回   | 内部审批驳回（含原因），需 PM 修改重提     |
| PENDING_CUSTOMER_APPROVAL    | 待客户审批     | 已发送客户审批链接，等待客户在线审批       |
| CUSTOMER_APPROVED            | 客户审批通过   | 客户审批通过，可开始执行割接               |
| CUSTOMER_REJECTED            | 客户审批驳回   | 客户审批驳回（含原因），需 PM 修改重提     |
| EXECUTING                    | 执行中         | 割接执行中，按步骤逐项操作                 |
| COMPLETED                    | 已完成         | 所有步骤完成，割接成功（终态）            |
| ABORTED                      | 已中止         | 执行中触发回退或异常，割接中止             |

### 5.2 割接步骤状态机（CutoverStep）

割接方案下的每个步骤独立维护状态：`PENDING/EXECUTING/COMPLETED/ROLLED_BACK/ABORTED`

| From      | To           | 触发条件                       |
| --------- | ------------ | ------------------------------ |
| PENDING   | EXECUTING    | 开始执行该步骤                 |
| EXECUTING | COMPLETED    | 步骤完成（记录耗时）           |
| EXECUTING | ROLLED_BACK  | 执行回退方案                   |
| EXECUTING | ABORTED      | 步骤异常（记录异常信息）       |

### 5.3 流转矩阵

| From                        | To                          | 触发条件                       | 操作角色           | 说明                          |
| --------------------------- | --------------------------- | ------------------------------ | ------------------ | ----------------------------- |
| DRAFT                       | PENDING_INTERNAL_APPROVAL   | PM 提交内部审批                | PM                 | 必须含步骤与回退方案           |
| PENDING_INTERNAL_APPROVAL   | INTERNAL_APPROVED           | 内部审批通过                   | 技术主管 / DIRECTOR | 记录 approvalUserId/Time      |
| PENDING_INTERNAL_APPROVAL   | INTERNAL_REJECTED           | 内部审批驳回（含原因）         | 技术主管 / DIRECTOR | 记录驳回原因                  |
| INTERNAL_REJECTED           | DRAFT                       | PM 修改后重新编辑              | PM                 | 回到草稿状态                  |
| INTERNAL_APPROVED           | PENDING_CUSTOMER_APPROVAL   | 发起客户审批，生成签核链接     | PM                 | 生成 customerSignLink         |
| PENDING_CUSTOMER_APPROVAL   | CUSTOMER_APPROVED           | 客户审批通过                   | CUSTOMER           | 记录 customerSignUser/Time    |
| PENDING_CUSTOMER_APPROVAL   | CUSTOMER_REJECTED           | 客户审批驳回（含原因）         | CUSTOMER           | 记录驳回原因                  |
| CUSTOMER_REJECTED           | DRAFT                       | PM 修改后重新编辑              | PM                 | 回到草稿状态                  |
| CUSTOMER_APPROVED           | EXECUTING                   | 开始执行割接                   | PM / ENGINEER      | 记录 actualStartTime          |
| EXECUTING                   | COMPLETED                   | 所有步骤完成                   | PM / ENGINEER      | 记录 actualEndTime 与 summary |
| EXECUTING                   | ABORTED                     | 触发回退或异常中止             | PM / ENGINEER      | 记录 problemImprovement       |

### 5.4 异常分支

- `INTERNAL_REJECTED` / `CUSTOMER_REJECTED`：非终态，PM 修改后回到 `DRAFT` 重新提交
- `ABORTED`（已中止）：执行中触发回退方案或异常中止，需 PM 编写总结与改进
- 步骤异常（`ABORTED`）：触发自动通知 PM 与技术主管

### 5.5 ASCII 状态图

```
   ┌────────┐ 提交审批 ┌──────────────────────────┐ 通过 ┌──────────────────┐ 发起客户审批 ┌──────────────────────────┐ 客户通过 ┌──────────────────┐ 开始执行 ┌──────────┐ 全部步骤完成 ┌───────────┐
   │ DRAFT  │ ───────▶ │ PENDING_INTERNAL_APPROVAL│ ────▶│ INTERNAL_APPROVED│ ──────────▶ │ PENDING_CUSTOMER_APPROVAL│ ────────▶│ CUSTOMER_APPROVED│ ───────▶│ EXECUTING│ ────────────▶ │ COMPLETED │
   └────┬───┘          └────────────┬─────────────┘      └──────────────────┘             └────────────┬─────────────┘          └──────────────────┘          └─────┬────┘              └───────────┘
        ▲                           │ 驳回                                                                │ 驳回                                              │ 异常/回退
        │                           ▼                                                                     ▼                                                   ▼
        │                   ┌──────────────────┐                                              ┌──────────────────┐                                       ┌──────────┐
        │                   │ INTERNAL_REJECTED │                                              │ CUSTOMER_REJECTED │                                       │ ABORTED  │
        │                   └──────────────────┘                                              └──────────────────┘                                       └──────────┘
        │                          │ 修改                                                                  │ 修改
        └──────────────────────────┘                                                                      └──────────────────────────┘
```

---

## 六、工作量确认状态机（WorkloadConfirm）

> 数据来源：`系统设计文档.md` 2.5 / 2.8 节 + 实体 `com.vibe.finance.entity.FinanceWorkloadConfirmationEntity` + Service `FinanceWorkloadService`
> 字段：`approvalStatus`（VARCHAR） + `paymentStatus`（VARCHAR，独立付款状态机）

### 6.1 审批状态节点表

| 状态码           | 中文名         | 说明                                       |
| ---------------- | -------------- | ------------------------------------------ |
| DRAFT            | 草稿           | PM 创建结算单，未确认                      |
| PM_CONFIRMED     | PM 已确认      | PM 确认工作量                              |
| AGENT_CONFIRMED  | 代理商已确认   | 代理商确认工作量（进入待审批）             |
| PENDING          | 待审批         | 待财务审批                                 |
| APPROVED         | 审批通过       | 财务审批通过，进入付款流程                 |
| REJECTED         | 审批驳回       | 财务审批驳回（含原因），需 PM 修改重提     |
| CLOSED           | 已关闭         | 付款完成且对账结束（终态）                 |

### 6.2 付款状态节点表（独立状态机）

| 状态码   | 中文名   | 说明                  |
| -------- | -------- | --------------------- |
| UNPAID   | 未付款   | 审批通过后初始状态    |
| PAYING   | 付款中   | 财务发起付款          |
| PAID     | 已付款   | 付款完成              |

### 6.3 流转矩阵（审批状态）

| From              | To                | 触发条件                       | 操作角色            | 说明                          |
| ----------------- | ----------------- | ------------------------------ | ------------------- | ----------------------------- |
| DRAFT             | PM_CONFIRMED      | PM 确认工作量                   | PM                  | 记录 pmConfirmUserId/Time     |
| PM_CONFIRMED      | PENDING           | 代理商确认工作量               | AGENT_ADMIN         | 中间状态 AGENT_CONFIRMED 后自动进入 PENDING；记录 agentConfirmUserId/Time |
| PENDING           | APPROVED          | 财务审批通过                   | FINANCE             | 触发付款流程 UNPAID           |
| PENDING           | REJECTED          | 财务审批驳回（含原因）         | FINANCE             | 记录驳回原因                  |
| REJECTED          | DRAFT             | PM 修改后重新提交             | PM                  | 回到草稿状态                  |
| APPROVED + PAID   | CLOSED            | 付款完成且对账结束             | FINANCE             | 终态                          |

### 6.4 流转矩阵（付款状态）

| From    | To      | 触发条件                       | 操作角色  | 说明                          |
| ------- | ------- | ------------------------------ | --------- | ----------------------------- |
| UNPAID  | PAYING  | 财务发起付款                   | FINANCE   | 仅审批通过（APPROVED）后可触发 |
| PAYING  | PAID    | 付款完成                       | FINANCE   | 银行回单确认                   |
| PAYING  | UNPAID  | 付款失败                       | FINANCE   | 退回未付款状态                 |

### 6.5 异常分支

- `REJECTED`（审批驳回）：非终态，PM 修改后回到 `DRAFT` 重新确认
- 付款失败：`PAYING` 退回 `UNPAID`，可重新发起付款
- 双向确认机制：PM 与代理商任一拒绝，结算单回到 `DRAFT`

### 6.6 ASCII 状态图

```
审批状态：

   ┌────────┐ PM 确认 ┌──────────────┐ 代理商确认 ┌──────────┐ 财务通过 ┌──────────┐ 付款完成 ┌─────────┐
   │ DRAFT  │ ───────▶ │ PM_CONFIRMED │ ─────────▶ │ PENDING  │ ───────▶ │ APPROVED │ ───────▶ │ CLOSED  │
   └────┬───┘          └──────────────┘            └────┬─────┘          └──────────┘          └─────────┘
        ▲                                              │ 驳回
        │                                              ▼
        │                                        ┌──────────┐
        │                                        │ REJECTED │
        │                                        └────┬─────┘
        │                                             │ PM 修改
        └─────────────────────────────────────────────┘

付款状态（独立）：

   UNPAID ──▶ PAYING ──▶ PAID
       ▲          │
       │          │ 付款失败
       └──────────┘
```

---

## 附：状态机实现与校验规范

### A.1 状态码常量

每个业务模块在 `constant` 包下定义状态码常量，例如 `DeliveryConstant`：

```java
public final class DeliveryConstant {
    public static final String WORK_ORDER_STATUS_CREATED = "CREATED";
    public static final String WORK_ORDER_STATUS_CHECKED_IN = "CHECKED_IN";
    public static final String WORK_ORDER_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String WORK_ORDER_STATUS_COMPLETED = "COMPLETED";
    public static final String WORK_ORDER_STATUS_CONFIRMED = "CONFIRMED";
    // ...
}
```

业务代码中**严禁硬编码状态字符串**，必须引用常量类。

### A.2 状态校验模式

Service 层在状态流转方法中显式校验当前状态：

```java
@Transactional
public void pmConfirm(Long workOrderId, PmConfirmDTO dto) {
    WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
    if (entity == null) {
        throw BusinessException.notFound("工单不存在");
    }
    // 状态校验：仅 COMPLETED 状态可由 PM 确认
    if (!DeliveryConstant.WORK_ORDER_STATUS_COMPLETED.equals(entity.getStatus())) {
        throw BusinessException.stateNotAllowed("非 COMPLETED 状态无法确认");
    }
    entity.setStatus(DeliveryConstant.WORK_ORDER_STATUS_CONFIRMED);
    workOrderMapper.updateById(entity);
    // 操作日志
    sysLogService.record("WorkOrder", "pmConfirm", workOrderId,
        "COMPLETED", "CONFIRMED", "PM 确认工单完成");
}
```

### A.3 错误码

| 错误码 | 含义                       | 触发场景                                   |
| ------ | -------------------------- | ------------------------------------------ |
| 40901  | 状态不允许此操作           | `BusinessException.stateNotAllowed(...)`   |
| 40902  | 状态流转非法               | `canTransition/canTransitionTo` 校验未通过 |
| 40903  | 重复操作                   | 重复接单/重复提交等                        |
| 40904  | 数据重复                   | 唯一约束冲突（如 SN/项目编号重复）          |
| 40911  | 数据已被他人修改           | 乐观锁版本冲突（`@Version`），提示刷新重试  |

### A.4 操作日志记录

所有状态变更必须通过 `SysLogService.record(...)` 记录：

```java
sysLogService.record(
    /* module    */ "Project",
    /* action    */ "transition",
    /* bizId     */ projectId,
    /* oldValue  */ "EXECUTE",
    /* newValue  */ "ACCEPT",
    /* remark    */ "PM 发起验收申请"
);
```

详见 [开发规范 - 日志规范](./development-guide.md#四日志规范)。
