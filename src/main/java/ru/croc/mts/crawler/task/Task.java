package ru.croc.mts.crawler.task;

public class Task {

    private Integer index;
    private String pageUrl;

    public Task(Integer index){
        this.index = index;
    }

    public Task(String pageUrl){
        this.pageUrl = pageUrl;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }
}
