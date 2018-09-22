package rsswidget.restwl.com.rsswidget.utils;

import static rsswidget.restwl.com.rsswidget.utils.Constants.ACTION_SHOW_PREVIOUS;

public class IndexManager {

    public static boolean isNotOutofRange(int index, int lastIndex) {
        return index >= 0 && index <= lastIndex;
    }

    public static boolean isOutofRange(int index, int lastIndex) {
        return index < 0 && index > lastIndex;
    }

    public static int getNextPresentIndex(int oldIndex, int lastIndex) {
        return oldIndex == lastIndex ? 0 : ++oldIndex;
    }

    public static int getPreviousPresentIndex(int index, int lastIndex) {
        return index == 0 ? lastIndex : --index;
    }

    public static int getNewNavigationIndex(String action, int oldIndex, int lastIndex) {
        if (action.contains(ACTION_SHOW_PREVIOUS)) {
            return oldIndex == 0 ? lastIndex : --oldIndex;
        } else {
            return oldIndex == lastIndex ? 0 : ++oldIndex;
        }
    }

    public static int nextIndexAfterRemote(int remoteIndex, int lastIndex) {
        return remoteIndex >= lastIndex - 1 ? 0 : remoteIndex;
    }

}
