package org.thanthoai.securesms.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.apache.log4j.Logger;
import org.thanthoai.securesms.utils.SmsSender;

public class SmsDeliveryReceiver extends BroadcastReceiver {

    private static final Logger sLogger = Logger.getLogger(SmsDeliveryReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        final long sendId = intent.getLongExtra(SmsSender.EXTRA_SEND_ID, 0);
        sLogger.debug("Delivery one part of message id: " + sendId);

        if (SmsSender.getInstance().setPartDelivery(sendId)) {
            Intent i = new Intent(SmsSender.ACTION_SMS_DELIVERY);
            i.putExtra(SmsSender.EXTRA_SUCCESS, getResultCode() == Activity.RESULT_OK);
            i.putExtra(SmsSender.EXTRA_ADDRESS, intent.getStringExtra(SmsSender.EXTRA_ADDRESS));
            i.putExtra(SmsSender.EXTRA_DATA, intent.getBundleExtra(SmsSender.EXTRA_DATA));
            LocalBroadcastManager.getInstance(context).sendBroadcast(i);
        }
    }
}
