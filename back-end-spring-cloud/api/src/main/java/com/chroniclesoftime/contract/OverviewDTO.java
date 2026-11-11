package com.chroniclesoftime.contract;

/** 跨限界上下文共享的 DTO 契约。 */
public class OverviewDTO {
    private long records;
    private long milestones;
    private long questions;
    private long contents;
    private long plans;
    private long links;

    public long getRecords() { return records; }
    public void setRecords(long v) { this.records = v; }
    public long getMilestones() { return milestones; }
    public void setMilestones(long v) { this.milestones = v; }
    public long getQuestions() { return questions; }
    public void setQuestions(long v) { this.questions = v; }
    public long getContents() { return contents; }
    public void setContents(long v) { this.contents = v; }
    public long getPlans() { return plans; }
    public void setPlans(long v) { this.plans = v; }
    public long getLinks() { return links; }
    public void setLinks(long v) { this.links = v; }
}
