package org.anhtn.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import org.anhtn.securesms.model.SmsObject;
import org.anhtn.securesms.utils.CacheHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmsLoader extends SimpleBaseLoader<List<SmsObject>> {

    public SmsLoader(Context context) {
        super(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SmsObject> loadInBackground() {
        Uri inboxUri = Uri.parse("content://sms/");
        String[] reqCols = new String[] {"address, body"};
        final List<SmsObject> results = new ArrayList<>();

        Cursor c = getContext().getContentResolver().query(inboxUri, reqCols,
                null, null, "date DESC");
        Set<String> addressSet = new HashSet<>();
        if (c.moveToFirst()) {
            Map<String, String> lookupData = (Map<String, String>)
                    CacheHelper.getInstance().get("phone_lookup");
            if (lookupData == null) {
                lookupData = new HashMap<>();
                CacheHelper.getInstance().put("phone_lookup", lookupData);
            }
            do {
                String address = c.getString(c.getColumnIndex("address"));
                if (address.startsWith("+84")) {
                    address = address.replace("+84", "0");
                }

                final boolean ok = addressSet.add(address);
                if (ok) {
                    SmsObject smsObject = new SmsObject();
                    smsObject.Address = address;
                    smsObject.Content = c.getString(c.getColumnIndex("body"));
                    try {
                        String number = String.valueOf(Long.parseLong(address));
                        if (lookupData.containsKey(number)) {
                            smsObject.AddressInContact = lookupData.get(number);
                        } else {
                            smsObject.AddressInContact = phoneLookup(number);
                            if (smsObject.AddressInContact != null) {
                                lookupData.put(number, smsObject.AddressInContact);
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                    results.add(smsObject);
                }
            } while (c.moveToNext());
        }
        c.close();

        return results;
    }

    private String phoneLookup(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));
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

            if (results.isEmpty()) return null;
            else {
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
