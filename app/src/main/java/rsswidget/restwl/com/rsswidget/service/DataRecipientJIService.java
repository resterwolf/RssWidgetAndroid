package rsswidget.restwl.com.rsswidget.service;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import rsswidget.restwl.com.rsswidget.contentprovider.WidgetContentProvider;
import rsswidget.restwl.com.rsswidget.model.News;
import rsswidget.restwl.com.rsswidget.network.HttpConnector;
import rsswidget.restwl.com.rsswidget.network.parsers.XmlParser;
import rsswidget.restwl.com.rsswidget.utils.PreferencesManager;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;

import static rsswidget.restwl.com.rsswidget.utils.WidgetConstants.TAG;

public class DataRecipientJIService extends JobIntentService {

    public static final int JOB_ID = 1111;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String urlString = PreferencesManager.extractUrl(getApplicationContext());
        if (TextUtils.isEmpty(urlString)) {
            stopSelf();
            return;
        }
        try (HttpConnector connector = new HttpConnector(urlString)) {
            connector.sendRequest();
            if (connector.getInputStreamServerError() == null) {
                List<News> newsList = XmlParser.parseRssData(connector.getInputStreamContent());
                WidgetContentProvider.insertAllNewsInNewsTable(getApplicationContext(), newsList);
                RssWidgetProvider.sendActionToAllWidgets(getApplicationContext(), AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            }
            Log.d(TAG, "DataRecipientJIService: News download and insert");
        } catch (Exception ex) {
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
        JobIntentService.enqueueWork(context, DataRecipientJIService.class, JOB_ID, intent);
    }
}
