package org.anhtn.securesms.utils;

import android.util.Log;

import java.util.List;

public class Global {

    public static final String DEFAULT_PASSWORD = "securesms";
    public static final String MESSAGE_PREFIX = "AES-128 ";

    public static boolean smartContains(String s1, String s2, List<Integer> matchedPos) {
        if (s2.length() > s1.length()) return false;
        else if (s2.length() == s1.length()) return s1.equals(s2);

        int i = -1;
        for (int j = 0; j < s2.length(); j++) {
            final String s = String.valueOf(s2.charAt(j));
            if ((i = s1.indexOf(s, i+ 1)) < 0) {
                return false;
            }
            if (matchedPos != null) {
                matchedPos.add(i);
            }
        }
        return true;
    }

    public static void log(Object obj) {
        log("SecureSMS-debug", obj);
    }

    public static void log(String tag, Object obj) {
        Log.i(tag, obj.toString());
    }

    public static void error(Object obj) {
        error("SecureSMS-error", obj);
    }

    public static void error(String tag, Object obj) {
        Log.e(tag, obj.toString());
    }
}
