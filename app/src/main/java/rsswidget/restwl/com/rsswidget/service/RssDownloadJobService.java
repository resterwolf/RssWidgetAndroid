package rsswidget.restwl.com.rsswidget.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;
import rsswidget.restwl.com.rsswidget.network.parsers.XmlParser;
import rsswidget.restwl.com.rsswidget.database.DatabaseManager;
import rsswidget.restwl.com.rsswidget.model.RemoteNews;
import rsswidget.restwl.com.rsswidget.network.HttpConnector;

import static rsswidget.restwl.com.rsswidget.utils.Constans.NEWS_COUNT;
import static rsswidget.restwl.com.rsswidget.utils.Constans.PREFERENCES_KEY;
import static rsswidget.restwl.com.rsswidget.utils.Constans.TAG;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RssDownloadJobService extends JobService {

    public static final int JOB_ID = 1100;
    private Executor executor;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Runnable runnable = () -> {
            try (DatabaseManager databaseManager = new DatabaseManager(getApplicationContext())) {
                HttpConnector connector = new HttpConnector("https://lenta.ru/rss/news");
                List<RemoteNews> newsList = XmlParser.parseRssData(connector.getContentStream());
                databaseManager.deleteAllEntryFromNewsTable();
                databaseManager.insertListNews(newsList);
                saveInPreferencesNewsCount(newsList.size());
                RssWidgetProvider.updateSelfData(getApplicationContext());
                Log.d(TAG, "onStartJob: News download and inserted");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            jobFinished(jobParameters, false);
        };
        executor.execute(runnable);
        return true;
    }

    private void saveInPreferencesNewsCount(int count) {
        SharedPreferences preference = getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        preference.edit().putInt(NEWS_COUNT, count).apply();
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
