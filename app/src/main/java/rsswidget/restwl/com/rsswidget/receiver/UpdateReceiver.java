package rsswidget.restwl.com.rsswidget.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import rsswidget.restwl.com.rsswidget.service.RssDownloadService;

public class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        RssDownloadService.startRssDownloadService(context);
    }
}
