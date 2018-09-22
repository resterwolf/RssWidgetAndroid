package rsswidget.restwl.com.rsswidget.loaders;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import java.util.List;

import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.LoaderData;
import rsswidget.restwl.com.rsswidget.model.News;

public class BlackListLoader extends AsyncTaskLoader<LoaderData> {

    public static final int LOADER_ID = 1020;

    private DatabaseManager databaseManager;

    public BlackListLoader(@NonNull Context context) {
        super(context);
        databaseManager = new DatabaseManager(context);
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
        List<News> newsList = databaseManager.extractAllEntryFromBlackList();
        return new LoaderData(newsList);
    }

    private void releaseResources() {
        databaseManager.close();
    }

}
