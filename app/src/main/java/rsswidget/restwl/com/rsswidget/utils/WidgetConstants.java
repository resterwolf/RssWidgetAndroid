package rsswidget.restwl.com.rsswidget.utils;

public class WidgetConstants {

    // Suppress default constructor for noninstantiability
    private WidgetConstants() {
        throw new AssertionError();
    }

    public static final String TAG = "MyLog";
    public static final String RU_DATE_PATTERN = "dd MMMM yyyy (EEEE)";
    public static final String RSS_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String LOCALE_RU = "ru";
    public static final String ACTION_OPEN_SETTINGS = "ACTION_OPEN_SETTINGS";
    public static final String ACTION_HIDE_NEWS = "ACTION_HIDE_NEWS";
    public static final String ACTION_INITIAL_CONFIG = "ACTION_INITIAL_CONFIG";
    public static final String ACTION_CUSTOM_CONFIG = "ACTION_CUSTOM_CONFIG";
    public static final String ACTION_SHOW_PREVIOUS = "ACTION_SHOW_PREVIOUS";
    public static final String ACTION_SHOW_NEXT = "ACTION_SHOW_NEXT";
    public static final String ACTION_UPDATE_WIDGET_DATA_AND_VIEW = "ACTION_UPDATE_WIDGET_DATA_AND_VIEW";
    public static final String ACTION_START_SERVICE = "com.restw.rsswidget.START_SERVICE";
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String EXTRA_URL = "EXTRA_URL";

}
