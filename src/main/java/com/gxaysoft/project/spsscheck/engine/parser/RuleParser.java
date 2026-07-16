package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.ComputeAction;
import com.gxaysoft.project.spsscheck.engine.model.DatasetRule;
import com.gxaysoft.project.spsscheck.engine.model.IfAssignAction;
import com.gxaysoft.project.spsscheck.engine.model.OutputRule;
import com.gxaysoft.project.spsscheck.engine.model.RecodeAction;
import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import com.gxaysoft.project.spsscheck.engine.model.StepAction;
import com.gxaysoft.project.spsscheck.model.RecodeCase;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unified SPSS rule parser that replaces v1/parser/SpssRuleParser.
 *
 * <p>Parses COMPUTE, RECODE (INTO and self-referencing), and IF assignments from SPSS
 * syntax text. Uses {@link ConditionStack} concepts for DO IF flattening — conditions
 * are resolved at parse time via {@link #findActiveDoIfCondition(String, int)} and
 * stored directly on {@link Step} objects, eliminating the need for conditional
 * wrapper steps at execution time.</p>
 *
 * <p>Key differences from v1:</p>
 * <ul>
 *   <li>Produces {@link Rule} instead of {@code SpssCheckRule}</li>
 *   <li>Steps use {@code new Step(condition, action)} — conditions are
 *       resolved at parse time and stored directly, no runtime wrapping</li>
 *   <li>Rule types are classified via {@link BlockClassifier}</li>
 *   <li>Variable extraction delegates to {@link SpssUtil}</li>
 * </ul>
 */
public final class RuleParser {

    // ── Regex patterns (same semantics as v1 SpssRuleParser) ──────────────

    private static final Pattern COMPUTE_PATTERN =
            Pattern.compile("(?i)COMPUTE\\s+([^=\\r\\n]+?)\\s*=\\s*(.+?)\\.(?=\\s|$)", Pattern.DOTALL);

    private static final Pattern LABEL_PATTERN =
            Pattern.compile("(?i)VARIABLE\\s+LABELS\\s+([^\\s]+)\\s+'([^']*)'");

    private static final Pattern RECODE_INTO_PATTERN =
            Pattern.compile("(?im)^[ \\t]*RECODE\\s+(.+?)\\s+INTO\\s+([^\\r\\n\\.]+?)\\.(?=\\s|$)");

    private static final Pattern RECODE_SELF_PATTERN =
            Pattern.compile("(?im)^[ \\t]*RECODE\\s+([^\\s\\(]+)\\s+((?:\\([^\\)]*\\)\\s*)+)\\.");

    private RuleParser() {
    }

    // ══════════════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Parse all check/computation rules from SPSS script text.
     *
     * @param spssText raw SPSS syntax
     * @return list of parsed {@link Rule} objects (never null)
     */
    public static List<Rule> parseRules(String spssText) {
        Map<String, String> labels = parseLabels(spssText);
        List<Rule> rules = new ArrayList<>();
        // 规则在原文中的锚点位置（用于 DO IF 块聚合判定规则归属）
        Map<Rule, Integer> anchors = new IdentityHashMap<>();

        // ── Pass 1: COMPUTE rules ─────────────────────────────────────
        Matcher matcher = COMPUTE_PATTERN.matcher(spssText);
        while (matcher.find()) {
            String target = matcher.group(1).trim();
            String expression = compactExpression(matcher.group(2));

            if (isConstantAssignment(expression) || isTransientDuplicateVariable(target)) {
                continue;
            }

            int blockEnd = nextRuleStart(spssText, matcher.end());
            String sourceBlock = spssText.substring(matcher.start(), blockEnd).trim();
            boolean hasSelfRecode = hasRecodeForTarget(spssText, matcher.end(), target);

            Rule rule = new Rule();
            rule.setTarget(target);
            rule.setExpression(expression);
            rule.setDescription(labels.get(SpssUtil.normalize(target)));
            rule.setSpssSource(sourceBlock);

            if (hasSelfRecode) {
                // COMPUTE step + trailing RECODE/IF steps from the same block
                rule.addStep(new Step(null, new ComputeAction(target, expression)));
                List<Step> subSteps = parseRuleSteps(sourceBlock, target);
                for (Step s : subSteps) {
                    rule.addStep(s);
                }
            } else {
                // If this COMPUTE sits inside a DO IF block, wrap it
                String doIfCond = findActiveDoIfCondition(spssText, matcher.start());
                if (doIfCond != null && !doIfCond.trim().isEmpty()) {
                    rule.addStep(new Step(doIfCond, new ComputeAction(target, expression)));
                }
                // Otherwise steps remain empty — the executor uses expression directly
            }

            List<String> sourceVars = rule.getSteps().isEmpty()
                    ? new ArrayList<>(SpssUtil.extractVariables(expression))
                    : extractSourceVariablesFromSteps(rule.getSteps(), target);
            rule.setSourceVariables(sourceVars);

            rule.setJavaPreview(rule.getSteps().isEmpty()
                    ? toJavaPreview(target, expression)
                    : toJavaPreviewFromSteps(target, rule.getSteps()));

            rule.setType(rule.getSteps().isEmpty()
                    ? BlockClassifier.classifyCompute(target, expression, sourceVars)
                    : classifyFromBlock(rule));
            anchors.put(rule, matcher.start());
            rules.add(rule);
        }

        // ── Pass 2 & 3: RECODE INTO, self-RECODE, and standalone IF ──
        rules.addAll(parseRecodeIntoRules(spssText, labels, anchors));
        rules.addAll(parseSelfRecodeRules(spssText, labels, anchors));
        rules.addAll(parseStandaloneIfAssignRules(spssText, labels, anchors));

        // ── Pass 4: DO IF 块聚合（中间变量并入汇规则）──────────────────
        List<Rule> aggregated = aggregateDoIfBlocks(spssText, rules, anchors, labels);

        // ── Pass 5: 同名目标相邻段合并（脚本作者迭代写法）──────────────
        List<Rule> sameTargetMerged = mergeSameTargetSegments(spssText, aggregated, anchors, labels);

        List<Rule> merged = mergeInitDeclarations(sameTargetMerged, labels);
        for (Rule r : merged) {
            r.setJavaPreview(buildJavaPreview(r));
            r.setExecutionChain(buildExecutionChain(r));
        }
        return merged;
    }

    /**
     * 构建完整执行链路（不截断），保存到 execution_chain 字段。
     */
    static String buildExecutionChain(Rule rule) {
        if (rule.getSteps() == null || rule.getSteps().isEmpty()) {
            if (rule.getExpression() != null && !rule.getExpression().isEmpty()) {
                return "每行 RowContext:\n  " + rule.getTarget() + " = " + rule.getExpression();
            }
            return null;
        }
        StringBuilder sb = new StringBuilder("每行 RowContext:\n");
        for (int i = 0; i < rule.getSteps().size(); i++) {
            Step step = rule.getSteps().get(i);
            sb.append("  Step ").append(i + 1).append(": ");
            if (step.getCondition() != null) {
                sb.append("IF(").append(step.getCondition()).append(") → ");
            } else {
                sb.append("无条件 → ");
            }
            if (step.getAction() instanceof ComputeAction) {
                ComputeAction ca = (ComputeAction) step.getAction();
                sb.append("COMPUTE ").append(step.getTarget()).append(" = ").append(ca.getExpression());
            } else if (step.getAction() instanceof RecodeAction) {
                RecodeAction ra = (RecodeAction) step.getAction();
                sb.append("RECODE ").append(ra.getSource()).append("(");
                if (ra.getCases() != null) {
                    for (int j = 0; j < ra.getCases().size(); j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(ra.getCases().get(j).toDisplayString());
                    }
                }
                sb.append(") → ").append(step.getTarget());
                // 只有含 ELSE 的 RECODE 才必然写入（覆盖），仅有 SYSMIS/MISSING/特定值
                // 的 RECODE 在不匹配时不写入目标变量，不算覆盖
                if (i > 0 && ra.alwaysWrites()) {
                    sb.append(" → 覆盖").append(step.getTarget());
                } else if (i == 0 && ra.alwaysWrites()) {
                    // 第一步但含 ELSE → 无条件初始化
                }
            } else if (step.getAction() instanceof IfAssignAction) {
                IfAssignAction ia = (IfAssignAction) step.getAction();
                sb.append("IF(").append(ia.getCondition()).append(") ").append(step.getTarget())
                  .append(" = ").append(ia.getValue());
            }
            sb.append("\n");
        }
        if (rule.isCheckRule()) {
            sb.append("  → ").append(rule.getTarget()).append(" 最终值 != 0 ? flag=1(未通过) : flag=0(通过)\n");
        }
        return sb.toString();
    }

    /**
     * 构建人类可读的 Java 伪代码预览（完整代码链路，不截断）。
     */
    static String buildJavaPreview(Rule rule) {
        StringBuilder sb = new StringBuilder();
        if (rule.getSteps() != null && !rule.getSteps().isEmpty()) {
            // 先统计: 无条件步骤数 + 条件步骤数
            int unconditional = 0, conditional = 0;
            for (Step s : rule.getSteps()) {
                if (s.getCondition() == null) unconditional++; else conditional++;
            }
            // 无条件步骤少 → 逐条展示；多 → 摘要
            boolean showAll = rule.getSteps().size() <= 8;
            if (showAll) {
                for (int i = 0; i < rule.getSteps().size(); i++) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(rule.getSteps().get(i).javaPreview());
                }
            } else {
                sb.append(unconditional).append(" 无条件步骤 + ")
                  .append(conditional).append(" 条件步骤，共")
                  .append(rule.getSteps().size()).append("步");
            }
        } else if (rule.getExpression() != null && !rule.getExpression().isEmpty()) {
            sb.append(rule.getTarget()).append(" = ").append(rule.getExpression());
        }
        if (rule.isCheckRule()) {
            sb.append("; ").append(rule.getTarget()).append(" = flag");
        }
        return sb.toString();
    }

    /**
     * Parse SELECT IF / SAVE OUTFILE output rules.
     */
    public static List<OutputRule> parseOutputRules(String spssText) {
        List<OutputRule> outputs = new ArrayList<>();
        Pattern selectPattern = Pattern.compile("(?is)SELECT\\s+IF\\s*\\((.*?)\\)\\s*\\.");
        Pattern savePattern = Pattern.compile("(?is)SAVE\\s+OUTFILE\\s*=\\s*'([^']+)'");
        Matcher matcher = selectPattern.matcher(spssText);
        while (matcher.find()) {
            int nextSelect = findNextSelect(spssText, matcher.end());
            int blockEnd = nextSelect < 0 ? spssText.length() : nextSelect;
            String block = spssText.substring(matcher.start(), blockEnd);
            Matcher saveMatcher = savePattern.matcher(block);
            if (!saveMatcher.find()) {
                continue;
            }
            String condition = matcher.group(1).trim().replaceAll("\\s+", " ");
            String path = saveMatcher.group(1).trim();
            String source = block.substring(0, saveMatcher.end()).trim();
            outputs.add(new OutputRule(sheetNameFromPath(path), condition, source));
        }
        return outputs;
    }

    /**
     * Parse SORT CASES / MATCH FILES dataset-level rules.
     */
    public static List<DatasetRule> parseDatasetRules(String spssText) {
        List<DatasetRule> rules = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "(?is)SORT\\s+CASES\\s+BY\\s+([^\\(\\s]+)\\(A\\)\\s*\\.\\s*"
                        + "MATCH\\s+FILES\\s+.*?/BY\\s+([^\\s\\r\\n]+)\\s+"
                        + "/FIRST\\s*=\\s*([^\\s\\r\\n]+)\\s+/LAST\\s*=\\s*([^\\.\\s\\r\\n]+)\\s*\\.");
        Matcher matcher = pattern.matcher(spssText);
        while (matcher.find()) {
            String sortBy = matcher.group(1).trim();
            String by = matcher.group(2).trim();
            String first = matcher.group(3).trim();
            String last = matcher.group(4).trim();
            rules.add(new DatasetRule(sortBy, by, first, last, matcher.group(0).trim()));
        }
        return rules;
    }

    // ══════════════════════════════════════════════════════════════════════
    // INIT declaration merging
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Merge INIT declarations (COMPUTE x = $SYSMIS) into the subsequent rule
     * for the same target. In SPSS, $SYSMIS declarations define the variable;
     * the real logic follows, often in a DO IF block.
     */
    private static List<Rule> mergeInitDeclarations(List<Rule> rules, Map<String, String> labels) {
        List<Rule> merged = new ArrayList<>();
        Rule pendingInit = null;

        for (Rule rule : rules) {
            boolean isInit = rule.getSteps().isEmpty()
                    && rule.getExpression() != null
                    && rule.getExpression().trim().equalsIgnoreCase("$SYSMIS");

            if (isInit) {
                pendingInit = rule;
                continue;
            }

            if (pendingInit != null
                    && SpssUtil.normalize(pendingInit.getTarget())
                            .equals(SpssUtil.normalize(rule.getTarget()))) {
                // Merge: INIT becomes first step, followed by subsequent rule's steps
                List<Step> allSteps = new ArrayList<>();
                allSteps.add(new Step(null, new ComputeAction(rule.getTarget(), "$SYSMIS")));
                allSteps.addAll(rule.getSteps());

                // If the second rule has no steps but a non-trivial expression, include it
                if (rule.getSteps().isEmpty()
                        && rule.getExpression() != null && !rule.getExpression().isEmpty()
                        && !isConstantAssignment(rule.getExpression())
                        && !rule.getExpression().equalsIgnoreCase("$SYSMIS")) {
                    allSteps.add(new Step(null, new ComputeAction(rule.getTarget(), rule.getExpression())));
                }

                // init($SYSMIS) 不引入源变量：沿用规则已算好的 sourceVariables
                // （聚合规则的中间变量过滤不能被重提取覆盖）
                List<String> srcVars = rule.getSourceVariables() != null
                        ? new ArrayList<>(rule.getSourceVariables())
                        : extractSourceVariablesFromSteps(allSteps, rule.getTarget());
                Rule mergedRule = new Rule();
                mergedRule.setTarget(rule.getTarget());
                mergedRule.setExpression(rule.getExpression());
                mergedRule.setDescription(labels.get(SpssUtil.normalize(rule.getTarget())));
                mergedRule.setSpssSource((pendingInit.getSpssSource() != null ? pendingInit.getSpssSource() : "")
                        + "\n" + (rule.getSpssSource() != null ? rule.getSpssSource() : ""));
                mergedRule.setSteps(allSteps);
                mergedRule.setSourceVariables(srcVars);
                mergedRule.setJavaPreview(toJavaPreviewFromSteps(rule.getTarget(), allSteps));
                mergedRule.setType(classifyFromBlock(mergedRule));
                merged.add(mergedRule);
                pendingInit = null;
            } else {
                // Different target — flush the pending INIT as standalone
                if (pendingInit != null) {
                    merged.add(pendingInit);
                    pendingInit = null;
                }
                merged.add(rule);
            }
        }
        if (pendingInit != null) {
            merged.add(pendingInit);
        }
        return merged;
    }

    // ══════════════════════════════════════════════════════════════════════
    // DO IF block aggregation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * DO IF 块聚合（设计文档：2026-07-15-doif-block-aggregation-design.md）。
     *
     * <p>对每个顶层 DO IF ... END IF 块：若块内锚定了 ≥2 个不同目标的规则、
     * 且存在被块内其他步骤消费的中间目标，则按「汇变量」重建规则 —— 每个汇
     * （不被块内消费的目标）一条规则，步骤为该汇在块内的依赖闭包（含中间变量
     * 计算、常量赋值分支），按源码顺序排列；原碎片规则被替换。</p>
     *
     * <p>无中间变量的多目标块（如血压块、教室代码块）与单目标块保持原样。</p>
     */
    private static List<Rule> aggregateDoIfBlocks(String text, List<Rule> rules,
                                                  Map<Rule, Integer> anchors,
                                                  Map<String, String> labels) {
        List<Rule> result = new ArrayList<>(rules);
        for (int[] block : scanTopLevelDoIfBlocks(text)) {
            int blockStart = block[0];
            int blockEnd = block[1];

            // 块内锚定的规则
            List<Rule> inBlock = new ArrayList<>();
            for (Rule r : result) {
                Integer pos = anchors.get(r);
                if (pos != null && pos >= blockStart && pos < blockEnd) {
                    inBlock.add(r);
                }
            }
            Set<String> anchoredTargets = new LinkedHashSet<>();
            for (Rule r : inBlock) {
                anchoredTargets.add(SpssUtil.normalize(r.getTarget()));
            }
            if (anchoredTargets.size() < 2) {
                continue;
            }

            // 重扫块内全部语句（含被 Pass 1 跳过的常量赋值 COMPUTE）
            List<PositionedStep> statements = parseBlockStatements(text, blockStart, blockEnd);
            Set<String> blockTargets = new LinkedHashSet<>();
            for (PositionedStep ps : statements) {
                blockTargets.add(SpssUtil.normalize(ps.step.getTarget()));
            }
            if (!blockTargets.containsAll(anchoredTargets)) {
                // 锚定规则的目标未被重扫覆盖（如步骤跨块的 RECODE 链）— 保守跳过
                continue;
            }

            // 消费关系：某目标出现在其他目标步骤的源变量中 → 中间变量
            Set<String> consumed = new LinkedHashSet<>();
            for (PositionedStep ps : statements) {
                String stepTarget = SpssUtil.normalize(ps.step.getTarget());
                for (String var : ps.step.sourceVariables()) {
                    String normalized = SpssUtil.normalize(var);
                    if (blockTargets.contains(normalized) && !normalized.equals(stepTarget)) {
                        consumed.add(normalized);
                    }
                }
            }
            if (consumed.isEmpty()) {
                continue; // 无中间变量 — 多汇块保持独立规则
            }
            List<String> sinks = new ArrayList<>();
            for (String target : blockTargets) {
                if (!consumed.contains(target)) {
                    sinks.add(target);
                }
            }
            if (sinks.isEmpty()) {
                continue; // 循环依赖等异常形态 — 保守跳过
            }

            // 每个汇：依赖闭包 → 重建规则
            String blockSource = text.substring(blockStart, Math.min(blockEnd, text.length())).trim();
            List<Rule> rebuilt = new ArrayList<>();
            Set<String> covered = new LinkedHashSet<>();
            for (String sink : sinks) {
                Set<String> closure = dependencyClosure(sink, statements, blockTargets);
                covered.addAll(closure);
                List<Step> steps = new ArrayList<>();
                String sinkDisplayName = null;
                for (PositionedStep ps : statements) {
                    String stepTarget = SpssUtil.normalize(ps.step.getTarget());
                    if (closure.contains(stepTarget)) {
                        steps.add(ps.step);
                    }
                    if (stepTarget.equals(sink) && sinkDisplayName == null) {
                        sinkDisplayName = ps.step.getTarget();
                    }
                }
                if (steps.isEmpty() || sinkDisplayName == null) {
                    continue;
                }
                Rule rule = new Rule();
                rule.setTarget(sinkDisplayName);
                rule.setDescription(labels.get(sink));
                rule.setSteps(steps);
                rule.setSpssSource(blockSource);
                List<String> sourceVars = new ArrayList<>();
                for (String var : extractSourceVariablesFromSteps(steps, sinkDisplayName)) {
                    if (!closure.contains(SpssUtil.normalize(var))) {
                        sourceVars.add(var);
                    }
                }
                rule.setSourceVariables(sourceVars);
                rule.setJavaPreview(toJavaPreviewFromSteps(sinkDisplayName, steps));
                rule.setType(classifyFromBlock(rule));
                anchors.put(rule, blockStart);
                rebuilt.add(rule);
            }
            if (rebuilt.isEmpty() || !covered.containsAll(anchoredTargets)) {
                // 有锚定规则未被任何汇的闭包覆盖 — 替换会丢逻辑，保守跳过
                continue;
            }

            // 原碎片规则替换为重建规则（插在第一条碎片的位置，保持 $SYSMIS init 相邻性）
            int insertAt = result.indexOf(inBlock.get(0));
            result.removeAll(inBlock);
            result.addAll(insertAt, rebuilt);
        }
        return result;
    }

    /**
     * 汇变量在块内的依赖闭包：从 sink 出发，反复吸收其步骤消费的块内目标。
     */
    private static Set<String> dependencyClosure(String sink, List<PositionedStep> statements,
                                                 Set<String> blockTargets) {
        Set<String> closure = new LinkedHashSet<>();
        closure.add(sink);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (PositionedStep ps : statements) {
                if (!closure.contains(SpssUtil.normalize(ps.step.getTarget()))) {
                    continue;
                }
                for (String var : ps.step.sourceVariables()) {
                    String normalized = SpssUtil.normalize(var);
                    if (blockTargets.contains(normalized) && closure.add(normalized)) {
                        changed = true;
                    }
                }
            }
        }
        return closure;
    }

    /**
     * 重扫块区间内的全部赋值语句（COMPUTE / RECODE INTO / RECODE self / IF 赋值），
     * 条件用全文 {@link #findActiveDoIfCondition} 解析以保留外层 DO IF 链。
     * 与 Pass 1 不同：保留常量赋值（分支逻辑需要），仍跳过跨行去重临时变量。
     */
    private static List<PositionedStep> parseBlockStatements(String text, int blockStart, int blockEnd) {
        String region = text.substring(blockStart, Math.min(blockEnd, text.length()));
        List<PositionedStep> positioned = new ArrayList<>();

        Matcher compute = COMPUTE_PATTERN.matcher(region);
        while (compute.find()) {
            String target = compute.group(1).trim();
            if (isTransientDuplicateVariable(target)) {
                continue;
            }
            String expression = compactExpression(compute.group(2));
            int pos = blockStart + compute.start();
            String condition = findActiveDoIfCondition(text, pos);
            positioned.add(new PositionedStep(pos, new Step(condition, new ComputeAction(target, expression))));
        }

        Matcher recodeInto = RECODE_INTO_PATTERN.matcher(region);
        while (recodeInto.find()) {
            String sourceAndCases = recodeInto.group(1).trim();
            String target = recodeInto.group(2).trim();
            int firstCase = sourceAndCases.indexOf('(');
            if (firstCase <= 0 || isTransientDuplicateVariable(target)) {
                continue;
            }
            String source = sourceAndCases.substring(0, firstCase).trim();
            String cases = sourceAndCases.substring(firstCase).trim();
            int pos = blockStart + recodeInto.start();
            String condition = findActiveDoIfCondition(text, pos);
            positioned.add(new PositionedStep(pos,
                    new Step(condition, new RecodeAction(source, target, parseRecodeCases(cases)))));
        }

        Matcher recodeSelf = RECODE_SELF_PATTERN.matcher(region);
        while (recodeSelf.find()) {
            String target = recodeSelf.group(1).trim();
            if (isTransientDuplicateVariable(target)) {
                continue;
            }
            int pos = blockStart + recodeSelf.start();
            String condition = findActiveDoIfCondition(text, pos);
            positioned.add(new PositionedStep(pos, new Step(condition,
                    new RecodeAction(target, target, parseRecodeCases(recodeSelf.group(2).trim())))));
        }

        Matcher ifHead = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(").matcher(region);
        while (ifHead.find()) {
            int parenStart = ifHead.end() - 1;
            int parenEnd = findBalancedParen(region, parenStart);
            if (parenEnd < 0) {
                continue;
            }
            String after = region.substring(parenEnd + 1);
            int dotIdx = after.indexOf('.');
            if (dotIdx < 0) {
                continue;
            }
            String assignment = after.substring(0, dotIdx).trim();
            int eqIdx = assignment.indexOf('=');
            if (eqIdx < 0) {
                continue;
            }
            String target = assignment.substring(0, eqIdx).trim();
            if (target.isEmpty() || isTransientDuplicateVariable(target)) {
                continue;
            }
            String ifCondition = region.substring(parenStart + 1, parenEnd).trim();
            String value = assignment.substring(eqIdx + 1).trim();
            int pos = blockStart + ifHead.start();
            String condition = findActiveDoIfCondition(text, pos);
            positioned.add(new PositionedStep(pos,
                    new Step(condition, new IfAssignAction(ifCondition, target, value))));
        }

        positioned.sort(new Comparator<PositionedStep>() {
            @Override
            public int compare(PositionedStep a, PositionedStep b) {
                return Integer.compare(a.position, b.position);
            }
        });
        return positioned;
    }

    /**
     * 定位全部顶层 DO IF ... END IF 块的字符区间 [start, end)。
     * 深度维护规则与 {@link #findActiveDoIfCondition} 一致（含非标准
     * IF ... ELSE ... END IF 块的虚拟层，保证多余 END IF 配平）。
     */
    private static List<int[]> scanTopLevelDoIfBlocks(String text) {
        List<int[]> blocks = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "(?is)\\bDO\\s+IF\\s*\\((.*?)\\)\\s*\\.|\\bELSE\\s*\\.|\\bEND\\s+IF\\s*\\.");
        Matcher matcher = pattern.matcher(text);
        List<Integer> posStack = new ArrayList<>();
        int pendingStart = -1;

        while (matcher.find()) {
            String token = matcher.group(0).trim().toUpperCase(Locale.ROOT);
            if (token.startsWith("DO IF")) {
                if (posStack.isEmpty()) {
                    pendingStart = matcher.start();
                }
                posStack.add(matcher.start());
            } else if (token.startsWith("END IF")) {
                if (!posStack.isEmpty()) {
                    posStack.remove(posStack.size() - 1);
                    if (posStack.isEmpty() && pendingStart >= 0) {
                        blocks.add(new int[]{pendingStart, matcher.end()});
                        pendingStart = -1;
                    }
                }
            } else if (token.startsWith("ELSE")) {
                int elsePos = matcher.start();
                String beforeElse = text.substring(0, elsePos);
                Matcher ifM = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(").matcher(beforeElse);
                int lastIfParen = -1;
                while (ifM.find()) {
                    lastIfParen = ifM.end() - 1;
                }
                int topPos = posStack.isEmpty() ? -1 : posStack.get(posStack.size() - 1);
                if (lastIfParen > topPos) {
                    // 非标准 IF ... ELSE ... END IF：虚拟层，由多余的 END IF 弹出
                    posStack.add(elsePos);
                }
                // 标准 DO IF ... ELSE：深度不变
            }
        }
        return blocks;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Same-target adjacent segment merging
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 同名目标相邻段处理（用户决定 2026-07-15：迭代多版不聚合）。
     * 只做一件事：init($SYSMIS) 规则按文本相邻并入紧随其后的同目标实体规则，
     * 成为其 step 0。修复 mergeInitDeclarations 依赖列表相邻、遇连续 init
     * 静默丢弃前一个的问题（表2-1 证件位数异常的 init 曾一直丢失）。
     *
     * <p>迭代写法的多版计算（如表2-1 身份证出生日期异常两个 DO IF 块）
     * 各自保留为独立规则——run 在吸收到第一个实体成员后即封口，
     * 不跨版聚合。隔着其他目标计算段的同名段同样不合并。</p>
     */
    private static List<Rule> mergeSameTargetSegments(String text, List<Rule> rules,
                                                      Map<Rule, Integer> anchors,
                                                      Map<String, String> labels) {
        // 按目标分组（保持首次出现顺序）
        Map<String, List<Rule>> byTarget = new LinkedHashMap<>();
        for (Rule r : rules) {
            if (r.getTarget() == null || anchors.get(r) == null) {
                continue;
            }
            String key = SpssUtil.normalize(r.getTarget());
            List<Rule> group = byTarget.get(key);
            if (group == null) {
                group = new ArrayList<>();
                byTarget.put(key, group);
            }
            group.add(r);
        }

        List<Rule> result = new ArrayList<>(rules);
        for (Map.Entry<String, List<Rule>> entry : byTarget.entrySet()) {
            String targetKey = entry.getKey();
            List<Rule> members = new ArrayList<>(entry.getValue());
            if (members.size() < 2) {
                continue;
            }
            members.sort(new Comparator<Rule>() {
                @Override
                public int compare(Rule a, Rule b) {
                    return Integer.compare(anchors.get(a), anchors.get(b));
                }
            });

            // 切分 run：同名相邻段按变量名归组；$SYSMIS 重初始化为分版信号——
            // 已含实体成员的 run 遇到 init 即封口（新版本另起规则）
            List<List<Rule>> runs = new ArrayList<>();
            List<Rule> current = new ArrayList<>();
            boolean currentHasReal = false;
            for (Rule member : members) {
                boolean breakRun = false;
                if (!current.isEmpty()) {
                    Rule prev = current.get(current.size() - 1);
                    if (!isAdjacentSameTarget(text, prev, member, anchors, targetKey)) {
                        breakRun = true; // 隔着其他目标计算段
                    } else if (isInitRule(member) && currentHasReal) {
                        breakRun = true; // $SYSMIS 重初始化 → 新版本
                    }
                }
                if (breakRun) {
                    runs.add(current);
                    current = new ArrayList<>();
                    currentHasReal = false;
                }
                current.add(member);
                if (!isInitRule(member)) {
                    currentHasReal = true;
                }
            }
            if (!current.isEmpty()) {
                runs.add(current);
            }

            for (List<Rule> run : runs) {
                int realCount = 0;
                for (Rule r : run) {
                    if (!isInitRule(r)) {
                        realCount++;
                    }
                }
                if (run.size() < 2 || realCount < 1) {
                    continue;
                }
                Rule mergedRule = buildSameTargetMergedRule(run, labels);
                // 重建列表避免 removeAll+add 因旧引用导致 IndexOutOfBounds
                List<Rule> rebuilt = new ArrayList<>();
                boolean inserted = false;
                for (Rule r : result) {
                    if (run.contains(r)) {
                        if (!inserted) {
                            rebuilt.add(mergedRule);
                            inserted = true;
                        }
                        // 其余 run 成员丢弃
                    } else {
                        rebuilt.add(r);
                    }
                }
                result = rebuilt;
                anchors.put(mergedRule, anchors.get(run.get(0)));
            }
        }
        return result;
    }

    /** 判断相邻性：不重叠 + 间隙内没有其他目标变量的赋值语句。 */
    private static boolean isAdjacentSameTarget(String text, Rule prev, Rule next,
                                                Map<Rule, Integer> anchors, String targetKey) {
        int prevStart = anchors.get(prev);
        int nextStart = anchors.get(next);
        int gapStart;
        if (isInitRule(prev)) {
            // init 的 spssSource 因块结束启发式会越界延伸——直接从锚点扫起
            // （init 语句本身是同目标赋值，间隙检查天然放行）
            gapStart = prevStart;
        } else {
            int prevLen = prev.getSpssSource() != null ? prev.getSpssSource().length() : 0;
            gapStart = prevStart + prevLen;
            if (gapStart > nextStart) {
                return false; // 真实重叠碎片（如 RECODE 链内嵌 IF），不处理
            }
        }
        for (PositionedStep ps : parseBlockStatements(text, gapStart, nextStart)) {
            if (!SpssUtil.normalize(ps.step.getTarget()).equals(targetKey)) {
                // RECODE / IF 赋值在间隙中不作为屏障——同一逻辑块内穿插赋值
                // 的其他变量（如血压块 Q81/Q82/BPC 的 RECODE INTO 和 IF 穿插）
                // 不应阻断归组；只有 COMPUTE 是真正隔离不同逻辑块的边界
                // （COMPUTE 声明新变量或重置变量，语义上开启新段）。
                StepAction action = ps.step.getAction();
                if (action instanceof RecodeAction || action instanceof IfAssignAction) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    /** 空步骤 + $SYSMIS 表达式的初始化规则。 */
    private static boolean isInitRule(Rule rule) {
        return rule.getSteps().isEmpty()
                && rule.getExpression() != null
                && rule.getExpression().trim().equalsIgnoreCase("$SYSMIS");
    }

    /** 将同名 run 的成员按序串接为一条规则（init 成员降级为 $SYSMIS 步骤）。 */
    private static Rule buildSameTargetMergedRule(List<Rule> run, Map<String, String> labels) {
        String displayTarget = null;
        String description = null;
        List<Step> steps = new ArrayList<>();
        StringBuilder source = new StringBuilder();
        LinkedHashMap<String, String> sourceVars = new LinkedHashMap<>();

        for (Rule member : run) {
            if (displayTarget == null && !isInitRule(member)) {
                displayTarget = member.getTarget();
            }
            if (description == null && member.getDescription() != null) {
                description = member.getDescription();
            }
            if (!member.getSteps().isEmpty()) {
                steps.addAll(member.getSteps());
            } else if (member.getExpression() != null && !member.getExpression().isEmpty()) {
                // init($SYSMIS) 或无条件 COMPUTE 降级为顺序步骤
                steps.add(new Step(null, new ComputeAction(member.getTarget(), member.getExpression())));
            }
            if (member.getSpssSource() != null && !member.getSpssSource().isEmpty()) {
                if (source.length() > 0) {
                    source.append("\n\n");
                }
                source.append(member.getSpssSource());
            }
            if (member.getSourceVariables() != null) {
                for (String var : member.getSourceVariables()) {
                    sourceVars.put(SpssUtil.normalize(var), var);
                }
            }
        }

        Rule rule = new Rule();
        rule.setTarget(displayTarget != null ? displayTarget : run.get(0).getTarget());
        rule.setDescription(description != null ? description
                : labels.get(SpssUtil.normalize(rule.getTarget())));
        rule.setSteps(steps);
        rule.setSpssSource(source.toString());
        rule.setSourceVariables(new ArrayList<>(sourceVars.values()));
        rule.setJavaPreview(toJavaPreviewFromSteps(rule.getTarget(), steps));
        rule.setType(classifyFromBlock(rule));
        return rule;
    }

    // ══════════════════════════════════════════════════════════════════════
    // RECODE INTO rules
    // ══════════════════════════════════════════════════════════════════════

    private static List<Rule> parseRecodeIntoRules(String spssText, Map<String, String> labels,
                                                   Map<Rule, Integer> anchors) {
        List<Rule> rules = new ArrayList<>();
        Matcher matcher = RECODE_INTO_PATTERN.matcher(spssText);
        int consumedUntil = -1;
        while (matcher.find()) {
            if (matcher.start() < consumedUntil) {
                continue;
            }
            String target = matcher.group(2).trim();
            String normalizedTarget = SpssUtil.normalize(target);
            int blockEnd = findBlockEndForTarget(spssText, matcher.end(), target);
            String block = spssText.substring(matcher.start(), blockEnd).trim();
            List<Step> steps = parseRuleSteps(block, target);
            if (steps.isEmpty()) {
                continue;
            }
            List<String> sourceVars = extractSourceVariablesFromSteps(steps, target);
            Rule rule = new Rule();
            rule.setTarget(target);
            rule.setDescription(labels.get(normalizedTarget));
            rule.setSteps(steps);
            rule.setSourceVariables(sourceVars);
            rule.setSpssSource(block);
            rule.setJavaPreview(toJavaPreviewFromSteps(target, steps));
            rule.setType(classifyStepBased(block, sourceVars, steps));
            anchors.put(rule, matcher.start());
            rules.add(rule);
            consumedUntil = blockEnd;
        }
        return rules;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Self-RECODE rules（RECODE var (cases). — 无 INTO）
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 解析独立的 self-RECODE（无 INTO，如 RECODE 人员配备1 (0=0) (SYSMIS=1) (ELSE=1).）。
     * 这些语句之前被 hasRecodeForTarget 挂在同名 COMPUTE 规则下；EXECUTE 边界把它们
     * 分开后（COMPUTE 和 self-RECODE 不在同一段），需要独立成规则。
     *
     * <p>跳过已被 RECODE INTO 链覆盖的段落（由 parseRecodeIntoRules 的 consumedUntil 保证）。</p>
     */
    private static List<Rule> parseSelfRecodeRules(String spssText, Map<String, String> labels,
                                                   Map<Rule, Integer> anchors) {
        List<Rule> rules = new ArrayList<>();
        Matcher matcher = RECODE_SELF_PATTERN.matcher(spssText);
        int consumedUntil = -1;
        while (matcher.find()) {
            if (matcher.start() < consumedUntil) {
                continue;
            }
            String target = matcher.group(1).trim();
            if (isTransientDuplicateVariable(target) || isConstantAssignment(target)) {
                continue;
            }
            String casesText = matcher.group(2).trim();
            List<RecodeCase> cases = parseRecodeCases(casesText);
            RecodeAction action = new RecodeAction(target, target, cases);
            Step step = applyDoIfCondition(spssText, matcher.start(), action);

            // self-RECODE 的块只收到下一个语句起始处（避免 consumedUntil 跳过紧随的
            // 下一条 self-RECODE，如 舒张压可疑 RECODE 后紧跟 压差可疑 RECODE）
            int nextStmt = findNextStatementStartRegex(spssText, matcher.end(), "(?:COMPUTE|RECODE|IF\\s*\\(|DO\\s+IF|EXECUTE)");
            int blockEnd = minPositive(findNextValidExecute(spssText, matcher.end()),
                    findNextHardBoundary(spssText, matcher.end()));
            blockEnd = minPositive(blockEnd, nextStmt);
            if (blockEnd < 0) blockEnd = spssText.length();
            String sourceBlock = spssText.substring(matcher.start(),
                    Math.min(blockEnd, spssText.length())).trim();

            List<Step> steps = new ArrayList<>();
            steps.add(step);
            List<String> sourceVars = new ArrayList<>();
            sourceVars.add(target.toUpperCase(Locale.ROOT));
            for (Step s : steps) {
                for (String v : s.sourceVariables()) {
                    String nv = v.toUpperCase(Locale.ROOT);
                    if (!sourceVars.contains(nv)) sourceVars.add(nv);
                }
            }

            Rule rule = new Rule();
            rule.setTarget(target);
            rule.setDescription(labels.get(SpssUtil.normalize(target)));
            rule.setSteps(steps);
            rule.setSourceVariables(sourceVars);
            rule.setSpssSource(sourceBlock);
            rule.setJavaPreview(toJavaPreviewFromSteps(target, steps));
            rule.setType(classifyStepBased(sourceBlock, sourceVars, steps));
            anchors.put(rule, matcher.start());
            rules.add(rule);
            consumedUntil = blockEnd;
        }
        return rules;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Standalone IF rules
    // ══════════════════════════════════════════════════════════════════════

    private static List<Rule> parseStandaloneIfAssignRules(String spssText, Map<String, String> labels,
                                                           Map<Rule, Integer> anchors) {
        List<Rule> rules = new ArrayList<>();
        Pattern ifHead = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(");
        Matcher m = ifHead.matcher(spssText);
        while (m.find()) {
            int parenStart = m.end() - 1;
            int parenEnd = findBalancedParen(spssText, parenStart);
            if (parenEnd < 0) {
                continue;
            }
            String condition = spssText.substring(parenStart + 1, parenEnd).trim();
            String after = spssText.substring(parenEnd + 1).trim();
            int eqIdx = after.indexOf('=');
            int dotIdx = after.indexOf('.');
            if (eqIdx < 0 || dotIdx < 0 || eqIdx >= dotIdx) {
                continue;
            }
            String target = after.substring(0, eqIdx).trim();
            String value = after.substring(eqIdx + 1, dotIdx).trim();
            if (target.isEmpty() || isTransientDuplicateVariable(target)) {
                continue;
            }
            // 注：同名 IF 不再跳过——每条 IF 赋值独立成规则，
            // 由 Pass 5 按变量名归组（原 alreadyHasCompute 跳过会静默丢弃
            // 表2-1 证件号码缺失 zjtype=2/3/4 分支的检查逻辑）

            // Skip IFs inside RECODE INTO blocks
            int ifPos = m.start();
            String before = spssText.substring(Math.max(0, ifPos - 200), ifPos);
            if (before.toUpperCase(Locale.ROOT).matches("(?s).*RECODE\\s+[^.]*$")) {
                continue;
            }

            // 块结束：下一个有效 EXECUTE（第一优先）/ 下一个 DO IF 或单行 IF
            // （新段开始）/ 关键字兜底
            int statementEnd = parenEnd + 1 + dotIdx + 1;
            int blockEnd = minPositive(findNextHardBoundary(spssText, ifPos),
                    findNextValidExecute(spssText, parenEnd));
            blockEnd = minPositive(blockEnd,
                    findNextStatementStartRegex(spssText, parenEnd, "DO\\s+IF"));
            Matcher nextIf = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(").matcher(spssText);
            if (nextIf.find(statementEnd)) {
                blockEnd = minPositive(blockEnd, nextIf.start());
            }
            if (blockEnd < 0) {
                blockEnd = spssText.length();
            }
            String sourceBlock = spssText.substring(ifPos, Math.min(ifPos + 500, blockEnd)).trim();

            IfAssignAction action = new IfAssignAction(condition, target, value);
            Step step = applyDoIfCondition(spssText, ifPos, action);
            List<Step> steps = new ArrayList<>();
            steps.add(step);

            List<String> sourceVars = new ArrayList<>(SpssUtil.extractVariables(condition));
            if (value != null && !value.matches("[+-]?\\d+(\\.\\d+)?")) {
                sourceVars.addAll(SpssUtil.extractVariables(value));
            }
            sourceVars.removeIf(v -> SpssUtil.normalize(v).equals(SpssUtil.normalize(target)));
            // Deduplicate preserving order
            List<String> deduped = new ArrayList<>();
            for (String v : sourceVars) {
                String n = SpssUtil.normalize(v);
                boolean found = false;
                for (String existing : deduped) {
                    if (SpssUtil.normalize(existing).equals(n)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    deduped.add(v.toUpperCase(Locale.ROOT));
                }
            }

            Rule rule = new Rule();
            rule.setTarget(target);
            rule.setDescription(labels.get(SpssUtil.normalize(target)));
            rule.setSteps(steps);
            rule.setSourceVariables(deduped);
            rule.setSpssSource(sourceBlock);
            rule.setJavaPreview("IF (" + condition + ") " + target + " = " + value);
            rule.setType(BlockClassifier.classifyConditional(deduped, sourceBlock));
            anchors.put(rule, ifPos);
            rules.add(rule);
        }
        return rules;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Step parsing — scans a block for RECODE INTO, self-RECODE, IF
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Parse all rule steps within a source block for the given target variable.
     * Steps are sorted by their textual position to preserve execution order.
     */
    static List<Step> parseRuleSteps(String block, String target) {
        List<PositionedStep> positioned = new ArrayList<>();

        // ── RECODE source (cases) INTO target ──────────────────────────
        Matcher recodeIntoMatcher = RECODE_INTO_PATTERN.matcher(block);
        while (recodeIntoMatcher.find()) {
            if (SpssUtil.normalize(target).equals(SpssUtil.normalize(recodeIntoMatcher.group(2)))) {
                String sourceAndCases = recodeIntoMatcher.group(1).trim();
                int firstCase = sourceAndCases.indexOf('(');
                if (firstCase > 0) {
                    String source = sourceAndCases.substring(0, firstCase).trim();
                    String cases = sourceAndCases.substring(firstCase).trim();
                    RecodeAction action = new RecodeAction(source, target, parseRecodeCases(cases));
                    int pos = recodeIntoMatcher.start();
                    positioned.add(new PositionedStep(pos, applyDoIfCondition(block, pos, action)));
                }
            }
        }

        // ── RECODE target (cases).  (self-referencing) ─────────────────
        Matcher recodeSelfMatcher = RECODE_SELF_PATTERN.matcher(block);
        while (recodeSelfMatcher.find()) {
            String source = recodeSelfMatcher.group(1).trim();
            if (SpssUtil.normalize(target).equals(SpssUtil.normalize(source))) {
                RecodeAction action = new RecodeAction(source, target,
                        parseRecodeCases(recodeSelfMatcher.group(2).trim()));
                int pos = recodeSelfMatcher.start();
                positioned.add(new PositionedStep(pos, applyDoIfCondition(block, pos, action)));
            }
        }

        // ── IF (condition) target = value. ─────────────────────────────
        Pattern ifHead = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(");
        Matcher ifM = ifHead.matcher(block);
        while (ifM.find()) {
            int parenStart = ifM.end() - 1;
            int parenEnd = findBalancedParen(block, parenStart);
            if (parenEnd < 0) {
                continue;
            }
            String after = block.substring(parenEnd + 1).trim();
            int dotIdx = after.indexOf('.');
            if (dotIdx < 0) {
                continue;
            }
            String assignment = after.substring(0, dotIdx).trim();
            int eqIdx = assignment.indexOf('=');
            if (eqIdx < 0) {
                continue;
            }
            String assignTarget = assignment.substring(0, eqIdx).trim();
            if (!SpssUtil.normalize(assignTarget).equals(SpssUtil.normalize(target))) {
                continue;
            }
            String condition = block.substring(parenStart + 1, parenEnd).trim();
            String value = assignment.substring(eqIdx + 1).trim();
            IfAssignAction action = new IfAssignAction(condition, target, value);
            int pos = ifM.start();
            positioned.add(new PositionedStep(pos, applyDoIfCondition(block, pos, action)));
        }

        // Sort by position to preserve textual order
        positioned.sort(new Comparator<PositionedStep>() {
            @Override
            public int compare(PositionedStep a, PositionedStep b) {
                return Integer.compare(a.position, b.position);
            }
        });

        List<Step> steps = new ArrayList<>();
        for (PositionedStep ps : positioned) {
            steps.add(ps.step);
        }
        return steps;
    }

    // ══════════════════════════════════════════════════════════════════════
    // DO IF condition handling
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Wrap a {@link StepAction} with any active DO IF condition found before
     * {@code statementPosition} in {@code block}. Returns a {@link Step} with
     * the resolved condition (null if no DO IF is active).
     */
    private static Step applyDoIfCondition(String block, int statementPosition, StepAction action) {
        String condition = findActiveDoIfCondition(block, statementPosition);
        if (condition == null || condition.trim().isEmpty()) {
            return new Step(null, action);
        }
        return new Step(condition, action);
    }

    /**
     * Find the active DO IF condition at the given position by scanning the
     * prefix text for DO IF / ELSE / END IF boundaries and maintaining a stack.
     *
     * <p>Algorithm (same as v1 SpssRuleParser):</p>
     * <ol>
     *   <li>DO IF(cond) → push cond onto stack</li>
     *   <li>ELSE → pop top, push NOT(top)</li>
     *   <li>END IF → pop top</li>
     * </ol>
     * <p>The result is the top-of-stack condition (AND-chained if nested).</p>
     */
    private static String findActiveDoIfCondition(String block, int statementPosition) {
        if (statementPosition <= 0) {
            return null;
        }
        String prefix = block.substring(0, Math.min(statementPosition, block.length()));
        Pattern pattern = Pattern.compile(
                "(?is)\\bDO\\s+IF\\s*\\((.*?)\\)\\s*\\.|\\bELSE\\s*\\.|\\bEND\\s+IF\\s*\\.");
        Matcher matcher = pattern.matcher(prefix);
        List<String> stack = new ArrayList<>();
        // 与 stack 平行：记录每个条目入栈时对应 token 在 prefix 中的起始位置，
        // 用于判断 ELSE 归属（属于标准 DO IF，还是脚本中非标准的 IF ... ELSE ... END IF 块）
        List<Integer> stackPos = new ArrayList<>();

        while (matcher.find()) {
            String token = matcher.group(0).trim().toUpperCase(Locale.ROOT);
            if (token.startsWith("DO IF")) {
                stack.add(matcher.group(1).trim());
                stackPos.add(matcher.start());
            } else if (token.startsWith("END IF")) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                    stackPos.remove(stackPos.size() - 1);
                }
            } else if (token.startsWith("ELSE")) {
                // Locate the last regular IF (not DO IF) before this ELSE
                int elsePos = matcher.start();
                String beforeElse = prefix.substring(0, elsePos);
                Pattern ifPat = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(");
                Matcher ifM = ifPat.matcher(beforeElse);
                int lastIfParen = -1;
                while (ifM.find()) {
                    lastIfParen = ifM.end() - 1;
                }
                int topPos = stackPos.isEmpty() ? -1 : stackPos.get(stackPos.size() - 1);
                if (lastIfParen > topPos) {
                    // The regular IF opened after the innermost DO IF — this ELSE closes a
                    // non-standard IF ... ELSE ... END IF block: push the negated IF condition
                    // (its later END IF pops it back off).
                    int end = findBalancedParen(beforeElse, lastIfParen);
                    if (end > lastIfParen) {
                        String ifCond = beforeElse.substring(lastIfParen + 1, end).trim();
                        if (!ifCond.isEmpty()) {
                            stack.add("NOT(" + ifCond + ")");
                            stackPos.add(elsePos);
                        }
                    }
                } else if (!stack.isEmpty()) {
                    // Standard DO IF ... ELSE: replace top with its negation
                    String cond = stack.remove(stack.size() - 1);
                    stackPos.remove(stackPos.size() - 1);
                    stack.add("NOT(" + cond + ")");
                    stackPos.add(elsePos);
                }
            }
        }
        return chainConditions(stack);
    }

    /**
     * AND 链接条件栈：单条件原样返回；多条件各自加括号后以 AND 连接，
     * 避免条件内含 OR 时的优先级错误（ConditionExpression 已支持括号布尔分组）。
     */
    private static String chainConditions(List<String> stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.size() == 1) {
            return stack.get(0);
        }
        StringBuilder sb = new StringBuilder();
        for (String cond : stack) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append('(').append(cond).append(')');
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Recode case parsing
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Parse recode case specifications like "(1=0)(SYSMIS=1)(ELSE=1)".
     */
    private static List<RecodeCase> parseRecodeCases(String casesText) {
        List<RecodeCase> cases = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\(([^=]+)=([^\\)]+)\\)").matcher(casesText);
        while (matcher.find()) {
            String from = matcher.group(1).trim();
            String to = matcher.group(2).trim();
            if ("SYSMIS".equalsIgnoreCase(from) || "MISSING".equalsIgnoreCase(from)) {
                cases.add(RecodeCase.missing(to));
            } else if ("ELSE".equalsIgnoreCase(from)) {
                cases.add(RecodeCase.elseCase(to));
            } else if (from.toLowerCase(Locale.ROOT).contains("thru")) {
                String[] parts = from.split("(?i)\\s+thru\\s+");
                if (parts.length == 2) {
                    cases.add(RecodeCase.range(parts[0].trim(), parts[1].trim(), to));
                }
            } else {
                cases.add(RecodeCase.equalsCase(from, to));
            }
        }
        return cases;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Source variable extraction
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Extract all source variables referenced by a list of steps, excluding
     * the target variable itself. Uses {@link Step#sourceVariables()} and
     * deduplicates by normalized name.
     */
    private static List<String> extractSourceVariablesFromSteps(List<Step> steps, String target) {
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        for (Step step : steps) {
            for (String variable : step.sourceVariables()) {
                if (!SpssUtil.normalize(target).equals(SpssUtil.normalize(variable))) {
                    variables.put(SpssUtil.normalize(variable), variable.toUpperCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(variables.values());
    }

    // ══════════════════════════════════════════════════════════════════════
    // Rule classification helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Classify a rule that has steps populated. Delegates to the appropriate
     * {@link BlockClassifier} method based on step content.
     */
    private static RuleType classifyFromBlock(Rule rule) {
        List<Step> steps = rule.getSteps();
        String spssSource = rule.getSpssSource();
        if (hasRecodeForTargetInSteps(steps)) {
            // Find the first recode step's source for classification
            String recodeSource = null;
            for (Step step : steps) {
                if (step.getAction() instanceof RecodeAction) {
                    recodeSource = ((RecodeAction) step.getAction()).getSource();
                    break;
                }
            }
            return BlockClassifier.classifyRecode(
                    recodeSource != null ? recodeSource : "",
                    rule.getTarget(),
                    spssSource != null ? spssSource : "");
        }
        // Has steps but no recode — likely a DO IF with compute
        if (steps.size() == 1 && steps.get(0).getAction() instanceof ComputeAction) {
            ComputeAction ca = (ComputeAction) steps.get(0).getAction();
            return BlockClassifier.classifyCompute(rule.getTarget(), ca.getExpression(),
                    rule.getSourceVariables());
        }
        return BlockClassifier.classifyConditional(rule.getSourceVariables(),
                spssSource != null ? spssSource : "");
    }

    /**
     * Classify a recode-into rule based on step content and block text.
     */
    private static RuleType classifyStepBased(String block, List<String> sourceVars, List<Step> steps) {
        if (!steps.isEmpty() && steps.get(0).getAction() instanceof RecodeAction) {
            RecodeAction ra = (RecodeAction) steps.get(0).getAction();
            return BlockClassifier.classifyRecode(ra.getSource(), ra.target(), block);
        }
        return BlockClassifier.classifyConditional(sourceVars, block);
    }

    private static boolean hasRecodeForTargetInSteps(List<Step> steps) {
        for (Step step : steps) {
            if (step.getAction() instanceof RecodeAction) {
                return true;
            }
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Java preview generation (human-readable, not executable)
    // ══════════════════════════════════════════════════════════════════════

    private static String toJavaPreview(String target, String expression) {
        return target + " = " + expression;
    }

    private static String toJavaPreviewFromSteps(String target, List<Step> steps) {
        StringBuilder sb = new StringBuilder();
        for (Step step : steps) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            StepAction action = step.getAction();
            String cond = step.getCondition();
            if (action instanceof ComputeAction) {
                ComputeAction ca = (ComputeAction) action;
                if (cond != null) {
                    sb.append("if (").append(cond).append(") ");
                }
                sb.append(ca.target()).append(" = ").append(ca.getExpression());
            } else if (action instanceof RecodeAction) {
                RecodeAction ra = (RecodeAction) action;
                if (cond != null) {
                    sb.append("if (").append(cond).append(") ");
                }
                sb.append("recode(").append(ra.getSource()).append(" -> ").append(ra.target()).append(")");
            } else if (action instanceof IfAssignAction) {
                IfAssignAction ia = (IfAssignAction) action;
                if (cond != null) {
                    sb.append("if (").append(cond).append(") ");
                }
                sb.append("if (").append(ia.getCondition()).append(") ")
                        .append(ia.target()).append(" = ").append(ia.getValue());
            }
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Boundary detection
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Find the start of the next rule after {@code fromIndex}.
     * 边界优先级（用户规格 2026-07-15）：深度 0 的 EXECUTE. 为第一优先级
     * 段边界（DO IF 内的 EXECUTE 失效）；COMPUTE/RECODE INTO/关键字列表
     * 保留为漏写 EXECUTE 时的兜底。
     */
    private static int nextRuleStart(String text, int fromIndex) {
        int nextExecute = findNextValidExecute(text, fromIndex);
        int nextCompute = findNextStatementStart(text, fromIndex, "COMPUTE");
        int nextRecodeInto = findNextRecodeInto(text, fromIndex);
        int nextHardBoundary = findNextHardBoundary(text, fromIndex);
        int next = minPositive(nextExecute, nextCompute);
        next = minPositive(next, nextRecodeInto);
        next = minPositive(next, nextHardBoundary);
        return next < 0 ? text.length() : next;
    }

    /**
     * 下一个有效 EXECUTE. 的位置：从头扫描 DO IF/ELSE/END IF/EXECUTE token
     * 维护深度（与 {@link #scanTopLevelDoIfBlocks} 同规则，含非标准
     * IF/ELSE/END IF 虚拟层），只有深度 0 的 EXECUTE 才是段边界——
     * DO IF ... END IF 内部出现的 EXECUTE 失效。
     */
    private static int findNextValidExecute(String text, int fromIndex) {
        Pattern pattern = Pattern.compile(
                "(?is)\\bDO\\s+IF\\s*\\((.*?)\\)\\s*\\.|\\bELSE\\s*\\.|\\bEND\\s+IF\\s*\\.|\\bEXECUTE\\s*\\.");
        Matcher matcher = pattern.matcher(text);
        List<Integer> posStack = new ArrayList<>();
        while (matcher.find()) {
            String token = matcher.group(0).trim().toUpperCase(Locale.ROOT);
            if (token.startsWith("DO IF")) {
                posStack.add(matcher.start());
            } else if (token.startsWith("END IF")) {
                if (!posStack.isEmpty()) {
                    posStack.remove(posStack.size() - 1);
                }
            } else if (token.startsWith("ELSE")) {
                // 与 scanTopLevelDoIfBlocks 相同的非标准 IF/ELSE/END IF 虚拟层判定
                int elsePos = matcher.start();
                String beforeElse = text.substring(0, elsePos);
                Matcher ifM = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(").matcher(beforeElse);
                int lastIfParen = -1;
                while (ifM.find()) {
                    lastIfParen = ifM.end() - 1;
                }
                int topPos = posStack.isEmpty() ? -1 : posStack.get(posStack.size() - 1);
                if (lastIfParen > topPos) {
                    posStack.add(elsePos);
                }
            } else if (token.startsWith("EXECUTE")) {
                if (posStack.isEmpty() && matcher.start() >= fromIndex) {
                    return matcher.start();
                }
            }
        }
        return -1;
    }

    private static int findNextRecodeInto(String text, int fromIndex) {
        return findNextRecodeIntoLine(text, fromIndex, null, false, -1);
    }

    private static int findNextDifferentRecodeInto(String text, int fromIndex, String target, int maxIndex) {
        return findNextRecodeIntoLine(text, fromIndex, target, true, maxIndex);
    }

    private static int findNextRecodeIntoLine(String text, int fromIndex, String target,
                                              boolean differentOnly, int maxIndex) {
        int cursor = Math.max(0, fromIndex);
        Matcher matcher = RECODE_INTO_PATTERN.matcher(text);
        while (matcher.find(cursor)) {
            if (maxIndex >= 0 && matcher.start() >= maxIndex) {
                return -1;
            }
            String matchTarget = matcher.group(2).trim();
            if (!differentOnly
                    || !SpssUtil.normalize(target).equals(SpssUtil.normalize(matchTarget))) {
                return matcher.start();
            }
            cursor = matcher.end();
        }
        return -1;
    }

    /**
     * Find the end boundary of a rule block that starts at {@code fromIndex}.
     * Searches for: the next RECODE INTO for a different target, the next COMPUTE,
     * or a hard statement boundary (SELECT IF, SAVE, etc.).
     */
    /**
     * RECODE INTO 链的块结束边界。
     * EXECUTE 参与切段：同目标 RECODE 被 EXECUTE 分开后各自成块，
     * 交由 Pass 5 按变量名重新归组（消除与 Pass 3 IF / Pass 2.5 self-RECODE
     * 的重叠 x2 重复规则）。不同目标 RECODE / COMPUTE / 关键字仍优先兜底。
     */
    private static int findBlockEndForTarget(String text, int fromIndex, String target) {
        int hardBoundary = findNextHardBoundary(text, fromIndex);
        int nextDifferentRecodeInto = findNextDifferentRecodeInto(text, fromIndex, target, hardBoundary);
        int nextCompute = findNextStatementStart(text, fromIndex, "COMPUTE");
        int nextExecute = findNextValidExecute(text, fromIndex);
        int end = minPositive(nextDifferentRecodeInto, nextCompute);
        end = minPositive(end, nextExecute);
        end = minPositive(end, hardBoundary);
        return end < 0 ? text.length() : end;
    }

    /**
     * Find the next hard statement boundary (SELECT IF, SAVE OUTFILE, DATASET, etc.)
     * after {@code fromIndex}. These statements terminate a rule block.
     */
    private static int findNextHardBoundary(String text, int fromIndex) {
        int next = -1;
        String[] statements = {
                "SELECT\\s+IF", "SAVE\\s+OUTFILE", "DATASET\\s+COPY", "DATASET\\s+ACTIVATE",
                "FILTER\\s+OFF", "USE\\s+ALL", "FREQUENCIES", "DESCRIPTIVES", "CTABLES",
                "SORT\\s+CASES", "MATCH\\s+FILES",
                "STRING\\s+", "NUMERIC\\s+",
                "VARIABLE\\s+LABELS", "VALUE\\s+LABELS",
                "VARIABLE\\s+LEVEL", "VARIABLE\\s+WIDTH",
                "FORMATS\\s+", "SPLIT\\s+FILE"
        };
        for (String statement : statements) {
            int index = findNextStatementStartRegex(text, fromIndex, statement);
            next = minPositive(next, index);
        }
        return next;
    }

    private static int findNextStatementStart(String text, int fromIndex, String statement) {
        return findNextStatementStartRegex(text, fromIndex, Pattern.quote(statement));
    }

    private static int findNextStatementStartRegex(String text, int fromIndex, String statementRegex) {
        Matcher matcher = Pattern.compile("(?im)^[ \\t]*" + statementRegex + "\\b").matcher(text);
        return matcher.find(fromIndex) ? matcher.start() : -1;
    }

    private static int findNextSelect(String text, int fromIndex) {
        Matcher matcher = Pattern.compile("(?is)SELECT\\s+IF\\s*\\(").matcher(text);
        return matcher.find(fromIndex) ? matcher.start() : -1;
    }

    /**
     * Find the closing parenthesis that balances the opening parenthesis at {@code start}.
     */
    private static int findBalancedParen(String text, int start) {
        if (start < 0 || start >= text.length() || text.charAt(start) != '(') {
            return -1;
        }
        int depth = 1;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Utility methods
    // ══════════════════════════════════════════════════════════════════════

    private static int minPositive(int left, int right) {
        if (left < 0) return right;
        if (right < 0) return left;
        return Math.min(left, right);
    }

    private static boolean hasRecodeForTarget(String text, int fromIndex, String target) {
        int nextCompute = indexOfIgnoreCase(text, "COMPUTE", fromIndex);
        int nextExecute = findNextValidExecute(text, fromIndex);
        int end = minPositive(nextCompute, nextExecute);
        if (end < 0) end = text.length();
        String block = text.substring(fromIndex, end);
        Matcher matcher = RECODE_SELF_PATTERN.matcher(block);
        while (matcher.find()) {
            if (SpssUtil.normalize(target).equals(SpssUtil.normalize(matcher.group(1)))) {
                return true;
            }
        }
        return false;
    }

    private static int indexOfIgnoreCase(String text, String needle, int fromIndex) {
        return text.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT), fromIndex);
    }

    private static boolean isConstantAssignment(String expression) {
        return expression != null && expression.matches("[+-]?\\d+(\\.\\d+)?");
    }

    private static boolean isTransientDuplicateVariable(String target) {
        String normalized = SpssUtil.normalize(target);
        return "MATCHSEQUENCE".equals(normalized)
                || "INDUPGRP".equals(normalized)
                || "PRIMARYLAST".equals(normalized);
    }

    private static String compactExpression(String expression) {
        return expression == null ? "" : expression.replaceAll("\\s+", " ").trim();
    }

    private static String sheetNameFromPath(String path) {
        String normalized = path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        if (name.toLowerCase(Locale.ROOT).endsWith(".sav")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Label parsing
    // ══════════════════════════════════════════════════════════════════════

    private static Map<String, String> parseLabels(String text) {
        Map<String, String> labels = new LinkedHashMap<>();
        Matcher matcher = LABEL_PATTERN.matcher(text);
        while (matcher.find()) {
            labels.put(SpssUtil.normalize(matcher.group(1)), matcher.group(2));
        }
        return labels;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Inner types
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Associates a {@link Step} with its textual position in the source block
     * so steps can be sorted back to execution order after pattern-matching.
     */
    private static class PositionedStep {
        final int position;
        final Step step;

        PositionedStep(int position, Step step) {
            this.position = position;
            this.step = step;
        }
    }
}
