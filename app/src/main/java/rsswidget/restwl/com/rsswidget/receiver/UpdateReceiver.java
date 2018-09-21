package rsswidget.restwl.com.rsswidget.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import rsswidget.restwl.com.rsswidget.service.RssDownloaderJobIntentService;
import rsswidget.restwl.com.rsswidget.widgedprovider.RssWidgetProvider;

import static rsswidget.restwl.com.rsswidget.utils.Constants.*;

public class UpdateReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (intentAction == null) return;

        // Alarm manager is reset on device reboot (all scheduled notifications are discarded), so it should be rescheduled
        if (intent.getAction().equals(ACTION_BOOT_COMPLETED)) {
            RssWidgetProvider.schedulePeriodicallyTasks(context);
        }

        // We don't need to care that Android might go to sleep mode, that's why we also don't need to use power manager,
        // JobIntentService will handle everything for us
        if (intent.getAction().equals(ACTION_START_SERVICE)) {
            RssDownloaderJobIntentService.startService(context);
        }
    }

}
