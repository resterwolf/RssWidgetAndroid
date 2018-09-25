package com.restwl.rsswidget.service;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import com.restwl.rsswidget.contentprovider.WidgetContentProvider;
import com.restwl.rsswidget.model.News;
import com.restwl.rsswidget.network.HttpConnector;
import com.restwl.rsswidget.network.parsers.XmlParser;
import com.restwl.rsswidget.utils.PreferencesManager;
import com.restwl.rsswidget.widgedprovider.RssWidgetProvider;

import static com.restwl.rsswidget.utils.WidgetConstants.TAG;

public class DataLoaderJIService extends JobIntentService {

    public static final int JOB_ID = 1111;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String urlString = PreferencesManager.getRssResourceUrl(getApplicationContext());
        if (TextUtils.isEmpty(urlString)) {
            stopSelf();
            return;
        }
        try (HttpConnector connector = new HttpConnector(urlString)) {
            connector.sendRequest();
            if (connector.getInputStreamServerError() == null) {
                List<News> newsList = XmlParser.parseRssData(connector.getInputStreamContent());
                WidgetContentProvider.insertAllNewsInNewsTable(getApplicationContext(), newsList);
                WidgetContentProvider.executeHousekeeper(getApplicationContext());
                RssWidgetProvider.sendActionToAllWidgets(getApplicationContext(), AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            } else {
                Log.d(TAG, "DataLoaderJIService: News download failed. Server error");
            }
            Log.d(TAG, "DataLoaderJIService: News download and insert");
        } catch (Exception ex) {
            Log.d(TAG, "DataLoaderJIService: News download failed. Network error");
            ex.printStackTrace();
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static void startService(Context context) {
        Intent intent = new Intent();
        JobIntentService.enqueueWork(context, DataLoaderJIService.class, JOB_ID, intent);
    }
}
