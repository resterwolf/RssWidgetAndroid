package rsswidget.restwl.com.rsswidget.model;

import java.util.Date;

public class LocalNews extends News {
    private int id;
    private long pubDate;

    public LocalNews(int id, String title, String description, long pubDate, String link) {
        super(title, description, link);
        this.id = id;
        this.pubDate = pubDate;
    }

    public int getId() {
        return id;
    }

    public long getPubDate() {
        return pubDate;
    }

    @Override
    public Date convertDate() {
        return new Date(pubDate);
    }
}
