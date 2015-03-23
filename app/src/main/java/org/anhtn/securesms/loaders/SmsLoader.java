package org.anhtn.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import org.anhtn.securesms.model.ContactObject;
import org.anhtn.securesms.model.SentMessageModel;
import org.anhtn.securesms.model.SmsObject;
import org.anhtn.securesms.utils.CacheHelper;
import org.anhtn.securesms.utils.Country;
import org.anhtn.securesms.utils.PhoneNumberConverterFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SmsLoader extends SimpleBaseLoader<List<SmsObject>> {

    public SmsLoader(Context context) {
        super(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SmsObject> loadInBackground() {
        Uri inboxUri = Uri.parse("content://sms/");
        String[] reqCols = new String[] {"address", "body", "date"};
        List<SmsObject> results = new ArrayList<>();

        Cursor c = getContext().getContentResolver().query(inboxUri, reqCols,
                null, null, "date DESC");
        Set<String> addressSet = new HashSet<>();
        while (c.moveToNext()) {
            String address = c.getString(c.getColumnIndex("address"));
            address = PhoneNumberConverterFactory.getConverter(
                    new Locale("vn", Country.VIETNAM)).toLocal(address);
            final boolean ok = addressSet.add(address);
            if (ok) {
                SmsObject smsObject = new SmsObject();
                smsObject.Address = address;
                smsObject.Content = c.getString(c.getColumnIndex("body"));
                smsObject.Date = c.getString(c.getColumnIndex("date"));
                try {
                    String number = String.valueOf(Long.parseLong(address));
                    smsObject.AddressInContact = phoneLookupFromCache(number);
                    if (smsObject.AddressInContact == null) {
                        smsObject.AddressInContact = phoneLookup(number);
                    }
                } catch (NumberFormatException ignored) {
                }
                results.add(smsObject);
            }
        }
        c.close();

        for (SmsObject smsObject : results) {
            List<SentMessageModel> models = SentMessageModel.findByAddress(getContext(),
                    smsObject.Address, "date DESC", "1");
            if (models.isEmpty()) continue;
            final SentMessageModel model = models.get(0);
            if (Long.parseLong(smsObject.Date) < Long.parseLong(model.Date)) {
                smsObject.Date = model.Date;
                smsObject.Content = model.Body;
            }
        }

        return results;
    }

    private String phoneLookup(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        Cursor c = getContext().getContentResolver().query(uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null, null, null);
        if (c.moveToFirst()) {
            Set<String> results = new HashSet<>();
            do {
                results.add(c.getString(c.getColumnIndex(
                        ContactsContract.PhoneLookup.DISPLAY_NAME)));
            } while (c.moveToNext());
            c.close();

            if (!results.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                int i = 0;
                for (String s : results) {
                    if (i++ > 0) builder.append(", ");
                    builder.append(s);
                }
                return builder.toString();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String phoneLookupFromCache(String number) {
        if (!CacheHelper.getInstance().contains("contact")) return null;
        List<ContactObject> list = (List<ContactObject>)
                CacheHelper.getInstance().get("contact");
        for (ContactObject contact : list) {
            Set<String> results = new HashSet<>();
            for (String phoneNumber : contact.PhoneNumbers.keySet()) {
                if (phoneNumber.equals(number)) {
                    results.add(contact.DisplayName);
                    break;
                }
            }

            if (!results.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                int i = 0;
                for (String s : results) {
                    if (i++ > 0) builder.append(", ");
                    builder.append(s);
                }
                return builder.toString();
            }
        }
        return null;
    }
}
