package org.thanthoai.securesms.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.thanthoai.securesms.model.SentMessageModel;
import org.thanthoai.securesms.utils.Global;
import org.thanthoai.securesms.utils.SmsSender;

public class SaveSentMessageService extends IntentService {

    public SaveSentMessageService() {
        super("SaveSentMessageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            if (pref.getBoolean("save_sent_message", false)) {
                SentMessageModel model = new SentMessageModel();
                model.Status = intent.getBooleanExtra(SmsSender.EXTRA_SUCCESS, false)
                        ? SentMessageModel.STATUS_SENT_SUCCESS : SentMessageModel.STATUS_SENT_FAIL;
                model.Date = String.valueOf(intent.getLongExtra(SmsSender.EXTRA_TIME_SENT, 0));
                model.Body = intent.getBundleExtra(SmsSender.EXTRA_DATA).getString("encrypted");
                model.Address = intent.getStringExtra(SmsSender.EXTRA_ADDRESS);

                if (model.insert(this) == -1) {
                    Global.error("Save sent message to database failed");
                }
            }
        }
    }
}
