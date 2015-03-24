package org.anhtn.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import org.anhtn.securesms.model.Contact;
import org.anhtn.securesms.model.SentMessageModel;
import org.anhtn.securesms.model.SmsConversation;
import org.anhtn.securesms.utils.CacheHelper;
import org.anhtn.securesms.utils.Country;
import org.anhtn.securesms.utils.PhoneNumberConverterFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SmsConversationLoader extends SimpleBaseLoader<List<SmsConversation>> {

    public SmsConversationLoader(Context context) {
        super(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SmsConversation> loadInBackground() {
        Uri inboxUri = Uri.parse("content://sms/");
        String[] reqCols = new String[] {"address", "body", "date"};
        List<SmsConversation> results = new ArrayList<>();

        Cursor c = getContext().getContentResolver().query(inboxUri, reqCols,
                null, null, "date DESC");
        Set<String> addressSet = new HashSet<>();
        while (c.moveToNext()) {
            String address = c.getString(c.getColumnIndex("address"));
            address = PhoneNumberConverterFactory.getConverter(
                    new Locale("vn", Country.VIETNAM)).toLocal(address);
            final boolean ok = addressSet.add(address);
            if (ok) {
                SmsConversation conversation = new SmsConversation();
                conversation.Address = address;
                conversation.Content = c.getString(c.getColumnIndex("body"));
                conversation.Date = c.getString(c.getColumnIndex("date"));
                try {
                    String number = String.valueOf(Long.parseLong(address));
                    conversation.AddressInContact = phoneLookupFromCache(number);
                    if (conversation.AddressInContact == null) {
                        conversation.AddressInContact = phoneLookup(number);
                    }
                } catch (NumberFormatException ignored) {
                }
                results.add(conversation);
            }
        }
        c.close();

        for (SmsConversation conversation : results) {
            List<SentMessageModel> models = SentMessageModel.findByAddress(getContext(),
                    conversation.Address, "date DESC", "1");
            if (models.isEmpty()) continue;
            final SentMessageModel model = models.get(0);
            if (Long.parseLong(conversation.Date) < Long.parseLong(model.Date)) {
                conversation.Date = model.Date;
                conversation.Content = model.Body;
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
        List<Contact> list = (List<Contact>)
                CacheHelper.getInstance().get("contact");
        for (Contact contact : list) {
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
