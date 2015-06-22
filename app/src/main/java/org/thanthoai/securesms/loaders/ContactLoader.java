package org.thanthoai.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;

import org.thanthoai.securesms.model.Contact;
import org.thanthoai.securesms.utils.Country;
import org.thanthoai.securesms.utils.PhoneNumberConverterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ContactLoader extends SimpleBaseLoader<List<Contact>> {

    public ContactLoader(Context context) {
        super(context);
    }

    @Override
    public List<Contact> loadInBackground() {
        List<Contact> results = new ArrayList<>();
        final Uri uri = Contacts.CONTENT_URI;

        String displayName = Contacts.DISPLAY_NAME;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            displayName = Contacts.DISPLAY_NAME_PRIMARY;
        }
        final String[] projections = new String[]{
                Contacts.HAS_PHONE_NUMBER,
                Contacts.LOOKUP_KEY,
                displayName};
        final String sortOrder = displayName + " ASC";
        Cursor c1 = getContext().getContentResolver().query(
                uri, projections, null, null, sortOrder);

        while (c1.moveToNext()) {
            if (c1.getInt(c1.getColumnIndex(Contacts.HAS_PHONE_NUMBER)) == 0) {
                continue;
            }
            Contact object = new Contact();
            object.DisplayName = c1.getString(c1.getColumnIndex(displayName));

            String lookUpKey = c1.getString(c1.getColumnIndex(Contacts.LOOKUP_KEY));
            Cursor c2 = getContext().getContentResolver().query(
                    CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            CommonDataKinds.Phone.NUMBER,
                            CommonDataKinds.Phone.TYPE
                    },
                    CommonDataKinds.Phone.LOOKUP_KEY + " = ?",
                    new String[] {lookUpKey},
                    null
            );

            while (c2.moveToNext()) {
                String phone = c2.getString(c2.getColumnIndex(CommonDataKinds.Phone.NUMBER));
                phone = PhoneNumberConverterFactory.getConverter(
                        new Locale("vn", Country.VIETNAM)).toLocal(phone);
                int type = c2.getInt(c2.getColumnIndex(CommonDataKinds.Phone.TYPE));
                String typeText = getContext().getResources().getString(
                        CommonDataKinds.Phone.getTypeLabelResource(type));
                if (object.PhoneNumbers.isEmpty()) {
                    object.PrimaryNumber = phone;
                }
                object.PhoneNumbers.put(phone, typeText);
            }
            c2.close();
            if (!object.PhoneNumbers.isEmpty()) {
                results.add(object);
            }
        }
        c1.close();
        setNewData(results);

        return results;
    }
}
