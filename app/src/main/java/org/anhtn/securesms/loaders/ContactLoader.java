package org.anhtn.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;

import org.anhtn.securesms.model.ContactObject;

import java.util.ArrayList;
import java.util.List;


public class ContactLoader extends SimpleBaseLoader<List<ContactObject>> {

    public ContactLoader(Context context) {
        super(context);
    }

    @Override
    public List<ContactObject> loadInBackground() {
        List<ContactObject> results = new ArrayList<>();
        final Uri uri = Contacts.CONTENT_URI;

        final String displayName = (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                ? Contacts.DISPLAY_NAME_PRIMARY 
                : Contacts.DISPLAY_NAME;
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
            ContactObject object = new ContactObject();
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
                String phone = c2.getString(c2.getColumnIndex(
                        CommonDataKinds.Phone.NUMBER)).replace("+84", "0");
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
