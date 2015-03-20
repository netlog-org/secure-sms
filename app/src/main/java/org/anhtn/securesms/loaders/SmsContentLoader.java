package org.anhtn.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.anhtn.securesms.crypto.AESHelper;
import org.anhtn.securesms.model.SmsMessage;
import org.anhtn.securesms.utils.Global;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SmsContentLoader extends SimpleBaseLoader<List<SmsMessage>> {

    private String mAddress;

    public SmsContentLoader(Context context, String address) {
        super(context);
        mAddress = address;
    }

    @Override
    public List<SmsMessage> loadInBackground() {
        Uri uri = Uri.parse("content://sms/");
        String[] reqCols = new String[] {"_id", "body", "date", "type"};
        final List<SmsMessage> results = new ArrayList<>();

        String address = mAddress;
        if (address == null) return null;
        String selection;
        try {
            address = String.valueOf(Long.parseLong(address.toString()));
            selection = "address like '%" + address + "'";
        } catch (NumberFormatException ex) {
            selection = "address='" + address + "'";
        }

        Cursor c = getContext().getContentResolver().query(uri, reqCols, selection,
                null, "date ASC");
        if (c.moveToFirst()) {
            do {
                SmsMessage sms = new SmsMessage();
                sms.Type = c.getInt(c.getColumnIndex("type"));
                sms.Id = c.getInt(c.getColumnIndex("_id"));
                if (sms.Type != SmsMessage.TYPE_INBOX
                        && sms.Type != SmsMessage.TYPE_SENT) {

                    Global.log("Ignore sms type: " + sms.Type);
                    continue;
                }
                try {
                    Date date = new Date(Long.parseLong(c.getString(c.getColumnIndex("date"))));
                    sms.Date = DateFormat.getInstance().format(date);
                    String content = c.getString(c.getColumnIndex("body"));
                    if (content.startsWith(Global.MESSAGE_PREFIX)) {
                        content = content.replace(Global.MESSAGE_PREFIX, "");
                        content = AESHelper.decryptFromBase64(Global.DEFAULT_PASSWORD, content);
                        if (content == null) continue;
                    }
                    sms.Content = content;
                    results.add(sms);
                } catch (NumberFormatException ignored) { }
            } while (c.moveToNext());
        }
        c.close();

        return results;
    }
}
