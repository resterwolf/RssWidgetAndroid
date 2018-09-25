package com.restwl.rsswidget.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.restwl.rsswidget.service.DataLoaderJIService;
import com.restwl.rsswidget.widgedprovider.RssWidgetProvider;

import static com.restwl.rsswidget.utils.WidgetConstants.*;

public class WidgetTasksReceiver extends BroadcastReceiver {

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
            DataLoaderJIService.startService(context);
        }
    }

}
