package rsswidget.restwl.com.rsswidget.utils;

import static rsswidget.restwl.com.rsswidget.utils.WidgetConstants.ACTION_SHOW_PREVIOUS;

public class IndexCalculator {

    // Suppress default constructor for noninstantiability
    private IndexCalculator() {
        throw new AssertionError();
    }

    public static boolean isNotOutOfRange(int index, int lastIndex) {
        return index >= 0 && index <= lastIndex;
    }

    public static boolean isOutOfRange(int index, int lastIndex) {
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
            return getPreviousPresentIndex(oldIndex, lastIndex);
        } else {
            return getNextPresentIndex(oldIndex, lastIndex);
        }
    }

    public static int nextIndexAfterRemote(int remoteIndex, int lastIndex) {
        return remoteIndex >= lastIndex ? 0 : remoteIndex;
    }

}
