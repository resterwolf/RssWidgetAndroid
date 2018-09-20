package rsswidget.restwl.com.rsswidget.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import rsswidget.restwl.com.rsswidget.service.RssDownloadService;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;

import static rsswidget.restwl.com.rsswidget.utils.Constants.*;

public class UpdateReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_BOOT_COMPLETED)) {
            RssWidgetProvider.schedulePeriodicallyTasks(context);
        }

        if (intent.getAction().equals(ACTION_START_SERVICE)) {
            RssDownloadService.startRssDownloadService(context);
        }
    }
}
