package com.gxaysoft.project.spsscheck.v2.model;

import com.gxaysoft.project.spsscheck.v1.model.RuleStep;

import java.util.ArrayList;
import java.util.List;

/** 一条业务规则：原始SPS块 + 识别出的规则类型 + 执行元数据 */
public class RuleDefinition {
    private final String target;
    private RuleType type;
    private final String spssBlock;       // 原始 SPS 文本块
    private List<String> sourceVariables;
    private String description;           // 业务说明
    private boolean enabled = true;

    // Source script segment metadata
    private int segmentIndex;
    private int startLine;
    private int endLine;
    private String segmentTitle;
    private String splitReason;

    // For COMPUTE_INTERMEDIATE
    private String expression;

    // For CHECK types
    private String checkCondition;
    private String flagExpression;        // how to compute the 0/1 flag

    // For OUTPUT_GROUP
    private String outputName;
    private String selectCondition;

    // For execution: parsed RuleSteps
    private List<RuleStep> steps;

    public RuleDefinition(String target, RuleType type, String spssBlock, List<String> sourceVariables) {
        this.target = target;
        this.type = type;
        this.spssBlock = spssBlock;
        this.sourceVariables = sourceVariables != null ? new ArrayList<>(sourceVariables) : new ArrayList<>();
    }

    public String getTarget() { return target; }
    public RuleType getType() { return type; }
    public void setType(RuleType t) { this.type = t; }
    public String getSpssBlock() { return spssBlock; }
    public List<String> getSourceVariables() { return sourceVariables; }
    public void setSourceVariables(List<String> v) { this.sourceVariables = v; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; }

    public int getSegmentIndex() { return segmentIndex; }
    public void setSegmentIndex(int segmentIndex) { this.segmentIndex = segmentIndex; }
    public int getStartLine() { return startLine; }
    public void setStartLine(int startLine) { this.startLine = startLine; }
    public int getEndLine() { return endLine; }
    public void setEndLine(int endLine) { this.endLine = endLine; }
    public int getLineCount() { return endLine >= startLine ? endLine - startLine + 1 : 0; }
    public String getSegmentTitle() { return segmentTitle; }
    public void setSegmentTitle(String segmentTitle) { this.segmentTitle = segmentTitle; }
    public String getSplitReason() { return splitReason; }
    public void setSplitReason(String splitReason) { this.splitReason = splitReason; }

    public void applySegment(SpssSegment segment) {
        if (segment == null) return;
        this.segmentIndex = segment.getIndex();
        this.startLine = segment.getStartLine();
        this.endLine = segment.getEndLine();
        this.segmentTitle = segment.getTitle();
        this.splitReason = segment.getSplitReason();
        if ((this.description == null || this.description.isEmpty()) && segment.getTitle() != null) {
            this.description = segment.getTitle();
        }
    }

    public String getExpression() { return expression; }
    public void setExpression(String e) { this.expression = e; }
    public String getCheckCondition() { return checkCondition; }
    public void setCheckCondition(String c) { this.checkCondition = c; }
    public String getFlagExpression() { return flagExpression; }
    public void setFlagExpression(String f) { this.flagExpression = f; }
    public String getOutputName() { return outputName; }
    public void setOutputName(String n) { this.outputName = n; }
    public String getSelectCondition() { return selectCondition; }
    public void setSelectCondition(String c) { this.selectCondition = c; }
    public List<RuleStep> getSteps() { return steps; }
    public void setSteps(List<RuleStep> s) { this.steps = s; }

    public String getJavaPreview() {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case COMPUTE_INTERMEDIATE:
                sb.append("// ").append(description != null ? description : "计算 " + target).append("\n");
                sb.append("BigDecimal ").append(escapeJavaIdent(target))
                  .append(" = new ArithmeticExpression(\"")
                  .append(expression != null ? expression.replace("\"", "\\\"") : "")
                  .append("\", row).parse();");
                if (sourceVariables != null && !sourceVariables.isEmpty()) {
                    sb.append(" // 依赖: ").append(String.join(", ", sourceVariables));
                }
                break;
            case IDENTITY_CHECK:
            case MISSING_CHECK:
            case CONSISTENCY_CHECK:
            case RANGE_CHECK:
            case DOCUMENT_CHECK:
            case OUTCOME_DETERMINATION:
                sb.append("// ").append(description != null ? description : type.label + ": " + target).append("\n");
                if (expression != null && !expression.isEmpty()) {
                    sb.append("BigDecimal ").append(escapeJavaIdent(target))
                      .append(" = new ArithmeticExpression(\"")
                      .append(expression.replace("\"", "\\\""))
                      .append("\", row).parse();\n");
                    sb.append("int flag_").append(escapeJavaIdent(target))
                      .append(" = (").append(escapeJavaIdent(target))
                      .append(" != null && ").append(escapeJavaIdent(target))
                      .append(".compareTo(BigDecimal.ZERO) != 0) ? 1 : 0;\n");
                    sb.append("row.put(\"").append(target)
                      .append("\", flag_").append(escapeJavaIdent(target)).append(");\n");
                    sb.append("row.putFlag(\"").append(target)
                      .append("\", flag_").append(escapeJavaIdent(target)).append(");");
                } else if (checkCondition != null && !checkCondition.isEmpty()) {
                    sb.append("boolean check_").append(escapeJavaIdent(target))
                      .append(" = new ConditionExpression(\"")
                      .append(checkCondition.replace("\"", "\\\""))
                      .append("\", row).eval();\n");
                    sb.append("row.put(\"").append(target)
                      .append("\", check_").append(escapeJavaIdent(target)).append(" ? 1 : 0);");
                } else {
                    String s = spssBlock.replaceAll("\\s+", " ").trim();
                    sb.append("// 条件块: ").append(s.substring(0, Math.min(80, s.length())));
                }
                break;
            case CONDITIONAL_BLOCK:
                sb.append("// 条件判断块: ").append(description != null ? description : target).append("\n");
                String compacted = spssBlock.replaceAll("\\s+", " ").trim();
                sb.append("// SPSS: ").append(compacted.substring(0, Math.min(100, compacted.length()))).append("\n");
                if (expression != null && !expression.isEmpty()) {
                    sb.append("// 表达式: ").append(expression);
                }
                break;
            case DUPLICATE_MARK:
                sb.append("// 去重标记: 按 ").append(target).append(" 分组\n");
                sb.append("rows.sort(Comparator.comparing(r -> r.getDecimal(\"").append(target).append("\")));\n");
                sb.append("Map<String,List<RowContext>> groups = new LinkedHashMap<>();\n");
                sb.append("for (RowContext row : rows) {\n");
                sb.append("    String key = String.valueOf(row.get(\"").append(target).append("\"));\n");
                sb.append("    groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);\n");
                sb.append("}\n");
                sb.append("for (List<RowContext> g : groups.values()) {\n");
                sb.append("    for (int i = 0; i < g.size(); i++) {\n");
                sb.append("        g.get(i).put(\"PrimaryFirst1\", i == 0 ? 1 : 0);\n");
                sb.append("        g.get(i).put(\"PrimaryLast\", i == g.size() - 1 ? 1 : 0);\n");
                sb.append("    }\n");
                sb.append("}");
                break;
            case OUTPUT_GROUP:
                sb.append("// 输出分组: ").append(outputName != null ? outputName : target).append("\n");
                sb.append("if (new ConditionExpression(\"")
                  .append(selectCondition != null ? selectCondition.replace("\"", "\\\"") : "")
                  .append("\", row).eval()) {\n");
                sb.append("    // → ").append(outputName).append("\n");
                sb.append("}");
                break;
            default:
                sb.append("// ").append(type.label).append(": ").append(target).append("\n");
                if (steps != null && !steps.isEmpty()) {
                    for (com.gxaysoft.project.spsscheck.v1.model.RuleStep step : steps) {
                        sb.append("//   step: ").append(step.javaRule()).append("\n");
                    }
                }
                break;
        }
        return sb.toString();
    }

    private static String escapeJavaIdent(String s) {
        if (s == null || s.isEmpty()) return "var";
        return s.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("_+", "_");
    }
}
