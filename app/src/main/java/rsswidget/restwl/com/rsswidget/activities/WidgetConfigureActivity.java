package rsswidget.restwl.com.rsswidget.activities;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.concurrent.Executors;

import rsswidget.restwl.com.rsswidget.R;
import rsswidget.restwl.com.rsswidget.network.HttpConnector;

public class WidgetConfigureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        int mAppWidgetId;

        Intent intent = getIntent();
        String intentAction = intent.getAction();

        if (intentAction.equals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)) {

        } else {

        }


        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            RemoteViews views = new RemoteViews(getPackageName(),
                    R.layout.rss_widget_layout);
            appWidgetManager.updateAppWidget(mAppWidgetId, views);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }




//        Runnable runnable = ()-> {
//            try {
//                String content = new HttpConnector("https://lenta.ru/rss/news",5000).getContent();
//                Log.d("MyLog", "content: " + content);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        };
//
//        Executors.newSingleThreadExecutor().execute(runnable);
    }
}
