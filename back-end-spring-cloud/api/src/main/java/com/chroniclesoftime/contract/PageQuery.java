package com.chroniclesoftime.contract;

/** 跨限界上下文共享的 DTO 契约。 */
public class PageQuery {
    private Integer page = 1;
    private Integer size = 20;
    private String keyword;
    private String category;

    public Integer getPage() { return page; }
    public void setPage(Integer v) { this.page = v; }
    public Integer getSize() { return size; }
    public void setSize(Integer v) { this.size = v; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String v) { this.keyword = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
}
