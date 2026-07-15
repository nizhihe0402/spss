package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.model.RowContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 重复样本标记：按 BY 变量分组，组内首条 FIRST=1 其余 0、末条 LAST=1 其余 0。
 * 标记不应改变行的原始顺序（原实现的 rows.sort() 会打乱后续结果展示顺序）。
 */
public class DatasetRuleTest {

    @Test
    public void marksFirstAndLastPerGroupWithoutReordering() {
        RowContext r1 = row("s1", "30");
        RowContext r2 = row("s2", "10");
        RowContext r3 = row("s3", "30"); // 与 r1 重复
        RowContext r4 = row("s4", "20");
        List<RowContext> rows = new ArrayList<RowContext>();
        rows.add(r1);
        rows.add(r2);
        rows.add(r3);
        rows.add(r4);

        DatasetRule rule = new DatasetRule("ID1", "ID1", "PrimaryFirst1", "PrimaryLast", "SORT CASES ...");
        rule.execute(rows);

        // 行顺序保持不变
        assertSame(r1, rows.get(0));
        assertSame(r2, rows.get(1));
        assertSame(r3, rows.get(2));
        assertSame(r4, rows.get(3));

        // 首次出现=1（不重复），再次出现=0（重复样本）
        assertEquals(1, decimalInt(r1, "PrimaryFirst1"));
        assertEquals(1, decimalInt(r2, "PrimaryFirst1"));
        assertEquals(0, decimalInt(r3, "PrimaryFirst1"));
        assertEquals(1, decimalInt(r4, "PrimaryFirst1"));

        // 组内末条 LAST=1
        assertEquals(0, decimalInt(r1, "PrimaryLast"));
        assertEquals(1, decimalInt(r2, "PrimaryLast"));
        assertEquals(1, decimalInt(r3, "PrimaryLast"));
        assertEquals(1, decimalInt(r4, "PrimaryLast"));
    }

    @Test
    public void missingByValueFormsItsOwnGroup() {
        RowContext r1 = row("s1", null);
        RowContext r2 = row("s2", null); // 与 r1 同为缺失 → 同组，视为重复
        List<RowContext> rows = new ArrayList<RowContext>();
        rows.add(r1);
        rows.add(r2);

        new DatasetRule("ID1", "ID1", "PrimaryFirst1", "PrimaryLast", "...").execute(rows);

        assertEquals(1, decimalInt(r1, "PrimaryFirst1"));
        assertEquals(0, decimalInt(r2, "PrimaryFirst1"));
    }

    private static RowContext row(String key, String id1) {
        RowContext row = new RowContext(key);
        if (id1 != null) {
            row.put("ID1", new BigDecimal(id1));
        }
        return row;
    }

    private static int decimalInt(RowContext row, String var) {
        Object value = row.get(var);
        assertNotNull(value, var + " 应被标记");
        return new BigDecimal(String.valueOf(value)).intValue();
    }
}
