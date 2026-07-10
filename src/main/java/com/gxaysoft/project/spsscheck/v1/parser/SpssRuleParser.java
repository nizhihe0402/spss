package com.gxaysoft.project.spsscheck.v1.parser;

import com.gxaysoft.project.spsscheck.v1.model.*;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.model.RecodeCase;
import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpssRuleParser {
    private static final Logger log = LoggerFactory.getLogger(SpssRuleParser.class);

    private static final Pattern COMPUTE_PATTERN = Pattern.compile("(?i)COMPUTE\\s+([^=\\r\\n]+?)\\s*=\\s*(.+?)\\.(?=\\s|$)", Pattern.DOTALL);
    private static final Pattern LABEL_PATTERN = Pattern.compile("(?i)VARIABLE\\s+LABELS\\s+([^\\s]+)\\s+'([^']*)'");
    private static final Pattern RECODE_INTO_PATTERN = Pattern.compile("(?im)^[ \\t]*RECODE\\s+(.+?)\\s+INTO\\s+([^\\r\\n\\.]+?)\\.(?=\\s|$)");
    private static final Pattern RECODE_SELF_PATTERN = Pattern.compile("(?im)^[ \\t]*RECODE\\s+([^\\s\\(]+)\\s+((?:\\([^\\)]*\\)\\s*)+)\\.");
    private static final Set<String> KEYWORDS = new LinkedHashSet<>(Arrays.asList(
            "COMPUTE", "EXECUTE", "RECODE", "SYSMIS", "ELSE", "THRU", "INTO",
            "VARIABLE", "VALUE", "LABELS", "IF", "DO", "END", "AND", "OR",
            "DATEDIFF", "DATEDIF", "CHAR", "MAX", "MIN", "SUM", "MEAN",
            "DAYS", "MONTHS", "YEARS", "HOURS", "MINUTES", "SECONDS",
            "NUMBER", "STRING", "MISSING", "RND", "MOD", "NOT",
            "XDATE", "LTRIM", "RTRIM", "LENGTH", "SUBSTR",
            "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10",
            "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10",
            "LOWEST", "HIGHEST", "THRU", "COPY",
            "YEAR", "MONTH", "MDAY", "SUBSTR", "ORDINAL", "SCALE", "LAYERED",
            "ANALYSIS", "FORMAT", "LEAVE", "SPLIT", "FILE", "OFF",
            "NOTABLE", "STATISTICS", "STDDEV", "MINIMUM", "MAXIMUM", "MEDIAN",
            "COMPRESSED", "OUTFILE", "DROP", "FIRST", "LAST", "PRIMARY", "DUPLICATE",
            "DISPLAY", "LABEL", "CATEGORIES", "KEY", "VALUE", "EMPTY", "EXCLUDE", "INCLUDE",
            "SELECT", "SAVE", "CASES", "MATCH", "ALL", "KEEP", "MAP", "RENAME", "MAKE",
            "TABLE", "VLABELS", "VARIABLES", "COUNT", "COLUMN", "ROW", "TOTAL", "POSITION",
            "TITLE", "SUBTITLE", "FOOTNOTE", "TEMPORARY", "FILTER", "DATASET", "NAME", "WINDOW",
            "ACTIVATE", "CLOSE", "SORT", "ASCENDING", "DESCENDING", "TO", "WITH"
    ));

    private SpssRuleParser() {
    }

    public static List<SpssCheckRule> parseRules(String spssText) {
        Map<String, String> labels = parseLabels(spssText);
        List<SpssCheckRule> rules = new ArrayList<>();
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
            List<RuleStep> steps = new ArrayList<>();
            if (hasSelfRecode) {
                steps.add(new ComputeRuleStep(target, expression));
                steps.addAll(parseRuleSteps(sourceBlock, target));
            } else {
                // If this COMPUTE is inside a DO IF block, wrap it with the condition
                String doIfCond = findActiveDoIfCondition(spssText, matcher.start());
                if (doIfCond != null && !doIfCond.trim().isEmpty()) {
                    steps.add(new ConditionalRuleStep(doIfCond, new ComputeRuleStep(target, expression)));
                }
            }
            List<String> sourceVariables = steps.isEmpty()
                    ? extractVariables(expression)
                    : extractSourceVariablesFromSteps(steps, target);

            rules.add(new SpssCheckRule(target, expression, labels.get(SpssUtil.normalize(target)),
                    sourceVariables, hasSelfRecode, sourceBlock,
                    steps.isEmpty() ? toJavaRule(target, expression, false) : toJavaRuleFromSteps(target, steps),
                    steps));
        }
        rules.addAll(parseRecodeIntoRules(spssText, labels));
        rules.addAll(parseStandaloneIfAssignRules(spssText, labels));
        log.info("解析完成: rules={}", rules.size());
        return mergeInitDeclarations(rules, labels);
    }

    /**
     * Merge INIT declarations (COMPUTE x = $SYSMIS) into the subsequent rule for the same target.
     * In SPSS, $SYSMIS declarations define the variable; the real logic follows in a DO IF block.
     */
    private static List<SpssCheckRule> mergeInitDeclarations(List<SpssCheckRule> rules, Map<String, String> labels) {
        List<SpssCheckRule> merged = new ArrayList<>();
        SpssCheckRule pendingInit = null;

        for (SpssCheckRule rule : rules) {
            boolean isInit = rule.getSteps().isEmpty()
                    && rule.getExpression().trim().equalsIgnoreCase("$SYSMIS");

            if (isInit) {
                pendingInit = rule;
                continue; // hold it, don't add yet
            }

            if (pendingInit != null && SpssUtil.normalize(pendingInit.getTarget())
                    .equals(SpssUtil.normalize(rule.getTarget()))) {
                // Merge: init becomes first step
                List<RuleStep> allSteps = new ArrayList<>();
                allSteps.add(new ComputeRuleStep(rule.getTarget(), "$SYSMIS"));
                allSteps.addAll(rule.getSteps());
                // If the second rule has no steps but a non-trivial expression, add it
                if (rule.getSteps().isEmpty() && !rule.getExpression().isEmpty()
                        && !isConstantAssignment(rule.getExpression())
                        && !rule.getExpression().equalsIgnoreCase("$SYSMIS")) {
                    allSteps.add(new ComputeRuleStep(rule.getTarget(), rule.getExpression()));
                }
                List<String> srcVars = extractSourceVariablesFromSteps(allSteps, rule.getTarget());
                boolean isCheck = rule.isCheckRule() || hasRecodeForTargetInSteps(allSteps);
                merged.add(new SpssCheckRule(rule.getTarget(), rule.getExpression(),
                        labels.get(SpssUtil.normalize(rule.getTarget())), srcVars, isCheck,
                        pendingInit.getSpssSource() + "\n" + rule.getSpssSource(),
                        toJavaRuleFromSteps(rule.getTarget(), allSteps), allSteps));
                pendingInit = null;
            } else {
                // Different target — flush the pending init as standalone
                if (pendingInit != null) {
                    merged.add(pendingInit);
                    pendingInit = null;
                }
                merged.add(rule);
            }
        }
        if (pendingInit != null) merged.add(pendingInit);
        return merged;
    }

    private static boolean hasRecodeForTargetInSteps(List<RuleStep> steps) {
        for (RuleStep step : steps) {
            if (step instanceof RecodeRuleStep) return true;
            if (step instanceof ConditionalRuleStep) {
                try {
                    java.lang.reflect.Field f = ConditionalRuleStep.class.getDeclaredField("delegate");
                    f.setAccessible(true);
                    RuleStep d = (RuleStep) f.get(step);
                    if (d instanceof RecodeRuleStep) return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private static List<SpssCheckRule> parseStandaloneIfAssignRules(String spssText, Map<String, String> labels) {
        List<SpssCheckRule> rules = new ArrayList<>();
        // IF (condition) target = value.  — scan manually for balanced parentheses
        Pattern ifHead = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(");
        Matcher m = ifHead.matcher(spssText);
        while (m.find()) {
            int parenStart = m.end() - 1; // position of '('
            int parenEnd = findBalancedParen(spssText, parenStart);
            if (parenEnd < 0) continue;
            String condition = spssText.substring(parenStart + 1, parenEnd).trim();
            String after = spssText.substring(parenEnd + 1).trim();
            // after should be "target = value."
            int eqIdx = after.indexOf('=');
            int dotIdx = after.indexOf('.');
            if (eqIdx < 0 || dotIdx < 0 || eqIdx >= dotIdx) continue;
            String target = after.substring(0, eqIdx).trim();
            String value = after.substring(eqIdx + 1, dotIdx).trim();
            if (target.isEmpty()) continue;

            // Skip if value is constant and target is a transient/system variable
            if (isTransientDuplicateVariable(target)) continue;

            // Check if this target already has a COMPUTE rule — if so, this IF is a step, not standalone
            boolean alreadyHasCompute = false;
            for (SpssCheckRule r : rules) {
                if (SpssUtil.normalize(r.getTarget()).equals(SpssUtil.normalize(target))) {
                    alreadyHasCompute = true;
                    break;
                }
            }
            if (alreadyHasCompute) continue;

            // Skip IFs that are inside RECODE INTO blocks (they appear within rule source blocks)
            // A simple heuristic: if the IF appears between RECODE and ".", it's part of a recode
            int ifPos = m.start();
            String before = spssText.substring(Math.max(0, ifPos - 200), ifPos);
            if (before.toUpperCase(Locale.ROOT).matches("(?s).*RECODE\\s+[^.]*$")) continue;

            // Build source block: from this IF to next statement boundary
            int blockEnd = findNextHardBoundary(spssText, ifPos);
            if (blockEnd < 0) blockEnd = spssText.length();
            String sourceBlock = spssText.substring(ifPos, Math.min(ifPos + 500, blockEnd)).trim();

            List<RuleStep> steps = new ArrayList<>();
            steps.add(applyDoIfCondition(spssText, ifPos,
                    new IfAssignRuleStep(condition, target, value)));

            List<String> sourceVariables = new ArrayList<>();
            sourceVariables.addAll(SpssRuleParser.extractVariables(condition));
            // If value is not a constant, extract variables from it too
            if (!value.matches("[+-]?\\d+(\\.\\d+)?")) {
                sourceVariables.addAll(SpssRuleParser.extractVariables(value));
            }
            // Remove target from its own source vars
            sourceVariables.removeIf(v -> SpssUtil.normalize(v).equals(SpssUtil.normalize(target)));

            String javaRule = "if evalCondition(\"" + escapeForJavaRule(condition) + "\") then " + target + " = " + value;

            rules.add(new SpssCheckRule(target, "", labels.get(SpssUtil.normalize(target)),
                    sourceVariables, false, sourceBlock, javaRule, steps));
        }
        return rules;
    }

    public static List<SpssOutputRule> parseOutputRules(String spssText) {
        List<SpssOutputRule> outputs = new ArrayList<>();
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
            outputs.add(new SpssOutputRule(sheetNameFromPath(path), condition, source));
        }
        return outputs;
    }

    public static List<SpssDatasetRule> parseDatasetRules(String spssText) {
        List<SpssDatasetRule> rules = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?is)SORT\\s+CASES\\s+BY\\s+([^\\(\\s]+)\\(A\\)\\s*\\.\\s*MATCH\\s+FILES\\s+.*?/BY\\s+([^\\s\\r\\n]+)\\s+/FIRST\\s*=\\s*([^\\s\\r\\n]+)\\s+/LAST\\s*=\\s*([^\\.\\s\\r\\n]+)\\s*\\.");
        Matcher matcher = pattern.matcher(spssText);
        while (matcher.find()) {
            String sortBy = matcher.group(1).trim();
            String by = matcher.group(2).trim();
            String first = matcher.group(3).trim();
            String last = matcher.group(4).trim();
            rules.add(new SpssDatasetRule(sortBy, by, first, last, matcher.group(0).trim()));
        }
        return rules;
    }

    public static List<String> extractVariables(String expression) {
        String cleaned = stripCommentsAndStringLiterals(expression);
        // Match SPSS identifiers: optional # prefix for scratch variables, then letters/digits/underscores/Chinese
        Matcher matcher = Pattern.compile("#?[\\p{L}_][\\p{L}\\p{N}_]*").matcher(cleaned);
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        while (matcher.find()) {
            String variable = matcher.group();
            String normalized = SpssUtil.normalize(variable);
            if (isLikelyVariable(variable, normalized)) {
                variables.put(normalized, variable.toUpperCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(variables.values());
    }

    /**
     * Remove SPSS human comments and string literals before variable extraction.
     * Otherwise Chinese descriptions, value labels and file names are easily misread as source variables.
     */
    private static String stripCommentsAndStringLiterals(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder noComments = new StringBuilder();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String line : lines) {
            String trimmed = line.replace("\ufeff", "").trim();
            if (trimmed.startsWith("*")) continue;
            if (noComments.length() > 0) noComments.append('\n');
            noComments.append(line);
        }

        StringBuilder out = new StringBuilder(noComments.length());
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < noComments.length(); i++) {
            char c = noComments.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                out.append(' ');
            } else if (c == '\"' && !inSingle) {
                inDouble = !inDouble;
                out.append(' ');
            } else if (inSingle || inDouble) {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean isLikelyVariable(String variable, String normalized) {
        if (variable == null || variable.trim().isEmpty()) return false;
        if (normalized == null || normalized.isEmpty()) return false;
        if (KEYWORDS.contains(normalized)) return false;
        if (variable.startsWith("$")) return false;
        if (variable.length() > 64) return false;
        if (variable.indexOf('\\') >= 0 || variable.indexOf('/') >= 0 || variable.indexOf(':') >= 0) return false;
        // Function names frequently appear before '(' and should not become variables.
        if (Arrays.asList("TRUNC", "ABS", "SQRT", "EXP", "LN", "LG10", "ANY", "RANGE", "CHAR",
                "CONCAT", "INDEX", "DATE", "TIME", "VALUE", "LAG", "VALUELABEL").contains(normalized)) {
            return false;
        }
        return true;
    }

    private static int findNextSelect(String text, int fromIndex) {
        Matcher matcher = Pattern.compile("(?is)SELECT\\s+IF\\s*\\(").matcher(text);
        return matcher.find(fromIndex) ? matcher.start() : -1;
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

    private static List<SpssCheckRule> parseRecodeIntoRules(String spssText, Map<String, String> labels) {
        List<SpssCheckRule> rules = new ArrayList<>();
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
            List<RuleStep> steps = parseRuleSteps(block, target);
            if (steps.isEmpty()) {
                continue;
            }
            List<String> sourceVariables = extractSourceVariablesFromSteps(steps, target);
            boolean isCheck = isZeroOneRecodeCheck(steps);
            rules.add(new SpssCheckRule(target, "", labels.get(normalizedTarget), sourceVariables, isCheck,
                    block, toJavaRuleFromSteps(target, steps, isCheck), steps));
            consumedUntil = blockEnd;
        }
        return rules;
    }

    private static boolean isZeroOneRecodeCheck(List<RuleStep> steps) {
        boolean sawRecode = false;
        for (RuleStep step : steps) {
            RecodeRuleStep recode = unwrapRecodeStep(step);
            if (recode == null) {
                continue;
            }
            sawRecode = true;
            for (RecodeCase recodeCase : recode.getCases()) {
                if (recodeCase.isCopyResult() || !recodeCase.isZeroOneResult()) {
                    return false;
                }
            }
        }
        return sawRecode;
    }

    private static RecodeRuleStep unwrapRecodeStep(RuleStep step) {
        if (step instanceof RecodeRuleStep) {
            return (RecodeRuleStep) step;
        }
        if (step instanceof ConditionalRuleStep) {
            try {
                java.lang.reflect.Field f = ConditionalRuleStep.class.getDeclaredField("delegate");
                f.setAccessible(true);
                RuleStep delegate = (RuleStep) f.get(step);
                return unwrapRecodeStep(delegate);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static int findBlockEndForTarget(String text, int fromIndex, String target) {
        int hardBoundary = findNextHardBoundary(text, fromIndex);
        int nextDifferentRecodeInto = findNextDifferentRecodeInto(text, fromIndex, target, hardBoundary);
        int nextCompute = findNextStatementStart(text, fromIndex, "COMPUTE");
        int end = minPositive(nextDifferentRecodeInto, nextCompute);
        end = minPositive(end, hardBoundary);
        return end < 0 ? text.length() : end;
    }

    static List<RuleStep> parseRuleSteps(String block, String target) {
        List<PositionedRuleStep> positioned = new ArrayList<>();

        Matcher recodeIntoMatcher = RECODE_INTO_PATTERN.matcher(block);
        while (recodeIntoMatcher.find()) {
            if (SpssUtil.normalize(target).equals(SpssUtil.normalize(recodeIntoMatcher.group(2)))) {
                String sourceAndCases = recodeIntoMatcher.group(1).trim();
                int firstCase = sourceAndCases.indexOf('(');
                if (firstCase > 0) {
                    String source = sourceAndCases.substring(0, firstCase).trim();
                    String cases = sourceAndCases.substring(firstCase).trim();
                    int position = recodeIntoMatcher.start();
                    RuleStep step = new RecodeRuleStep(source, target, parseRecodeCases(cases));
                    positioned.add(new PositionedRuleStep(position, applyDoIfCondition(block, position, step)));
                }
            }
        }

        Matcher recodeSelfMatcher = RECODE_SELF_PATTERN.matcher(block);
        while (recodeSelfMatcher.find()) {
            String source = recodeSelfMatcher.group(1).trim();
            if (SpssUtil.normalize(target).equals(SpssUtil.normalize(source))) {
                int position = recodeSelfMatcher.start();
                RuleStep step = new RecodeRuleStep(source, target, parseRecodeCases(recodeSelfMatcher.group(2).trim()));
                positioned.add(new PositionedRuleStep(position, applyDoIfCondition(block, position, step)));
            }
        }

        // IF (condition) target = value. — handle nested parens in condition
        Pattern ifHead = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(");
        Matcher ifM = ifHead.matcher(block);
        while (ifM.find()) {
            int parenStart = ifM.end() - 1;
            int parenEnd = findBalancedParen(block, parenStart);
            if (parenEnd < 0) continue;
            String after = block.substring(parenEnd + 1).trim();
            int dotIdx = after.indexOf('.');
            if (dotIdx < 0) continue;
            String assignment = after.substring(0, dotIdx).trim();
            // Check that this assignment is for our target
            int eqIdx = assignment.indexOf('=');
            if (eqIdx < 0) continue;
            String assignTarget = assignment.substring(0, eqIdx).trim();
            if (!SpssUtil.normalize(assignTarget).equals(SpssUtil.normalize(target))) continue;
            String condition = block.substring(parenStart + 1, parenEnd).trim();
            String value = assignment.substring(eqIdx + 1).trim();
            int position = ifM.start();
            RuleStep step = new IfAssignRuleStep(condition, target, value);
            positioned.add(new PositionedRuleStep(position, applyDoIfCondition(block, position, step)));
        }

        positioned.sort(new java.util.Comparator<PositionedRuleStep>() {
            @Override
            public int compare(PositionedRuleStep left, PositionedRuleStep right) {
                return left.position - right.position;
            }
        });

        List<RuleStep> steps = new ArrayList<>();
        for (PositionedRuleStep step : positioned) {
            steps.add(step.step);
        }
        return steps;
    }

    private static RuleStep applyDoIfCondition(String block, int statementPosition, RuleStep step) {
        String condition = findActiveDoIfCondition(block, statementPosition);
        if (condition == null || condition.trim().isEmpty()) {
            return step;
        }
        return new ConditionalRuleStep(condition, step);
    }

    private static String findActiveDoIfCondition(String block, int statementPosition) {
        if (statementPosition <= 0) {
            return null;
        }
        String prefix = block.substring(0, Math.min(statementPosition, block.length()));
        Pattern pattern = Pattern.compile("(?is)\\bDO\\s+IF\\s*\\((.*?)\\)\\s*\\.|\\bELSE\\s*\\.|\\bEND\\s+IF\\s*\\.");
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
                // Check for regular IF preceding this ELSE (IF without DO IF)
                int elsePos = matcher.start();
                String beforeElse = prefix.substring(0, elsePos);
                Pattern ifPat = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(");
                Matcher ifM = ifPat.matcher(beforeElse);
                int lastIfParen = -1;
                while (ifM.find()) lastIfParen = ifM.end() - 1;
                if (lastIfParen >= 0) {
                    int end = findBalancedParen(beforeElse, lastIfParen);
                    if (end > lastIfParen) {
                        String ifCond = beforeElse.substring(lastIfParen + 1, end).trim();
                        if (!ifCond.isEmpty()) stack.add(ifCond);
                    }
                }
                if (!stack.isEmpty()) {
                    String cond = stack.remove(stack.size() - 1);
                    stack.add("NOT(" + cond + ")");
                }
            }
        }
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

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

    private static List<String> extractSourceVariablesFromSteps(List<RuleStep> steps, String target) {
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        for (RuleStep step : steps) {
            for (String variable : step.sourceVariables()) {
                if (!SpssUtil.normalize(target).equals(SpssUtil.normalize(variable))) {
                    variables.put(SpssUtil.normalize(variable), variable.toUpperCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(variables.values());
    }

    private static String toJavaRuleFromSteps(String target, List<RuleStep> steps) {
        return toJavaRuleFromSteps(target, steps, true);
    }

    private static String toJavaRuleFromSteps(String target, List<RuleStep> steps, boolean checkRule) {
        StringBuilder builder = new StringBuilder();
        for (RuleStep step : steps) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(step.javaRule());
        }
        if (checkRule) {
            builder.append("; ").append(target).append(" = missing(").append(target).append(") || ")
                    .append(target).append(" != 0 ? 1 : 0");
        }
        return builder.toString();
    }

    private static String toJavaRule(String target, String expression, boolean checkRule) {
        String javaExpression = "evalDecimal(\"" + escapeForJavaRule(expression) + "\")";
        if (checkRule) {
            return target + " = " + javaExpression + "; " + target + " = missing(" + target + ") || " + target + " != 0 ? 1 : 0";
        }
        return target + " = " + javaExpression;
    }

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

    private static int findNextRecodeIntoLine(String text, int fromIndex, String target, boolean differentOnly, int maxIndex) {
        int cursor = Math.max(0, fromIndex);
        Matcher matcher = RECODE_INTO_PATTERN.matcher(text);
        while (matcher.find(cursor)) {
            if (maxIndex >= 0 && matcher.start() >= maxIndex) {
                return -1;
            }
            String matchTarget = matcher.group(2).trim();
            if (!differentOnly || !SpssUtil.normalize(target).equals(SpssUtil.normalize(matchTarget))) {
                return matcher.start();
            }
            cursor = matcher.end();
        }
        return -1;
    }

    private static int findNextHardBoundary(String text, int fromIndex) {
        int next = -1;
        String[] statements = new String[]{
                "SELECT\\s+IF", "SAVE\\s+OUTFILE", "DATASET\\s+COPY", "DATASET\\s+ACTIVATE",
                "FILTER\\s+OFF", "USE\\s+ALL", "FREQUENCIES", "DESCRIPTIVES", "CTABLES",
                "SORT\\s+CASES", "MATCH\\s+FILES",
                "STRING\\s+", "NUMERIC\\s+",
                "VARIABLE\\s+LABELS", "VALUE\\s+LABELS",
                "VARIABLE\\s+LEVEL", "VARIABLE\\s+WIDTH",
                "FORMATS\\s+", "LEAVE\\s+", "SPLIT\\s+FILE"
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

    private static int findBalancedParen(String text, int start) {
        if (start < 0 || start >= text.length() || text.charAt(start) != '(') return -1;
        int depth = 1;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private static int minPositive(int left, int right) {
        if (left < 0) {
            return right;
        }
        if (right < 0) {
            return left;
        }
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
        return expression.matches("[+-]?\\d+(\\.\\d+)?");
    }

    private static boolean isTransientDuplicateVariable(String target) {
        String normalized = SpssUtil.normalize(target);
        return "MATCHSEQUENCE".equals(normalized)
                || "INDUPGRP".equals(normalized)
                || "PRIMARYLAST".equals(normalized);
    }

    private static String escapeForJavaRule(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String compactExpression(String expression) {
        return expression.replaceAll("\\s+", " ").trim();
    }

    private static Map<String, String> parseLabels(String text) {
        Map<String, String> labels = new LinkedHashMap<>();
        Matcher matcher = LABEL_PATTERN.matcher(text);
        while (matcher.find()) {
            labels.put(SpssUtil.normalize(matcher.group(1)), matcher.group(2));
        }
        return labels;
    }
}
