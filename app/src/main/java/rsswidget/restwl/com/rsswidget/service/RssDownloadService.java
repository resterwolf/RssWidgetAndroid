package rsswidget.restwl.com.rsswidget.service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rsswidget.restwl.com.rsswidget.R;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;
import rsswidget.restwl.com.rsswidget.network.parsers.XmlParser;
import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.RemoteNews;
import rsswidget.restwl.com.rsswidget.network.HttpConnector;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;
import static rsswidget.restwl.com.rsswidget.utils.Constans.NEWS_COUNT;
import static rsswidget.restwl.com.rsswidget.utils.Constans.PREFERENCES_KEY;
import static rsswidget.restwl.com.rsswidget.utils.Constans.TAG;

public class RssDownloadService extends Service {

    private Executor executor;

    private void startServiceOreoCondition() {
        if (Build.VERSION.SDK_INT >= 26) {

            String CHANNEL_ID = "my_service";
            String CHANNEL_NAME = "My Background Service";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setPriority(PRIORITY_MIN).build();

            startForeground(101, notification);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(getApplicationContext())) {
                HttpConnector connector = new HttpConnector("https://lenta.ru/rss/news");
                List<RemoteNews> newsList = XmlParser.parseRssData(connector.getContentStream());
                databaseManager.deleteAllEntryFromNewsTable();
                databaseManager.insertListNews(newsList);
                saveInPreferencesNewsCount(newsList.size());
                RssWidgetProvider.updateSelfData(getApplicationContext());
                Log.d(TAG, "onStartCommand: News download and inserted");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            stopSelf();
        };
        executor.execute(runnable);
        return super.onStartCommand(intent, flags, startId);
    }

    private void saveInPreferencesNewsCount(int count) {
        SharedPreferences preference = getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        preference.edit().putInt(NEWS_COUNT, count).apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startRssDownloadService(Context context) {
        Intent serviceIntent = new Intent(context, RssDownloadService.class);
        context.startService(serviceIntent);
    }
}
