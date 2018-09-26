package com.restwl.rsswidget.loaders;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import java.io.IOException;
import java.util.List;

import com.restwl.rsswidget.contentprovider.WidgetContentProvider;
import com.restwl.rsswidget.model.LoaderData;
import com.restwl.rsswidget.model.News;
import com.restwl.rsswidget.network.HttpConnector;
import com.restwl.rsswidget.network.parsers.XmlParser;
import com.restwl.rsswidget.utils.PreferencesManager;

import static com.restwl.rsswidget.utils.WidgetConstants.EXTRA_URL;

public class DataLoader extends AsyncTaskLoader<LoaderData> {

    public static final int LOADER_ID = 1010;

    private String urlString;
    private LoaderData.Status status = LoaderData.Status.Undefined;
    private List<News> newsList;

    public DataLoader(Context context, Bundle args) {
        super(context);
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

    @Nullable
    @Override
    public LoaderData loadInBackground() {
        if (urlString == null) return null;
        try (HttpConnector connector = new HttpConnector(urlString)) {
            connector.sendRequest();
            if (connector.getInputStreamServerError() != null) {
                status = LoaderData.Status.ServerError;
            } else {
                status = LoaderData.Status.Success;
                try {
                    newsList = XmlParser.parseRssData(connector.getInputStreamContent());
                    WidgetContentProvider.clearNews(getContext());
                    PreferencesManager.resetNewsIndexForAllWidgets(getContext());
                    WidgetContentProvider.insertNewsList(getContext(), newsList);
                    WidgetContentProvider.executeHousekeeper(getContext());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    status = LoaderData.Status.ResourceIsNotRssService;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            status = LoaderData.Status.NetworkError;
        }
        return new LoaderData(urlString, newsList, status);
    }
}
