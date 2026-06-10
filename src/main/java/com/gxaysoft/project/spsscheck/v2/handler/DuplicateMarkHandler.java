package com.gxaysoft.project.spsscheck.v2.handler;

import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.v2.model.*;

import java.util.*;

/** SORT CASES + MATCH FILES → FIRST/LAST 重复标记 */
public class DuplicateMarkHandler implements RuleHandler {
    private final String sortVariable = "ID1";     // configurable later
    private final String byVariable = "ID1";

    @Override public RuleType handles() { return RuleType.DUPLICATE_MARK; }

    @Override
    public void execute(RuleDefinition rule, RowContext row) {
        // Handled at the dataset level, not per-row
    }

    @Override
    public String describe(RuleDefinition rule) {
        return "去重标记：按" + byVariable + "分组，每组首条标记为1（不重复）";
    }

    /** Execute on the full dataset: sort, group, mark FIRST/LAST */
    public void executeOnDataset(List<RowContext> rows, String firstVar, String lastVar) {
        String sortBy = sortVariable;
        rows.sort(Comparator.comparing(r -> r.getDecimal(sortBy),
                Comparator.nullsFirst(Comparator.naturalOrder())));

        Map<String, List<RowContext>> groups = new LinkedHashMap<>();
        for (RowContext row : rows) {
            Object v = row.get(byVariable);
            String key = v == null ? "" : String.valueOf(v);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        for (List<RowContext> g : groups.values()) {
            for (int i = 0; i < g.size(); i++) {
                g.get(i).put(firstVar, i == 0 ? 1 : 0);
                g.get(i).put(lastVar, i == g.size() - 1 ? 1 : 0);
            }
        }
    }
}
