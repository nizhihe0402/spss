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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            rules.add(rule);
        }

        // ── Pass 2 & 3: RECODE INTO and standalone IF ─────────────────
        rules.addAll(parseRecodeIntoRules(spssText, labels));
        rules.addAll(parseStandaloneIfAssignRules(spssText, labels));

        List<Rule> merged = mergeInitDeclarations(rules, labels);
        for (Rule r : merged) {
            r.setJavaPreview(buildJavaPreview(r));
        }
        return merged;
    }

    /**
     * 构建人类可读的 Java 伪代码预览。
     */
    static String buildJavaPreview(Rule rule) {
        StringBuilder sb = new StringBuilder();
        if (rule.getSteps() != null && !rule.getSteps().isEmpty()) {
            for (Step step : rule.getSteps()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(step.javaPreview());
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

                List<String> srcVars = extractSourceVariablesFromSteps(allSteps, rule.getTarget());
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
    // RECODE INTO rules
    // ══════════════════════════════════════════════════════════════════════

    private static List<Rule> parseRecodeIntoRules(String spssText, Map<String, String> labels) {
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
            rules.add(rule);
            consumedUntil = blockEnd;
        }
        return rules;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Standalone IF rules
    // ══════════════════════════════════════════════════════════════════════

    private static List<Rule> parseStandaloneIfAssignRules(String spssText, Map<String, String> labels) {
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

            // Skip if target already has a COMPUTE rule
            boolean alreadyHasCompute = false;
            for (Rule r : rules) {
                if (SpssUtil.normalize(r.getTarget()).equals(SpssUtil.normalize(target))) {
                    alreadyHasCompute = true;
                    break;
                }
            }
            if (alreadyHasCompute) {
                continue;
            }

            // Skip IFs inside RECODE INTO blocks
            int ifPos = m.start();
            String before = spssText.substring(Math.max(0, ifPos - 200), ifPos);
            if (before.toUpperCase(Locale.ROOT).matches("(?s).*RECODE\\s+[^.]*$")) {
                continue;
            }

            int blockEnd = findNextHardBoundary(spssText, ifPos);
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

        while (matcher.find()) {
            String token = matcher.group(0).trim().toUpperCase(Locale.ROOT);
            if (token.startsWith("DO IF")) {
                stack.add(matcher.group(1).trim());
            } else if (token.startsWith("END IF")) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
            } else if (token.startsWith("ELSE")) {
                // Check for a preceding regular IF (not DO IF)
                int elsePos = matcher.start();
                String beforeElse = prefix.substring(0, elsePos);
                Pattern ifPat = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(");
                Matcher ifM = ifPat.matcher(beforeElse);
                int lastIfParen = -1;
                while (ifM.find()) {
                    lastIfParen = ifM.end() - 1;
                }
                if (lastIfParen >= 0) {
                    int end = findBalancedParen(beforeElse, lastIfParen);
                    if (end > lastIfParen) {
                        String ifCond = beforeElse.substring(lastIfParen + 1, end).trim();
                        if (!ifCond.isEmpty()) {
                            stack.add(ifCond);
                        }
                    }
                }
                // Pop the last condition and push its negation
                if (!stack.isEmpty()) {
                    String cond = stack.remove(stack.size() - 1);
                    stack.add("NOT(" + cond + ")");
                }
            }
        }
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
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
     * Considers COMPUTE, RECODE INTO, and hard boundaries.
     */
    private static int nextRuleStart(String text, int fromIndex) {
        int nextCompute = findNextStatementStart(text, fromIndex, "COMPUTE");
        int nextRecodeInto = findNextRecodeInto(text, fromIndex);
        int nextHardBoundary = findNextHardBoundary(text, fromIndex);
        int next = minPositive(nextCompute, nextRecodeInto);
        next = minPositive(next, nextHardBoundary);
        return next < 0 ? text.length() : next;
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
    private static int findBlockEndForTarget(String text, int fromIndex, String target) {
        int hardBoundary = findNextHardBoundary(text, fromIndex);
        int nextDifferentRecodeInto = findNextDifferentRecodeInto(text, fromIndex, target, hardBoundary);
        int nextCompute = findNextStatementStart(text, fromIndex, "COMPUTE");
        int end = minPositive(nextDifferentRecodeInto, nextCompute);
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
        int end = nextCompute < 0 ? text.length() : nextCompute;
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
