package rsswidget.restwl.com.rsswidget.model;

import java.util.Date;

public abstract class News {

    private String title;
    private String description;
    private String link;

    public News(String title, String description, String link) {
        this.title = title;
        this.description = description;
        this.link = link;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    public abstract Date convertDate();
}
