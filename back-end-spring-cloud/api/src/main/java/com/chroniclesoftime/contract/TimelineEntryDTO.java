package com.chroniclesoftime.contract;

/** 跨限界上下文共享的 DTO 契约。 */
public class TimelineEntryDTO {
    private Long id;
    private String type;
    private String title;
    private String summary;
    private String occurredAt;
    private String link;
    private String color;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getSummary() { return summary; }
    public void setSummary(String v) { this.summary = v; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String v) { this.occurredAt = v; }
    public String getLink() { return link; }
    public void setLink(String v) { this.link = v; }
    public String getColor() { return color; }
    public void setColor(String v) { this.color = v; }
}
