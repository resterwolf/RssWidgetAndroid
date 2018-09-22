package rsswidget.restwl.com.rsswidget.model;

import java.util.List;

public class LoaderData {

    private String urlString;
    private List<News> newsList;
    private Status status;

    public LoaderData(String urlString, List<News> newsList, Status status) {
        this.urlString = urlString;
        this.newsList = newsList;
        this.status = status;
    }

    public LoaderData(List<News> newsList) {
        this(null, newsList, Status.Success);
    }

    public String getUrlString() {
        return urlString;
    }

    public Status getStatus() {
        return status;
    }

    public List<News> getListNews() {
        return newsList;
    }

    public enum Status {
        Success, RemoteResourceNotRssService, NetworkError
    }
}
