package rsswidget.restwl.com.rsswidget.loaders;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.LoaderData;
import rsswidget.restwl.com.rsswidget.model.News;
import rsswidget.restwl.com.rsswidget.network.HttpConnector;
import rsswidget.restwl.com.rsswidget.network.parsers.XmlParser;

import static rsswidget.restwl.com.rsswidget.utils.WidgetConstants.EXTRA_URL;

public class DataRecipientLoader extends AsyncTaskLoader<LoaderData> {

    public static final int LOADER_ID = 1010;

    private String urlString;
    private LoaderData.Status status = LoaderData.Status.Success;
    private List<News> newsList;
    private DatabaseManager databaseManager;

    public DataRecipientLoader(Context context, Bundle args) {
        super(context);
        databaseManager = new DatabaseManager(context);
        if (args != null) {
            urlString = args.getString(EXTRA_URL);
        }
        if (urlString == null) {
            cancelLoad();
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        releaseResources();
    }

    @Nullable
    @Override
    public LoaderData loadInBackground() {
        if (urlString == null) return null;
        InputStream contentInputStream = null;
        try {
            HttpConnector connector = new HttpConnector(urlString);
            contentInputStream = connector.getContentStream();
        } catch (IOException ex) {
            ex.printStackTrace();
            status = LoaderData.Status.NetworkError;
        }
        if (status == LoaderData.Status.Success) {
            try {
                newsList = XmlParser.parseRssData(contentInputStream);
                databaseManager.deleteAllEntryFromNews();
                databaseManager.insertEntriesInNews(newsList);
            } catch (Exception ex) {
                ex.printStackTrace();
                status = LoaderData.Status.RemoteResourceNotRssService;
            }
        }

        return new LoaderData(urlString, newsList, status);
    }

    private void releaseResources() {
        databaseManager.close();
    }

}
