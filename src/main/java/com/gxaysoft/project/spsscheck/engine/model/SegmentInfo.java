package com.gxaysoft.project.spsscheck.engine.model;

public class SegmentInfo {
    private int startLine;
    private int endLine;
    private String segmentTitle;
    private String splitReason;

    public SegmentInfo() {}
    public SegmentInfo(int startLine, int endLine, String segmentTitle, String splitReason) {
        this.startLine = startLine; this.endLine = endLine;
        this.segmentTitle = segmentTitle; this.splitReason = splitReason;
    }
    public int getStartLine() { return startLine; }
    public void setStartLine(int startLine) { this.startLine = startLine; }
    public int getEndLine() { return endLine; }
    public void setEndLine(int endLine) { this.endLine = endLine; }
    public String getSegmentTitle() { return segmentTitle; }
    public void setSegmentTitle(String segmentTitle) { this.segmentTitle = segmentTitle; }
    public String getSplitReason() { return splitReason; }
    public void setSplitReason(String splitReason) { this.splitReason = splitReason; }
}
