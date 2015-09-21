package org.thanthoai.securesms.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import org.thanthoai.securesms.BuildConfig;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AppPassphraseManager {

    public static void store(Context ctx, String passphrase) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        final String pwDigest = getSha1Digest(passphrase);
        editor.putString(Keys.APP_PASSPHRASE, pwDigest);
        editor.apply();
    }

    public static void clear(Context ctx) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.remove(Keys.APP_PASSPHRASE);
        editor.apply();
    }

    public static boolean isExists(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return pref.getString(Keys.APP_PASSPHRASE, null) != null;
    }

    public static boolean isMatched(Context ctx, String passphrase) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        final String pwDigestSaved = pref.getString(Keys.APP_PASSPHRASE, null);
        return pwDigestSaved == null || pwDigestSaved.equals(getSha1Digest(passphrase));
    }

    private static String getSha1Digest(String plain) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.reset();
            md.update(plain.getBytes("UTF-8"));
            return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) throw new AssertionError();
        }
        return null;
    }
}
