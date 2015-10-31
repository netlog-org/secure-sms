package org.thanthoai.securesms.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;

import org.apache.log4j.Logger;
import org.thanthoai.securesms.receiver.SmsDeliveryReceiver;
import org.thanthoai.securesms.receiver.SmsSentReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SmsSender {

    public static final String ACTION_SMS_SENT = "org.thanthoai.securesms.action.SMS_SENT";
    public static final String ACTION_SMS_DELIVERY = "org.thanthoai.securesms.action.SMS_DELIVERY";

    public static final String EXTRA_SEND_ID = "send_id";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_DATA = "ext";
    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_TIME_SENT = "time_sent";

    private static final Logger sLogger = Logger.getLogger(SmsSender.class);

    private static SmsSender sInstance;

    private final Map<Long, Integer> mSentParts = new HashMap<>();
    private final Map<Long, Integer> mDeliveryParts = new HashMap<>();

    public static SmsSender getInstance() {
        if (sInstance == null) {
            sInstance = new SmsSender();
        }
        return sInstance;
    }

    private SmsSender() {
    }

    public void sendSms(Context context, Bundle bundle, String address, String msg) {
        final SmsManager smsManager = SmsManager.getDefault();
        final ArrayList<String> parts = smsManager.divideMessage(msg);
        final long sendId = System.currentTimeMillis();

        Intent i1 = new Intent();
        i1.putExtra(EXTRA_SEND_ID, sendId);
        i1.putExtra(EXTRA_ADDRESS, address);
        i1.putExtra(EXTRA_DATA, bundle);
        i1.setClass(context, SmsSentReceiver.class);
        PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, i1, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent i2 = new Intent(i1);
        i2.setClass(context, SmsDeliveryReceiver.class);
        PendingIntent deliveryIntent = PendingIntent.getBroadcast(context, 0, i2, PendingIntent.FLAG_UPDATE_CURRENT);

        sLogger.info("Message sending: " + sendId);
        mSentParts.put(sendId, parts.size());
        mDeliveryParts.put(sendId, parts.size());

        if (parts.size() > 1) {
            final ArrayList<PendingIntent> sentIntents = new ArrayList<>(parts.size());
            final ArrayList<PendingIntent> deliveryIntents = new ArrayList<>(parts.size());
            for (int i = 0; i < parts.size(); i++) {
                sentIntents.add(sentIntent);
                deliveryIntents.add(deliveryIntent);
            }
            smsManager.sendMultipartTextMessage(address, null, parts, sentIntents, deliveryIntents);
        } else {
            smsManager.sendTextMessage(address, null, msg, sentIntent, deliveryIntent);
        }
    }

    public synchronized boolean setPartSent(long sendId) {
        if (mSentParts.containsKey(sendId)) {
            final int partRemain = mSentParts.get(sendId) - 1;
            if (partRemain == 0) {
                sLogger.debug("Received all parts of message id: " + sendId);
                mSentParts.remove(sendId);
                return true;
            } else {
                mSentParts.put(sendId, partRemain);
                sLogger.debug(String.format("Message %d has %d parts is not sent", sendId, partRemain));
            }
        }
        return false;
    }

    public synchronized boolean setPartDelivery(long sendId) {
        if (mDeliveryParts.containsKey(sendId)) {
            final int partRemain = mDeliveryParts.get(sendId) - 1;
            if (partRemain == 0) {
                sLogger.debug("Delivery all parts of message id: " + sendId);
                mDeliveryParts.remove(sendId);
                return true;
            } else {
                mDeliveryParts.put(sendId, partRemain);
                sLogger.debug(String.format("Message %d has %d parts is not delivery", sendId, partRemain));
            }
        }
        return false;
    }
}
