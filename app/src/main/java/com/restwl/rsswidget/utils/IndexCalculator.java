package com.restwl.rsswidget.utils;

public class IndexCalculator {

    // Suppress default constructor for noninstantiability
    private IndexCalculator() {
        throw new AssertionError();
    }

    public static int getDisplayNextIndex(int index, int lastIndex) {
        return index == lastIndex ? 0 : ++index;
    }

    public static int getDisplayPreviousIndex(int index, int lastIndex) {
        return index == 0 ? lastIndex : --index;
    }

    public static int getDisplayIndexAfterRemote(int remoteIndex, int lastIndex) {
        if (remoteIndex <= 0) {
            return 0;
        } else if (remoteIndex >= lastIndex) {
            return lastIndex - 1;
        } else {
            return remoteIndex;
        }
    }

    public static boolean indexIsCorrect(int index, int lastIndex) {
        return index >= 0 && index <= lastIndex;
    }


}
