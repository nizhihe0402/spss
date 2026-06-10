# SPSS Rule Engine 学生级执行结果完整替换说明

## 本包基线

基于你上传的 `pom.zip` 直接修改，不再使用之前外置的 `com.example.spss` 包。

## 已处理的问题

1. 删除 `src/main/java/com/example` 外置示例包，避免：
   - `List<AnswerRow>` 与 `List<AnswerDataRow>` 泛型冲突
   - `/api/v2/rules/execute` 重复 Controller
   - 当前 `SpsApplication` 无法扫描 `com.example.spss` 导致运行不一致

2. 基于现有主包 `com.gxaysoft.project.spsscheck` 增加学生级结果：
   - `passedList`：通过学生，含学生ID、姓名
   - `failedList`：未通过学生，含学生ID、姓名、违反校验规则、原因说明

3. 前端主页面 `index.html` 的执行结果不再展示旧的 ERROR/WARN 字段表作为主结果，改为：
   - 通过学生表
   - 未通过学生表

4. 执行入口统一支持：
   - `POST /api/v2/rules/execute`

5. Layui 上传仍使用：
   - `obj.preview(function(index, file){ ... })`
   - 未使用 `obj.getFile()`

## 新增/修改文件

### 新增

- `src/main/java/com/gxaysoft/project/spsscheck/web/RuleExecuteV2Controller.java`
- `src/main/java/com/gxaysoft/project/spsscheck/validation/StudentValidationResultBuilder.java`

### 修改

- `src/main/java/com/gxaysoft/project/spsscheck/io/PrototypeFileReaders.java`
- `src/main/java/com/gxaysoft/project/spsscheck/web/RunController.java`
- `src/main/resources/static/index.html`
- `src/main/resources/static/spss/js/rule-execute-v2.js`

### 删除

- `src/main/java/com/example/**`

## 返回结构

接口返回：

```json
{
  "code": 0,
  "msg": "校验完成，存在未通过学生",
  "data": {
    "totalRows": 100,
    "studentCount": 20,
    "passedCount": 18,
    "failedCount": 2,
    "passedList": [
      {
        "studentId": 10001,
        "studentKey": "10001",
        "studentName": "张三"
      }
    ],
    "failedList": [
      {
        "studentId": 10002,
        "studentKey": "10002",
        "studentName": "李四",
        "violationCount": 1,
        "ruleNames": "题目与表匹配检查",
        "messages": "数据行 table_id 与 bus_question.table_id 不一致",
        "violations": [
          {
            "level": "ERROR",
            "ruleCode": "QUESTION_TABLE_MISMATCH",
            "ruleName": "题目与表匹配检查",
            "message": "数据行 table_id 与 bus_question.table_id 不一致",
            "lineNo": 12,
            "questionId": 216011,
            "optionId": 21001102,
            "fieldName": "table_id"
          }
        ]
      }
    ]
  }
}
```

## 验证方式

1. 替换整个工程目录内容。
2. 执行：

```bash
mvn clean package -DskipTests
```

3. 启动：

```bash
mvn spring-boot:run
```

4. 打开主页面，进入“执行规则”。
5. 上传 CSV，执行。
6. 页面右侧应显示：
   - 学生级校验结果
   - 通过学生
   - 未通过学生

不应再出现旧的“数据字段校验：通过 / ERROR / WARN / 字段 / 行 / 问题 / 选项”作为主结果。
