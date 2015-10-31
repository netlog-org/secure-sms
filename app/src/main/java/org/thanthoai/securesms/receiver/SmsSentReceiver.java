package org.thanthoai.securesms.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.apache.log4j.Logger;
import org.thanthoai.securesms.services.SaveSentMessageService;
import org.thanthoai.securesms.utils.SmsSender;

public class SmsSentReceiver extends BroadcastReceiver {

    private static final Logger sLogger = Logger.getLogger(SmsSentReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        final long sendId = intent.getLongExtra(SmsSender.EXTRA_SEND_ID, 0);
        sLogger.debug("Sent one part of message id: " + sendId);

        if (SmsSender.getInstance().setPartSent(sendId)) {
            final long now = System.currentTimeMillis();

            Intent i1 = new Intent();
            i1.putExtra(SmsSender.EXTRA_SUCCESS, getResultCode() == Activity.RESULT_OK);
            i1.putExtra(SmsSender.EXTRA_TIME_SENT, now);
            i1.putExtra(SmsSender.EXTRA_ADDRESS, intent.getStringExtra(SmsSender.EXTRA_ADDRESS));
            i1.putExtra(SmsSender.EXTRA_DATA, intent.getBundleExtra(SmsSender.EXTRA_DATA));

            Intent i2 = new Intent(i1);
            i2.setClass(context, SaveSentMessageService.class);
            context.startService(i2);

            i1.setAction(SmsSender.ACTION_SMS_SENT);
            LocalBroadcastManager.getInstance(context).sendBroadcast(i1);
        }
    }
}
