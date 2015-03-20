package org.anhtn.securesms.utils;

import android.util.Log;

public class Global {

    public static final String DEFAULT_PASSWORD = "securesms";
    public static final String MESSAGE_PREFIX = "AES-128 ";

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
