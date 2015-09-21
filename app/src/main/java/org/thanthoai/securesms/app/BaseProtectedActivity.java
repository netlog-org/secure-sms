package org.thanthoai.securesms.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import org.thanthoai.securesms.utils.Global;
import org.thanthoai.securesms.utils.Keys;

public abstract class BaseProtectedActivity extends AppCompatActivity {

    private String mAppPassphrase;

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getBoolean(Keys.PREF_ENABLE_APP_PASSPHRASE, false)) {
            final long timeLeave = System.currentTimeMillis() - Global.sLastTimeLeave;
            if (timeLeave > Global.TIME_NEED_RE_AUTHENTICATE) {
                Intent i = new Intent(this, AuthenticationActivity.class);
                startActivityForResult(i, Global.AUTHENTICATE_REQUEST_CODE);
            }
        }
        else mAppPassphrase = Global.DEFAULT_PASSPHRASE;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Global.sLastTimeLeave = System.currentTimeMillis();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Global.AUTHENTICATE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mAppPassphrase = data.getStringExtra(Keys.APP_PASSPHRASE);
            } else {
                Global.sLastTimeLeave = 0;
                if (getClass().toString().equals(SmsConversationActivity.class.toString())) {
                    finish();
                } else {
                    Intent i = new Intent(this, SmsConversationActivity.class);
                    i.setAction(Intent.ACTION_SHUTDOWN);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected String getAppPassphrase() {
        return mAppPassphrase;
    }
}
