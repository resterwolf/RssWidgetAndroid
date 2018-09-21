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

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof News)) return false;
        News otherMyClass = (News) other;
        if (title.equals(otherMyClass.title) && description.equals(otherMyClass.description) &&
                link.equals(otherMyClass.link)) {
            return true;
        } else {
            return false;
        }
    }
}
