package com.gxaysoft.project.spsscheck.v2.parser;

import com.gxaysoft.project.spsscheck.v2.model.RuleDefinition;
import com.gxaysoft.project.spsscheck.v2.model.RuleType;
import com.gxaysoft.project.spsscheck.v2.model.SpssSegment;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;

import java.util.*;
import java.util.regex.*;

/**
 * Splits SPS text into complete logical rule blocks and classifies each block by RuleType.
 *
 * Important:
 * - Do not use command keywords as separators and discard them.
 * - Split only on complete SPSS command/control-block boundaries.
 * - Keep line numbers so the UI can show startLine/endLine for manual review.
 */
public final class BlockParser {

    private static final int MAX_SEGMENT_LINES = 120;

    private static final Pattern COMPUTE_PAT = Pattern.compile("(?i)COMPUTE\\s+([^=\\r\\n]+?)\\s*=\\s*(.+?)\\.", Pattern.DOTALL);
    private static final Pattern RECODE_PAT = Pattern.compile("(?ims)^[ \\t]*RECODE.+?\\.", Pattern.DOTALL);
    private static final Pattern DO_IF_PAT  = Pattern.compile("(?is)DO\\s+IF\\s*\\(.+\\)\\s*\\.");
    private static final Pattern IF_PAT     = Pattern.compile("(?im)^[ \\t]*IF\\s*\\(.+\\)\\s+\\S+\\s*=\\s*[^\\.]+\\.");
    private static final Pattern SELECT_PAT = Pattern.compile("(?is)SELECT\\s+IF\\s*\\(.+");
    private static final Pattern SORT_PAT   = Pattern.compile("(?is)SORT\\s+CASES\\s+BY.+");

    // Known intermediate variable patterns (English names used as building blocks)
    private static final Set<String> KNOWN_INTERMEDIATE = new HashSet<>(Arrays.asList(
            "ID3", "AGE2", "AGE", "BMI", "BPC", "MATCHSEQUENCE", "INDUPGRP", "SFZ_DATE"
    ));
    // Document-related source variables (case-insensitive)
    private static final Pattern DOC_SOURCE_PAT = Pattern.compile(
            "(?i)\\b(zjtype|zjcard|sfz|card|zjcode|idcard|id_card|证件类型|证件号码)\\b");

    public static List<RuleDefinition> parse(String spsText) {
        List<SpssSegment> segments = splitIntoSegments(spsText);
        List<RuleDefinition> rules = new ArrayList<>();

        String pendingHumanTitle = null;
        int pendingHumanStartLine = 0;
        for (SpssSegment segment : segments) {
            RuleDefinition rule = classifyBlock(segment.getText());
            if (rule == null) {
                // Keep the latest human comment title through setup commands such as DATASET/FILTER/USE,
                // so SELECT IF / SAVE OUTFILE blocks can inherit meaningful titles like “拷贝ID不一致的数据”.
                if (segment.getTitle() != null && !isCommandLikeTitle(segment.getTitle())) {
                    pendingHumanTitle = segment.getTitle();
                    pendingHumanStartLine = segment.getStartLine();
                }
                continue;
            }
            rule.applySegment(segment);
            if (pendingHumanTitle != null && isCommandLikeTitle(rule.getDescription())) {
                rule.setDescription(pendingHumanTitle);
                rule.setSegmentTitle(pendingHumanTitle);
                if (pendingHumanStartLine > 0 && (rule.getStartLine() <= 0 || pendingHumanStartLine < rule.getStartLine())) {
                    rule.setStartLine(pendingHumanStartLine);
                }
            }
            pendingHumanTitle = null;
            pendingHumanStartLine = 0;
            rules.add(rule);
        }

        rules = mergeSameTarget(rules);

        // Do not reclassify a rule merely because its target is referenced later.
        // In these audit scripts, check flags are intentionally referenced by SELECT IF / output groups.
        // Reclassifying them as COMPUTE_INTERMEDIATE would hide real validation rules.
        mergeV1Execution(spsText, rules);
        sanitizeRuleSources(rules);
        return rules;
    }

    /** Backward-compatible API: returns segment text only. */
    static List<String> splitIntoBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        for (SpssSegment segment : splitIntoSegments(text)) {
            if (segment.getText() != null && !segment.getText().trim().isEmpty()) {
                blocks.add(segment.getText().trim());
            }
        }
        return blocks;
    }

    /** Public for UI/export: full line-aware segmentation. */
    public static List<SpssSegment> splitIntoSegments(String text) {
        List<Statement> statements = scanStatements(text);
        List<SpssSegment> result = new ArrayList<>();
        List<Statement> pendingComments = new ArrayList<>();

        int i = 0;
        while (i < statements.size()) {
            Statement st = statements.get(i);
            if (st.comment) {
                pendingComments.add(st);
                i++;
                continue;
            }
            if (st.blank) {
                i++;
                continue;
            }

            int start = i;
            int end = i;
            String cmd = commandWord(st.text);

            if ("SELECT".equals(cmd)) {
                end = collectSelectSave(statements, i);
            } else if ("SORT".equals(cmd)) {
                end = collectSortMatch(statements, i);
            } else if ("DO".equals(cmd) && startsWith(st.text, "DO IF")) {
                end = collectDoIfBlock(statements, i);
            } else if ("COMPUTE".equals(cmd)) {
                end = collectComputeRule(statements, i);
            } else if ("RECODE".equals(cmd)) {
                end = collectRecodeRule(statements, i);
            } else if ("IF".equals(cmd)) {
                end = collectIfAssignRule(statements, i);
            } else {
                // One complete SPSS command; declarations/statistical output commands may be ignored later by classifyBlock.
                end = i;
            }

            List<Statement> included = new ArrayList<>();
            included.addAll(pendingComments);
            included.addAll(statements.subList(start, end + 1));
            pendingComments.clear();

            // If a generated segment is too long, split only at safe statement/control boundaries.
            appendPossiblySplit(result, included, "完整命令/控制块切分");
            i = end + 1;
        }

        // Preserve trailing comments as one segment for manual review, although classifyBlock will ignore it.
        if (!pendingComments.isEmpty()) {
            appendPossiblySplit(result, pendingComments, "文件结束");
        }
        return result;
    }

    private static List<Statement> scanStatements(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        List<Statement> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int startLine = -1;

        for (int idx = 0; idx < lines.length; idx++) {
            int lineNo = idx + 1;
            String line = lines[idx];
            String trimmed = stripBom(line).trim();

            if (trimmed.isEmpty() && current.length() == 0) {
                statements.add(new Statement(lineNo, lineNo, line, false, true));
                continue;
            }

            // In these SPS audit scripts, lines beginning with * are used as human comments/headings.
            // Treat the whole line as a complete comment even if it does not end with a period.
            if (isCommentLine(line) && current.length() == 0) {
                statements.add(new Statement(lineNo, lineNo, line, true, false));
                continue;
            }

            if (current.length() == 0) {
                startLine = lineNo;
            } else {
                current.append('\n');
            }
            current.append(line);

            if (lineHasTerminalPeriod(line)) {
                statements.add(new Statement(startLine, lineNo, current.toString(), false, false));
                current.setLength(0);
                startLine = -1;
            }
        }

        if (current.length() > 0) {
            statements.add(new Statement(startLine < 0 ? lines.length : startLine, lines.length, current.toString(), false, false));
        }
        return statements;
    }

    private static void appendPossiblySplit(List<SpssSegment> out, List<Statement> statements, String reason) {
        if (statements == null || statements.isEmpty()) return;
        int cursor = 0;
        while (cursor < statements.size()) {
            int end = cursor;
            int startLine = statements.get(cursor).startLine;
            while (end + 1 < statements.size()
                    && statements.get(end + 1).endLine - startLine + 1 <= MAX_SEGMENT_LINES) {
                end++;
            }
            // If a single statement is longer than MAX_SEGMENT_LINES, keep it intact.
            List<Statement> part = statements.subList(cursor, end + 1);
            out.add(toSegment(out.size() + 1, part, reason));
            cursor = end + 1;
        }
    }

    private static SpssSegment toSegment(int index, List<Statement> statements, String reason) {
        int startLine = statements.get(0).startLine;
        int endLine = statements.get(statements.size() - 1).endLine;
        StringBuilder text = new StringBuilder();
        for (Statement s : statements) {
            if (s.blank) continue;
            if (text.length() > 0) text.append('\n');
            text.append(s.text);
        }
        String title = findTitle(statements);
        if (title == null || title.isEmpty()) {
            title = firstExecutablePreview(statements);
        }
        return new SpssSegment(index, startLine, endLine, title, reason, text.toString().trim());
    }

    private static String findTitle(List<Statement> statements) {
        for (Statement s : statements) {
            if (s.comment && isHeadingComment(s.text)) return cleanTitle(s.text);
        }
        for (Statement s : statements) {
            if (s.comment) {
                String title = cleanTitle(s.text);
                if (title != null && !title.isEmpty() && !title.toUpperCase(Locale.ROOT).contains("ENCODING:")) {
                    return title;
                }
            }
        }
        return null;
    }

    private static String firstExecutablePreview(List<Statement> statements) {
        for (Statement s : statements) {
            if (!s.comment && !s.blank && s.text != null && !s.text.trim().isEmpty()) {
                String oneLine = s.text.replaceAll("\\s+", " ").trim();
                return oneLine.substring(0, Math.min(120, oneLine.length()));
            }
        }
        return "空白/注释段";
    }

    private static int collectSelectSave(List<Statement> statements, int i) {
        int end = i;
        for (int j = i + 1; j < statements.size(); j++) {
            Statement s = statements.get(j);
            if (s.comment || s.blank) continue;
            String cmd = commandWord(s.text);
            end = j;
            if ("SAVE".equals(cmd)) break;
            if (isMajorStart(s.text) && !"EXECUTE".equals(cmd) && !"SAVE".equals(cmd)) {
                end = j - 1;
                break;
            }
        }
        return includeTrailingExecute(statements, end);
    }

    private static int collectSortMatch(List<Statement> statements, int i) {
        int end = i;
        for (int j = i + 1; j < statements.size(); j++) {
            Statement s = statements.get(j);
            if (s.comment || s.blank) continue;
            String cmd = commandWord(s.text);
            end = j;
            if ("MATCH".equals(cmd)) break;
            if (isMajorStart(s.text) && !"MATCH".equals(cmd)) {
                end = j - 1;
                break;
            }
        }
        return includeTrailingExecute(statements, end);
    }

    private static int collectDoIfBlock(List<Statement> statements, int i) {
        int depth = 0;
        int end = i;
        for (int j = i; j < statements.size(); j++) {
            Statement s = statements.get(j);
            if (s.comment || s.blank) continue;
            if (startsWith(s.text, "DO IF")) depth++;
            if (startsWith(s.text, "END IF")) depth = Math.max(0, depth - 1);
            end = j;
            if (depth == 0 && j > i) break;
        }
        return includeTrailingExecute(statements, end);
    }

    private static int collectComputeRule(List<Statement> statements, int i) {
        String target = extractComputeTarget(statements.get(i).text);
        int end = i;
        for (int j = i + 1; j < statements.size(); j++) {
            Statement s = statements.get(j);
            if (s.comment || s.blank) continue;
            String cmd = commandWord(s.text);
            if ("EXECUTE".equals(cmd)) { end = j; break; }
            if ("IF".equals(cmd) && sameTarget(target, extractIfAssignTarget(s.text))) { end = j; continue; }
            if ("RECODE".equals(cmd) && sameTarget(target, extractRecodeTarget(s.text))) { end = j; continue; }
            if (isLabelForTarget(s.text, target)) { end = j; continue; }
            break;
        }
        return end;
    }

    private static int collectRecodeRule(List<Statement> statements, int i) {
        String target = extractRecodeTarget(statements.get(i).text);
        int end = i;
        for (int j = i + 1; j < statements.size(); j++) {
            Statement s = statements.get(j);
            if (s.comment || s.blank) continue;
            String cmd = commandWord(s.text);
            if ("EXECUTE".equals(cmd)) { end = j; break; }
            if ("RECODE".equals(cmd) && sameTarget(target, extractRecodeTarget(s.text))) { end = j; continue; }
            if (isLabelForTarget(s.text, target)) { end = j; continue; }
            break;
        }
        return end;
    }

    private static int collectIfAssignRule(List<Statement> statements, int i) {
        String target = extractIfAssignTarget(statements.get(i).text);
        int end = i;
        for (int j = i + 1; j < statements.size(); j++) {
            Statement s = statements.get(j);
            if (s.comment || s.blank) continue;
            String cmd = commandWord(s.text);
            if ("EXECUTE".equals(cmd)) { end = j; break; }
            if ("IF".equals(cmd) && sameTarget(target, extractIfAssignTarget(s.text))) { end = j; continue; }
            if (isLabelForTarget(s.text, target)) { end = j; continue; }
            break;
        }
        return end;
    }

    private static int includeTrailingExecute(List<Statement> statements, int end) {
        for (int j = end + 1; j < statements.size(); j++) {
            Statement s = statements.get(j);
            if (s.comment || s.blank) continue;
            if ("EXECUTE".equals(commandWord(s.text))) return j;
            return end;
        }
        return end;
    }

    static RuleDefinition classifyBlock(String block) {
        if (SELECT_PAT.matcher(block).find()) return parseOutputGroup(block);
        if (SORT_PAT.matcher(block).find()) return parseDuplicateMark(block);

        Matcher cm = COMPUTE_PAT.matcher(block);
        if (cm.find()) {
            RuleDefinition rule = parseComputeBlock(block, cm);
            if (rule != null) return rule;
        }

        Matcher rm = RECODE_PAT.matcher(block);
        if (rm.find()) return parseRecodeBlock(block);

        if (DO_IF_PAT.matcher(block).find() || IF_PAT.matcher(block).find()) {
            return parseConditionalBlock(block);
        }
        return null;
    }

    // ── COMPUTE classification (no Chinese keyword checks) ──

    private static RuleDefinition parseComputeBlock(String block, Matcher cm) {
        String target = stripTrailingDot(cm.group(1).trim());
        String expr = compact(cm.group(2));
        List<String> vars = SpssRuleParser.extractVariables(expr);
        String exprUpper = expr.toUpperCase(Locale.ROOT);

        if (KNOWN_INTERMEDIATE.contains(target.toUpperCase(Locale.ROOT))) {
            RuleDefinition r = new RuleDefinition(target, RuleType.COMPUTE_INTERMEDIATE, block, vars);
            r.setExpression(expr);
            return r;
        }

        if (isIdCheckExpression(exprUpper)) {
            RuleDefinition r = new RuleDefinition(target, RuleType.IDENTITY_CHECK, block, vars);
            r.setExpression(expr);
            return r;
        }

        if (hasDocSources(vars) || hasDocSources(extractAllVars(block))) {
            RuleDefinition r = new RuleDefinition(target, RuleType.DOCUMENT_CHECK, block, vars);
            r.setExpression(expr);
            return r;
        }

        if (exprUpper.matches("^\\s*0\\s*$") || exprUpper.contains("$SYSMIS")) {
            RuleDefinition r = new RuleDefinition(target, RuleType.COMPUTE_INTERMEDIATE, block, vars);
            r.setExpression(expr);
            return r;
        }

        RuleDefinition r = new RuleDefinition(target, RuleType.COMPUTE_INTERMEDIATE, block, vars);
        r.setExpression(expr);
        return r;
    }

    private static boolean isIdCheckExpression(String expr) {
        boolean hasPowers = expr.contains("**") && (expr.contains("10") || expr.contains("PROVINCE") || expr.contains("CITY"));
        boolean hasIdVars = expr.contains("PROVINCE") || expr.contains("CITY") || expr.contains("COUNTY")
                || expr.contains("POINT") || expr.contains("SCHOOL");
        return hasPowers && hasIdVars;
    }

    private static boolean hasDocSources(List<String> vars) {
        for (String v : vars) {
            if (DOC_SOURCE_PAT.matcher(v).find()) return true;
        }
        return false;
    }

    private static List<String> extractAllVars(String block) {
        return SpssRuleParser.extractVariables(block);
    }

    // ── RECODE classification (no Chinese keyword checks) ──

    private static RuleDefinition parseRecodeBlock(String block) {
        Matcher m = Pattern.compile("(?i)RECODE\\s+(\\S+).*?INTO\\s+(\\S+)", Pattern.DOTALL).matcher(block);
        if (m.find()) {
            String source = stripTrailingDot(m.group(1).trim());
            String target = stripTrailingDot(m.group(2).trim());
            List<String> vars = Collections.singletonList(source);
            RuleType type = classifyByRecodePattern(source, target, block);
            RuleDefinition r = new RuleDefinition(target, type, block, vars);
            r.setExpression(source + " -> " + target);
            return r;
        }
        m = Pattern.compile("(?i)RECODE\\s+(\\S+)").matcher(block);
        if (m.find()) {
            String target = stripTrailingDot(m.group(1).trim());
            List<String> vars = SpssRuleParser.extractVariables(block);
            RuleType type = classifyByRecodePattern(target, target, block);
            return new RuleDefinition(target, type, block, vars);
        }
        return null;
    }

    /** Classify RECODE by SPSS pattern, not by Chinese target name */
    private static RuleType classifyByRecodePattern(String source, String target, String block) {
        String blockUpper = block.toUpperCase(Locale.ROOT);

        if (blockUpper.contains(" THRU ") && (blockUpper.contains("HIGHEST") || blockUpper.contains("LOWEST"))) {
            return RuleType.OUTCOME_DETERMINATION;
        }
        if (blockUpper.contains("SYSMIS") || blockUpper.contains("MISSING")) {
            return RuleType.MISSING_CHECK;
        }
        if (blockUpper.contains(" THRU ") && !blockUpper.contains("HIGHEST") && !blockUpper.contains("LOWEST")) {
            return RuleType.RANGE_CHECK;
        }
        if (DOC_SOURCE_PAT.matcher(source).find() || DOC_SOURCE_PAT.matcher(block).find()) {
            return RuleType.DOCUMENT_CHECK;
        }
        if (blockUpper.contains(" INTO ")) {
            return RuleType.MISSING_CHECK;
        }
        return RuleType.MISSING_CHECK;
    }

    // ── CONDITIONAL_BLOCK ──

    private static RuleDefinition parseConditionalBlock(String block) {
        List<String> vars = SpssRuleParser.extractVariables(block);
        String blockUpper = block.toUpperCase(Locale.ROOT);

        String target = null;
        String expr = null;
        Matcher cm = COMPUTE_PAT.matcher(block);
        if (cm.find()) {
            target = stripTrailingDot(cm.group(1).trim());
            expr = compact(cm.group(2));
        } else {
            Matcher rm = Pattern.compile("(?i)RECODE\\s+(\\S+).*?INTO\\s+(\\S+)", Pattern.DOTALL).matcher(block);
            if (rm.find()) target = stripTrailingDot(rm.group(2).trim());
            else {
                rm = Pattern.compile("(?i)RECODE\\s+(\\S+)").matcher(block);
                if (rm.find()) target = stripTrailingDot(rm.group(1).trim());
            }
        }
        if (target == null || target.isEmpty()) {
            Matcher dm = Pattern.compile("(?i)DO\\s+IF\\s*\\(([^)]+)\\)").matcher(block);
            if (dm.find()) target = "COND_" + dm.group(1).replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("_+", "_");
            else target = "conditional";
        }

        RuleType type = RuleType.CONDITIONAL_BLOCK;
        if (hasDocSources(vars) || DOC_SOURCE_PAT.matcher(block).find()) type = RuleType.DOCUMENT_CHECK;
        else if (blockUpper.contains("SYSMIS") || blockUpper.contains("MISSING")) type = RuleType.MISSING_CHECK;
        else if (blockUpper.contains(" THRU ") && (blockUpper.contains("HIGHEST") || blockUpper.contains("LOWEST")))
            type = RuleType.OUTCOME_DETERMINATION;

        RuleDefinition r = new RuleDefinition(target, type, block, vars);
        if (expr != null) r.setExpression(expr);
        String descBlock = block.replaceAll("\\s+", " ").trim();
        r.setDescription("条件判断块: " + descBlock.substring(0, Math.min(80, descBlock.length())));
        return r;
    }

    // ── OUTPUT_GROUP / DUPLICATE_MARK ──

    private static RuleDefinition parseOutputGroup(String block) {
        Matcher sm = Pattern.compile("(?is)SELECT\\s+IF\\s*\\((.*?)\\)").matcher(block);
        Matcher vm = Pattern.compile("(?is)SAVE\\s+OUTFILE\\s*=\\s*'([^']+)'").matcher(block);
        if (sm.find() && vm.find()) {
            String cond = sm.group(1).trim();
            String path = vm.group(1).trim();
            String name = path.replace('\\', '/');
            name = name.substring(name.lastIndexOf('/') + 1).replace(".sav", "");
            RuleDefinition r = new RuleDefinition(name, RuleType.OUTPUT_GROUP, block,
                    SpssRuleParser.extractVariables(cond));
            r.setSelectCondition(cond);
            r.setOutputName(name);
            return r;
        }
        return null;
    }

    private static RuleDefinition parseDuplicateMark(String block) {
        Matcher m = Pattern.compile("(?is)/FIRST\\s*=\\s*(\\S+)\\s+/LAST\\s*=\\s*(\\S+)").matcher(block);
        if (m.find()) {
            Matcher by = Pattern.compile("(?is)/BY\\s+([^\\s\\r\\n]+)").matcher(block);
            String byVar = by.find() ? by.group(1).trim() : "ID1";
            return new RuleDefinition(m.group(1), RuleType.DUPLICATE_MARK, block,
                    Collections.singletonList(byVar));
        }
        return null;
    }


    // ── Source variable cleanup ──

    private static void sanitizeRuleSources(List<RuleDefinition> rules) {
        if (rules == null) return;
        for (RuleDefinition rule : rules) {
            if (rule == null) continue;
            rule.setSourceVariables(cleanSourceVariables(rule.getSourceVariables(), rule.getTarget()));
        }
    }

    private static List<String> cleanSourceVariables(List<String> vars, String target) {
        LinkedHashMap<String, String> cleaned = new LinkedHashMap<>();
        String targetNorm = SpssUtil.normalize(target);
        if (vars != null) {
            for (String var : vars) {
                String v = normalizeSourceToken(var);
                if (v == null) continue;
                String norm = SpssUtil.normalize(v);
                if (norm.isEmpty()) continue;
                if (targetNorm.equals(norm)) continue;
                if (isBadSourceToken(v, norm)) continue;
                cleaned.put(norm, v);
            }
        }
        return new ArrayList<>(cleaned.values());
    }

    private static String normalizeSourceToken(String var) {
        if (var == null) return null;
        String v = stripTrailingDot(var.trim());
        v = v.replaceAll("^[,;:，；：]+|[,;:，；：]+$", "");
        v = v.replaceAll("^[`\"']+|[`\"']+$", "");
        return v.trim().isEmpty() ? null : v.trim();
    }

    private static boolean isBadSourceToken(String v, String norm) {
        if (v.length() > 64) return true;
        if (v.contains("\\") || v.contains("/") || v.contains(":")) return true;
        if (norm.matches("^[0-9]+$")) return true;
        if (Arrays.asList("COMPUTE", "EXECUTE", "RECODE", "IF", "DO", "END", "SELECT", "SAVE", "OUTFILE",
                "VARIABLE", "VALUE", "LABELS", "CTABLES", "DESCRIPTIVES", "FREQUENCIES", "SORT", "MATCH",
                "FILE", "BY", "FIRST", "LAST", "ALL", "KEEP", "DROP", "TO", "WITH", "AND", "OR", "NOT").contains(norm)) {
            return true;
        }
        // Usually a parsed Chinese sentence/comment rather than a real SPS variable.
        return v.matches("[\\p{IsHan}]{18,}");
    }

    // ── Merge logic ──

    static List<RuleDefinition> mergeSameTarget(List<RuleDefinition> rules) {
        Map<String, RuleDefinition> merged = new LinkedHashMap<>();
        for (RuleDefinition r : rules) {
            String key = SpssUtil.normalize(r.getTarget());
            RuleDefinition existing = merged.get(key);
            if (existing == null) {
                merged.put(key, r);
                continue;
            }

            RuleDefinition keep;
            RuleDefinition other;
            if (existing.getType() == RuleType.COMPUTE_INTERMEDIATE && r.getType() != RuleType.COMPUTE_INTERMEDIATE) {
                keep = r;
                other = existing;
            } else if (existing.getType() != RuleType.COMPUTE_INTERMEDIATE && r.getType() == RuleType.COMPUTE_INTERMEDIATE) {
                keep = existing;
                other = r;
            } else if (r.getType() == RuleType.CONDITIONAL_BLOCK
                    && existing.getExpression() != null
                    && existing.getExpression().toUpperCase(Locale.ROOT).contains("$SYSMIS")) {
                keep = existing;
                other = r;
                keep.setExpression(r.getExpression());
                keep.setDescription(r.getDescription());
            } else {
                keep = r;
                other = existing;
            }

            mergeRuleMetadata(keep, other);
            merged.put(key, keep);
        }
        return new ArrayList<>(merged.values());
    }

    private static void mergeRuleMetadata(RuleDefinition keep, RuleDefinition other) {
        if (keep == null || other == null) return;
        int start = minPositiveLine(keep.getStartLine(), other.getStartLine());
        int end = Math.max(keep.getEndLine(), other.getEndLine());
        if (start > 0) keep.setStartLine(start);
        if (end > 0) keep.setEndLine(end);

        if ((keep.getDescription() == null || keep.getDescription().isEmpty() || isCommandLikeTitle(keep.getDescription()))
                && other.getDescription() != null && !isCommandLikeTitle(other.getDescription())) {
            keep.setDescription(other.getDescription());
        }
        if ((keep.getSegmentTitle() == null || keep.getSegmentTitle().isEmpty() || isCommandLikeTitle(keep.getSegmentTitle()))
                && other.getSegmentTitle() != null && !isCommandLikeTitle(other.getSegmentTitle())) {
            keep.setSegmentTitle(other.getSegmentTitle());
        }
        if ((keep.getSplitReason() == null || keep.getSplitReason().isEmpty()) && other.getSplitReason() != null) {
            keep.setSplitReason(other.getSplitReason());
        }
        if (keep.getExpression() == null || keep.getExpression().isEmpty() || keep.getExpression().contains(" -> ")) {
            if (other.getExpression() != null && !other.getExpression().isEmpty()) keep.setExpression(other.getExpression());
        }

        List<String> mergedSrc = new ArrayList<>(keep.getSourceVariables());
        for (String sv : other.getSourceVariables()) {
            if (!mergedSrc.contains(sv)) mergedSrc.add(sv);
        }
        keep.setSourceVariables(mergedSrc);
    }

    private static int minPositiveLine(int a, int b) {
        if (a <= 0) return b;
        if (b <= 0) return a;
        return Math.min(a, b);
    }

    // ── V1 execution merge ──

    private static void mergeV1Execution(String spsText, List<RuleDefinition> v2Rules) {
        List<com.gxaysoft.project.spsscheck.v1.model.SpssCheckRule> v1Rules =
                SpssRuleParser.parseRules(spsText);

        Set<String> matchedV1 = new LinkedHashSet<>();

        for (RuleDefinition v2 : v2Rules) {
            String v2target = SpssUtil.normalize(v2.getTarget());
            for (com.gxaysoft.project.spsscheck.v1.model.SpssCheckRule v1 : v1Rules) {
                if (SpssUtil.normalize(v1.getTarget()).equals(v2target)) {
                    matchedV1.add(SpssUtil.normalize(v1.getTarget()));
                    v2.setSteps(new ArrayList<>(v1.getSteps()));
                    if (v2.getExpression() == null && v1.getExpression() != null && !v1.getExpression().isEmpty()) {
                        v2.setExpression(v1.getExpression());
                    }
                    String v1desc = v1.getDescription();
                    if (v1desc != null && !v1desc.isEmpty()
                            && (v2.getDescription() == null || "null".equals(v2.getDescription()))) {
                        v2.setDescription(v1desc);
                    }
                    List<String> mergedSrc = new ArrayList<>(v2.getSourceVariables());
                    for (String sv : v1.getSourceVariables()) {
                        if (!mergedSrc.contains(sv)) mergedSrc.add(sv);
                    }
                    v2.setSourceVariables(mergedSrc);
                    break;
                }
            }
        }

        // Add unmatched V1 rules as new V2 RuleDefinitions (classified by SPSS pattern)
        for (com.gxaysoft.project.spsscheck.v1.model.SpssCheckRule v1 : v1Rules) {
            String key = SpssUtil.normalize(v1.getTarget());
            if (!matchedV1.contains(key)) {
                RuleType type = classifyV1Rule(v1);
                RuleDefinition v2 = new RuleDefinition(
                        v1.getTarget(), type,
                        v1.getSpssSource(),
                        new ArrayList<>(v1.getSourceVariables()));
                v2.setSteps(new ArrayList<>(v1.getSteps()));
                v2.setExpression(v1.getExpression());
                String desc = v1.getDescription();
                if (desc != null && !desc.isEmpty()) v2.setDescription(desc);
                applyLineRangeFromSource(spsText, v2, v1.getSpssSource());
                v2Rules.add(v2);
            }
        }
    }

    /** Classify a V1 rule into V2 RuleType using SPSS syntax patterns only */
    private static RuleType classifyV1Rule(com.gxaysoft.project.spsscheck.v1.model.SpssCheckRule v1) {
        String expr = v1.getExpression() != null ? v1.getExpression().toUpperCase(Locale.ROOT) : "";
        List<String> srcVars = v1.getSourceVariables();
        String spss = v1.getSpssSource() != null ? v1.getSpssSource().toUpperCase(Locale.ROOT) : "";

        if (isIdCheckExpression(expr)) return RuleType.IDENTITY_CHECK;
        if (hasDocSources(srcVars) || DOC_SOURCE_PAT.matcher(spss).find()) return RuleType.DOCUMENT_CHECK;
        if (spss.contains(" THRU ") && (spss.contains("HIGHEST") || spss.contains("LOWEST")))
            return RuleType.OUTCOME_DETERMINATION;
        if (spss.contains(" THRU ") && !spss.contains("SYSMIS") && !spss.contains("MISSING"))
            return RuleType.RANGE_CHECK;
        if (spss.contains("SYSMIS") || spss.contains("MISSING("))
            return RuleType.MISSING_CHECK;
        if (expr.equals("0") || spss.contains("=1") || spss.contains("=0"))
            return RuleType.MISSING_CHECK;
        if (!v1.isCheckRule()) return RuleType.COMPUTE_INTERMEDIATE;
        return RuleType.MISSING_CHECK;
    }

    private static boolean isCommandLikeTitle(String title) {
        if (title == null || title.trim().isEmpty()) return true;
        String t = title.trim().toUpperCase(Locale.ROOT);
        return t.matches("^(COMPUTE|RECODE|IF|DO IF|END IF|EXECUTE|SELECT IF|SAVE OUTFILE|SORT CASES|MATCH FILES|DATASET|FILTER|USE|DESCRIPTIVES|FREQUENCIES|CTABLES|VARIABLE|VALUE|FORMATS|LEAVE|SPLIT)\\b.*");
    }

    // ── Line-aware helper methods ──

    private static void applyLineRangeFromSource(String fullText, RuleDefinition rule, String source) {
        if (fullText == null || source == null || source.isEmpty()) return;
        int idx = fullText.indexOf(source);
        if (idx < 0) idx = fullText.replace("\r\n", "\n").indexOf(source.replace("\r\n", "\n"));
        if (idx < 0) return;
        int startLine = 1;
        for (int i = 0; i < idx; i++) if (fullText.charAt(i) == '\n') startLine++;
        int endLine = startLine;
        for (int i = idx; i < Math.min(fullText.length(), idx + source.length()); i++) if (fullText.charAt(i) == '\n') endLine++;
        rule.setStartLine(startLine);
        rule.setEndLine(endLine);
    }

    private static boolean isCommentLine(String line) {
        return stripBom(line).trim().startsWith("*");
    }

    private static String stripBom(String s) {
        return s == null ? "" : s.replace("\ufeff", "");
    }

    private static boolean lineHasTerminalPeriod(String line) {
        if (line == null) return false;
        int last = -1;
        for (int i = 0; i < line.length(); i++) if (!Character.isWhitespace(line.charAt(i))) last = i;
        if (last < 0) return false;
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i <= last; i++) {
            char ch = line.charAt(i);
            if (ch == '\'' && !inDouble) inSingle = !inSingle;
            else if (ch == '"' && !inSingle) inDouble = !inDouble;
            else if (ch == '.' && !inSingle && !inDouble && i == last) return true;
        }
        return false;
    }

    private static boolean startsWith(String text, String command) {
        return text != null && Pattern.compile("(?is)^\\s*" + Pattern.quote(command) + "\\b").matcher(text).find();
    }

    private static String commandWord(String text) {
        if (text == null) return "";
        Matcher m = Pattern.compile("(?is)^\\s*([A-Z]+)").matcher(text.trim().toUpperCase(Locale.ROOT));
        return m.find() ? m.group(1) : "";
    }

    private static boolean isMajorStart(String text) {
        String cmd = commandWord(text);
        return Arrays.asList("COMPUTE", "RECODE", "IF", "DO", "SELECT", "SAVE", "SORT", "MATCH",
                "DESCRIPTIVES", "FREQUENCIES", "CTABLES", "DATASET", "FILTER", "USE", "STRING",
                "NUMERIC", "VARIABLE", "VALUE", "FORMATS", "LEAVE", "SPLIT").contains(cmd);
    }

    private static String extractComputeTarget(String text) {
        Matcher m = Pattern.compile("(?is)^\\s*COMPUTE\\s+([^=\\r\\n]+?)\\s*=").matcher(text);
        return m.find() ? stripTrailingDot(m.group(1).trim()) : null;
    }

    private static String extractIfAssignTarget(String text) {
        if (text == null) return null;
        Pattern head = Pattern.compile("(?is)^\\s*IF\\s*\\(");
        Matcher m = head.matcher(text);
        if (!m.find()) return null;
        int parenStart = m.end() - 1;
        int parenEnd = findBalancedParen(text, parenStart);
        if (parenEnd < 0 || parenEnd + 1 >= text.length()) return null;
        String after = text.substring(parenEnd + 1);
        int eq = after.indexOf('=');
        if (eq < 0) return null;
        return stripTrailingDot(after.substring(0, eq).trim());
    }

    private static String extractRecodeTarget(String text) {
        if (text == null) return null;
        Matcher into = Pattern.compile("(?is)^\\s*RECODE\\s+.+?\\s+INTO\\s+([^\\.\\s]+)").matcher(text);
        if (into.find()) return stripTrailingDot(into.group(1).trim());
        Matcher self = Pattern.compile("(?is)^\\s*RECODE\\s+([^\\s\\(]+)").matcher(text);
        return self.find() ? stripTrailingDot(self.group(1).trim()) : null;
    }

    private static boolean isLabelForTarget(String text, String target) {
        if (target == null || text == null) return false;
        String norm = SpssUtil.normalize(target);
        Matcher m = Pattern.compile("(?is)^\\s*(VARIABLE\\s+LABELS|VALUE\\s+LABELS|VARIABLE\\s+LEVEL|VARIABLE\\s+WIDTH|FORMATS)\\s+([^\\s\\(]+)").matcher(text);
        return m.find() && norm.equals(SpssUtil.normalize(m.group(2).trim()));
    }

    private static boolean sameTarget(String left, String right) {
        return left != null && right != null && SpssUtil.normalize(left).equals(SpssUtil.normalize(right));
    }

    private static boolean isHeadingComment(String text) {
        if (!isCommentLine(text)) return false;
        String s = cleanTitle(text);
        if (s == null || s.isEmpty()) return false;
        if (s.toUpperCase(Locale.ROOT).contains("ENCODING:")) return false;
        if (s.matches("^\\d+(?:\\.\\d+)*\\s*[.．、]?.*")) return true;
        if (s.matches("^题[一二三四五六七八九十0-9].*")) return true;
        if (s.matches(".*表\\s*\\d+\\s*[-－]\\s*\\d+.*")) return true;
        String compact = s.replace(" ", "");
        String[] keys = {"查看", "标记", "拷贝", "复制", "清理", "删除", "剔出", "数据库", "逻辑检验",
                "基本信息", "人员配备", "经费", "学校情况", "学生情况", "年龄", "身高", "体重",
                "BMI", "血压", "近视", "龋齿", "脊柱", "筛查", "判定", "异常", "缺失", "可疑",
                "定义", "计算", "总样本", "地市", "区县", "幼儿园", "中小学", "大学", "男生", "女生", "附件"};
        if (compact.length() <= 60) {
            for (String k : keys) if (compact.contains(k)) return true;
        }
        return false;
    }

    private static String cleanTitle(String text) {
        if (text == null) return "";
        String s = stripBom(text).trim();
        s = s.replaceAll("^\\*+\\s*", "");
        s = s.replaceAll("[\\*。\\.\\s]+$", "").trim();
        return s.length() > 120 ? s.substring(0, 120) : s;
    }

    private static int findBalancedParen(String text, int start) {
        if (start < 0 || start >= text.length() || text.charAt(start) != '(') return -1;
        int depth = 1;
        boolean inSingle = false, inDouble = false;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (!inSingle && !inDouble) {
                if (c == '(') depth++;
                else if (c == ')') { depth--; if (depth == 0) return i; }
            }
        }
        return -1;
    }

    // ── Helpers ──

    private static String compact(String s) { return s.replaceAll("\\s+", " ").trim(); }
    static String stripTrailingDot(String s) { return s == null ? null : s.replaceAll("[.。]+$", ""); }

    private static class Statement {
        final int startLine;
        final int endLine;
        final String text;
        final boolean comment;
        final boolean blank;

        Statement(int startLine, int endLine, String text, boolean comment, boolean blank) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.text = text == null ? "" : text;
            this.comment = comment;
            this.blank = blank;
        }
    }
}
