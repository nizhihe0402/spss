# SPSS Rule Engine 执行结果拆分完整替换包

## 包内容

本包包含后端完整类和前端完整页面/脚本：

```text
src/main/java/com/example/spss/common/ApiResponse.java
src/main/java/com/example/spss/controller/RuleExecuteV2Controller.java
src/main/java/com/example/spss/validator/AnswerDataLoadService.java
src/main/java/com/example/spss/validator/AnswerDataValidator.java
src/main/java/com/example/spss/validator/AnswerDataRow.java
src/main/java/com/example/spss/validator/AnswerMetadataRepository.java
src/main/java/com/example/spss/validator/RuleViolationCode.java
src/main/java/com/example/spss/validator/dto/*.java
src/main/java/com/example/spss/validator/meta/*.java
src/main/resources/static/spss/js/rule-execute-v2.js
src/main/resources/templates/rule-execute-v2.html
```

## 直接替换方式

在项目根目录执行：

```bash
# 1. 备份当前代码
git status
git checkout -b feature/result-split-full

# 2. 解压本包，把 src 目录覆盖到项目根目录
# Windows 可以直接复制 src 目录到项目根目录

# 3. 编译
mvn clean package -DskipTests
```

## 接口

执行规则接口：

```text
POST /api/v2/rules/execute
Content-Type: multipart/form-data
```

参数：

```text
csvFile / file   CSV 文件，二选一
tableId          当前表ID，可选，但建议传
projectId        当前项目ID，可选，但建议传
year             年份，可选，但建议传
scriptId         脚本ID，可选
fieldCheck       字段校验标记，可选
```

## 返回结构

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "totalRows": 2957,
    "studentCount": 31,
    "passedCount": 20,
    "failedCount": 11,
    "passedList": [
      {
        "studentId": 10001,
        "studentName": "张三"
      }
    ],
    "failedList": [
      {
        "studentId": 10002,
        "studentName": "李四",
        "violations": [
          {
            "ruleCode": "REQUIRED_MISSING",
            "ruleName": "必填项缺失",
            "message": "必填题缺失：question_id=214003",
            "lineNo": 56,
            "fieldName": "content",
            "questionId": 214003
          }
        ]
      }
    ]
  }
}
```

## 已修复点

1. `AnswerDataLoadService` 已提供：

```java
loadFromCsv(MultipartFile file, Long tableId, Long projectId, String year)
```

2. 前端上传已使用：

```js
obj.preview(function(index, file) { ... })
```

不再使用：

```js
obj.getFile()
```

3. 页面执行结果分为：

- 通过学生
- 未通过学生

不再把旧的 ERROR/WARN 字段表作为主结果。

## 如果你项目已有统一返回 AjaxResult

可以把 `RuleExecuteV2Controller` 中：

```java
return ApiResponse.success(validateResult);
```

替换为你项目已有的：

```java
return AjaxResult.success(validateResult);
```

其他类不用改。

## 如果出现 Controller 路径冲突

如果你项目已有同路径：

```text
/api/v2/rules/execute
```

保留你现有 Controller，复制本包中的：

- AnswerDataLoadService
- AnswerDataValidator
- DTO
- Repository

然后把现有 Controller 的执行方法改为：

```java
List rows = answerDataLoadService.loadFromCsv(file, tableId, projectId, year);
ValidateResult validateResult = answerDataValidator.validate(rows, tableId, projectId, year);
return AjaxResult.success(validateResult);
```
