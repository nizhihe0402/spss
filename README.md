# SPSS Rule Engine 执行结果分组替换包

## 替换目标

把执行规则页面从旧的“字段校验 ERROR/WARN 表格”改为：

1. 通过学生：列出学生ID、姓名
2. 未通过学生：列出学生ID、姓名、违反的校验规则、说明

## 必须替换的文件

### 后端

- `SpssRuleExecuteController.java`
- `AnswerDataValidator.java`
- `AnswerDataLoadService.java`
- `dto/*.java`

注意：包名当前是 `com.example.spss`，复制到项目后需要改成你项目实际包名。

### 前端

- `src/main/resources/static/spss/execute-rule.html`

关键点：

- API 固定走 `/api/v2/rules/execute`
- 成功回调固定调用 `renderSplitResult(res.data)`
- 不再调用旧的 `renderFieldCheckResult`
- Layui 上传使用 `obj.preview(function(index,file){...})`

## 返回 JSON

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "passedList": [],
    "failedList": [],
    "passedCount": 0,
    "failedCount": 0,
    "totalCount": 0
  }
}
```
