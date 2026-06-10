package com.gxaysoft.project.spsscheck.io;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TableIdDetector {
    private TableIdDetector() {
    }

    public static long detectMostFrequentTableId(List<AnswerRecord> answers) {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (AnswerRecord answer : answers) {
            if (answer.getTableId() < 0) {
                continue;
            }
            Integer old = counts.get(answer.getTableId());
            counts.put(answer.getTableId(), old == null ? 1 : old + 1);
        }
        long bestTableId = -1L;
        int bestCount = -1;
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestTableId = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return bestTableId;
    }
}
