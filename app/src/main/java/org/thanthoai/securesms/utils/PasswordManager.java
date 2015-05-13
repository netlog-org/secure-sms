package org.thanthoai.securesms.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import org.thanthoai.securesms.BuildConfig;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordManager {

    public static void storePassword(Context context, String password) {
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        final String passwordDigest = getPasswordDigest(password);
        editor.putString(Keys.APP_PASSPHRASE, passwordDigest);
        editor.commit();
    }

    public static void removePassword(Context context) {
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        editor.remove(Keys.APP_PASSPHRASE);
        editor.commit();
    }

    public static boolean hasPasswordSaved(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(Keys.APP_PASSPHRASE, null) != null;
    }

    public static boolean isPasswordMatched(Context context, String password) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final String passDigestSaved = pref.getString(Keys.APP_PASSPHRASE, null);
        return passDigestSaved == null
                || passDigestSaved.equals(getPasswordDigest(password));
    }

    private static String getPasswordDigest(String password) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.reset();
            md.update(password.getBytes("UTF-8"));
            return Base64.encodeToString(md.digest(), Base64.DEFAULT);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) throw new AssertionError();
        }
        return null;
    }
}
