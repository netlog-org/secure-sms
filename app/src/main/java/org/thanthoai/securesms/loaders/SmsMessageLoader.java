package org.thanthoai.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.thanthoai.securesms.crypto.AESHelper;
import org.thanthoai.securesms.model.SentMessageModel;
import org.thanthoai.securesms.model.SmsMessage;
import org.thanthoai.securesms.utils.locale.Country;
import org.thanthoai.securesms.utils.Global;
import org.thanthoai.securesms.utils.locale.IPhoneNumberConverter.NotValidPersonalNumberException;
import org.thanthoai.securesms.utils.locale.PhoneNumberConverterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SmsMessageLoader extends SimpleBaseLoader<List<SmsMessage>> {

    private final String mAddress;
    private final String mPassphrase;

    public SmsMessageLoader(Context context, String address, String passphrase) {
        super(context);
        mAddress = address;
        mPassphrase = passphrase;
    }

    @Override
    public List<SmsMessage> loadInBackground() {
        Uri uri = Uri.parse("content://sms/");
        String[] reqCols = new String[]{"_id", "body", "date", "type"};
        final List<SmsMessage> results = new ArrayList<>();

        String address = mAddress;
        if (address == null) return null;
        String selection;
        try {
            address = String.valueOf(Long.parseLong(address));
            if (!PhoneNumberConverterFactory.getConverter(
                    new Locale("vn", Country.VIETNAM)).isValidPersonalNumber(address)) {
                throw new NotValidPersonalNumberException();
            }
            selection = "address like '%" + address + "'";
        } catch (NumberFormatException | NotValidPersonalNumberException ex) {
            selection = "address='" + address + "'";
        }

        Cursor c = getContext().getContentResolver().query(uri, reqCols,
                selection, null, "date ASC");
        List<SentMessageModel> models = SentMessageModel.findByAddress(
                getContext(), mAddress);

        int i = 0;
        if (c.moveToFirst() && !models.isEmpty()) {
            SmsMessage obj1 = parseFromCursor(c);
            SmsMessage obj2 = parseFromModel(models.get(0));

            while (true) {
                while (obj1 == null) {
                    if (!c.moveToNext()) break;
                    obj1 = parseFromCursor(c);
                }
                assert obj1 != null;
                if (Long.parseLong(obj1.Date) <= Long.parseLong(obj2.Date)) {
                    results.add(obj1);
                    if (!c.moveToNext()) break;
                    obj1 = parseFromCursor(c);
                } else {
                    results.add(obj2);
                    if (++i >= models.size()) break;
                    obj2 = parseFromModel(models.get(i));
                }
            }
        }
        if (i < models.size()) {
            for (;i < models.size(); i++) {
                results.add(parseFromModel(models.get(i)));
            }
        }
        if (c.getPosition() < c.getCount()) {
            do {
                results.add(parseFromCursor(c));
            } while (c.moveToNext());
        }
        c.close();

        return results;
    }

    private SmsMessage parseFromCursor(Cursor c) {
        SmsMessage sms = new SmsMessage();
        sms.Type = c.getInt(c.getColumnIndex("type"));
        sms.Id = c.getInt(c.getColumnIndex("_id"));
        if (sms.Type != SmsMessage.TYPE_INBOX
                && sms.Type != SmsMessage.TYPE_SENT) {

            Global.log("Ignore sms type: " + sms.Type);
            return null;
        }
        sms.Date = c.getString(c.getColumnIndex("date"));
        String content = c.getString(c.getColumnIndex("body"));
        if (content.startsWith(Global.AES_PREFIX)) {
            content = AESHelper.decryptFromBase64(mPassphrase,
                    content.replace(Global.AES_PREFIX, ""));
        }
        sms.Content = content;
        return sms;
    }

    private SmsMessage parseFromModel(SentMessageModel model) {
        SmsMessage sms = new SmsMessage();
        sms.Type = SmsMessage.TYPE_ENCRYPTED;
        sms.Id = (int) model._Id;
        sms.Date = model.Date;
        sms.Content = AESHelper.decryptFromBase64(mPassphrase,
                model.Body.replace(Global.AES_PREFIX, ""));
        return sms;
    }
}
