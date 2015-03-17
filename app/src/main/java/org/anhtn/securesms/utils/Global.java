package org.anhtn.securesms.utils;

import android.util.Log;

public class Global {

    public static void log(Object obj) {
        Log.i("SecureSMS", obj.toString());
    }

    public static void error(Object obj) {
        Log.e("SecureSMS", obj.toString());
    }
}
