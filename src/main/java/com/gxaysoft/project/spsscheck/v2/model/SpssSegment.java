package com.gxaysoft.project.spsscheck.v2.model;

/**
 * A line-aware logical segment of an SPSS script.
 *
 * Segment is different from a single SPSS command:
 * - one segment may contain a comment heading plus several complete commands;
 * - a segment must not end inside a multi-line command or a DO IF/LOOP/BEGIN block.
 */
public class SpssSegment {
    private final int index;
    private final int startLine;
    private final int endLine;
    private final String title;
    private final String splitReason;
    private final String text;

    public SpssSegment(int index, int startLine, int endLine, String title, String splitReason, String text) {
        this.index = index;
        this.startLine = startLine;
        this.endLine = endLine;
        this.title = title;
        this.splitReason = splitReason;
        this.text = text;
    }

    public int getIndex() { return index; }
    public int getStartLine() { return startLine; }
    public int getEndLine() { return endLine; }
    public int getLineCount() { return endLine >= startLine ? endLine - startLine + 1 : 0; }
    public String getTitle() { return title; }
    public String getSplitReason() { return splitReason; }
    public String getText() { return text; }
}
