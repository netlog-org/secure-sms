package org.anhtn.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.anhtn.securesms.crypto.AESHelper;
import org.anhtn.securesms.model.SmsContentObject;
import org.anhtn.securesms.utils.Global;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SmsContentLoader extends SimpleBaseLoader<List<SmsContentObject>> {

    private String mAddress;

    public SmsContentLoader(Context context, String address) {
        super(context);
        mAddress = address;
    }

    @Override
    public List<SmsContentObject> loadInBackground() {
        Uri uri = Uri.parse("content://sms/");
        String[] reqCols = new String[]{"_id", "body", "date", "type"};
        final List<SmsContentObject> results = new ArrayList<>();

        String address = mAddress;
        if (address == null) return null;
        String selection;
        try {
            address = String.valueOf(Long.parseLong(address));
            if (address.length() < Global.MIN_PHONE_NUMBER_LENGTH)
                throw new NumberFormatException();
            selection = "address like '%" + address + "'";
        } catch (NumberFormatException ex) {
            selection = "address='" + address + "'";
        }

        Cursor c = getContext().getContentResolver().query(uri, reqCols, selection,
                null, "date ASC");
        while (c.moveToNext()) {
            SmsContentObject sms = new SmsContentObject();
            sms.Type = c.getInt(c.getColumnIndex("type"));
            sms.Id = c.getInt(c.getColumnIndex("_id"));
            if (sms.Type != SmsContentObject.TYPE_INBOX
                    && sms.Type != SmsContentObject.TYPE_SENT) {

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
            } catch (NumberFormatException ignored) {
            }
        }
        c.close();

        return results;
    }
}
