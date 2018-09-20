package rsswidget.restwl.com.rsswidget.service;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.RemoteNews;
import rsswidget.restwl.com.rsswidget.network.HttpConnector;
import rsswidget.restwl.com.rsswidget.network.parsers.XmlParser;
import rsswidget.restwl.com.rsswidget.utils.PreferencesManager;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;

import static rsswidget.restwl.com.rsswidget.utils.Constants.ACTION_DATASET_CHANGED;
import static rsswidget.restwl.com.rsswidget.utils.Constants.TAG;

public class RssDownloaderJobIntentService extends JobIntentService {

    public static final int JOB_ID = 1111;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try (DatabaseManager databaseManager = new DatabaseManager(getApplicationContext())) {
            String urlString = PreferencesManager.extractUrl(getApplicationContext());
            if (TextUtils.isEmpty(urlString)) {
//                stopSelf();
                return;
            }
            HttpConnector connector = new HttpConnector(urlString);
            List<RemoteNews> newsList = XmlParser.parseRssData(connector.getContentStream());
            databaseManager.deleteAllEntryFromNewsTable();
            databaseManager.insertListNews(newsList);
            RssWidgetProvider.sendActionToAllWidgets(getApplicationContext(), ACTION_DATASET_CHANGED);
            Log.d(TAG, "onStartCommand: News download and inserted");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
//        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "RssDownloaderJobIntentService onDestroy: ");
    }

    public static void startService(Context context) {
        Intent intent = new Intent();
        JobIntentService.enqueueWork(context, RssDownloaderJobIntentService.class, JOB_ID, intent);
    }
}
