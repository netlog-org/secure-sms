package org.anhtn.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.anhtn.securesms.crypto.AESHelper;
import org.anhtn.securesms.model.SentMessageModel;
import org.anhtn.securesms.model.SmsContentObject;
import org.anhtn.securesms.utils.Country;
import org.anhtn.securesms.utils.Global;
import org.anhtn.securesms.utils.IPhoneNumberConverter.NotValidPersonalNumberException;
import org.anhtn.securesms.utils.PhoneNumberConverterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
            if (!PhoneNumberConverterFactory.getConverter(
                    new Locale("vn", Country.VIETNAM)).isValidPersonalNumber(address)) {
                throw new NotValidPersonalNumberException();
            }
            selection = "address like '%" + address + "'";
        } catch (NumberFormatException | NotValidPersonalNumberException ex) {
            selection = "address='" + address + "'";
        }

        Cursor c = getContext().getContentResolver().query(uri, reqCols, selection,
                null, "date ASC");
        List<SentMessageModel> models = SentMessageModel.findByAddress(getContext(), mAddress);

        int i = 0;
        if (c.moveToFirst() && !models.isEmpty()) {
            SmsContentObject obj1 = parseFromCursor(c);
            SmsContentObject obj2 = parseFromModel(models.get(0));

            while (true) {
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

    private SmsContentObject parseFromCursor(Cursor c) {
        SmsContentObject sms = new SmsContentObject();
        sms.Type = c.getInt(c.getColumnIndex("type"));
        sms.Id = c.getInt(c.getColumnIndex("_id"));
        if (sms.Type != SmsContentObject.TYPE_INBOX
                && sms.Type != SmsContentObject.TYPE_SENT) {

            Global.log("Ignore sms type: " + sms.Type);
            return null;
        }
        try {
            sms.Date = c.getString(c.getColumnIndex("date"));
            String content = c.getString(c.getColumnIndex("body"));
            if (content.startsWith(Global.MESSAGE_PREFIX)) {
                content = content.replace(Global.MESSAGE_PREFIX, "");
                content = AESHelper.decryptFromBase64(Global.DEFAULT_PASSWORD, content);
                if (content == null) throw new NullPointerException();
            }
            sms.Content = content;
            return sms;
        } catch (NumberFormatException | NullPointerException ignored) {
        }
        return null;
    }

    private SmsContentObject parseFromModel(SentMessageModel model) {
        SmsContentObject sms = new SmsContentObject();
        sms.Type = SmsContentObject.TYPE_ENCRYPTED;
        sms.Id = (int) model._Id;
        sms.Date = model.Date;
        sms.Content = AESHelper.decryptFromBase64(Global.DEFAULT_PASSWORD,
                model.Body.replace(Global.MESSAGE_PREFIX, ""));
        return sms;
    }
}
