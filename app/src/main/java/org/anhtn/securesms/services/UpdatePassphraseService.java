package org.anhtn.securesms.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.anhtn.securesms.R;
import org.anhtn.securesms.crypto.AESHelper;
import org.anhtn.securesms.model.PassphraseModel;
import org.anhtn.securesms.model.SentMessageModel;
import org.anhtn.securesms.utils.Country;
import org.anhtn.securesms.utils.Global;
import org.anhtn.securesms.utils.IPhoneNumberConverter.NotValidPersonalNumberException;
import org.anhtn.securesms.utils.Keys;
import org.anhtn.securesms.utils.PasswordManager;
import org.anhtn.securesms.utils.PhoneNumberConverterFactory;

import java.util.List;
import java.util.Locale;

public class UpdatePassphraseService extends IntentService {

    public static final String UPDATE_PASSPHRASE_DONE = "update.passphrase.done";
    public static final String ACTION_UPDATE_AES_PASSPHRASE = "update.aes.passphrase";
    public static final String ACTION_UPDATE_APP_PASSPHRASE = "update.app.passphrase";

    public UpdatePassphraseService() {
        super("UpdatePassphraseService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        final String action = intent.getAction();
        boolean ok = true;

        if (ACTION_UPDATE_AES_PASSPHRASE.equals(action)) {
            final String address = intent.getStringExtra(Keys.ADDRESS);
            final String oldPassphrase = intent.getStringExtra(Keys.OLD_PASSPHRASE);
            final String newPassphrase = intent.getStringExtra(Keys.NEW_PASSPHRASE);
            final String appPassphrase = intent.getStringExtra(Keys.APP_PASSPHRASE);

            ok = updateAesPassphraseInLocalDb(address, newPassphrase, appPassphrase)
                    && updateMessageInContentResolver(address, oldPassphrase, newPassphrase)
                    && updateMessageInLocalDb(address, oldPassphrase, newPassphrase);
        }
        else if (ACTION_UPDATE_APP_PASSPHRASE.equals(action)) {
            final String oldPassphrase = intent.getStringExtra(Keys.OLD_PASSPHRASE);
            final String newPassphrase = intent.getStringExtra(Keys.NEW_PASSPHRASE);

            List<PassphraseModel> models = PassphraseModel.getAll(this);
            for (PassphraseModel model : models) {
                String plain = AESHelper.decryptFromBase64(oldPassphrase, model.Passphrase);
                model.Passphrase = AESHelper.encryptToBase64(newPassphrase, plain);
                if (!(ok = model.update(this))) {
                    Global.error("Update encryption passphrase in local db error." +
                            ". Passphrase id: " + model._Id +
                            ". Plain text: " + plain);
                    break;
                }
            }
            if (ok) {
                if (newPassphrase.equals(Global.DEFAULT_PASSPHRASE))
                    PasswordManager.removePassword(this);
                else
                    PasswordManager.storePassword(this, newPassphrase);
            }
        }

        Intent i = new Intent(UPDATE_PASSPHRASE_DONE);
        i.putExtra("result", ok);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private boolean updateAesPassphraseInLocalDb(String address, String passphrase,
                                                 String appPassphrase) {
        boolean ok;
        PassphraseModel model = PassphraseModel.findByAddress(this, address);
        if (model != null) {
            model.Passphrase = AESHelper.encryptToBase64(appPassphrase, passphrase);
            ok = model.update(this);
        } else {
            model = new PassphraseModel();
            model.Address = address;
            model.Passphrase = AESHelper.encryptToBase64(appPassphrase, passphrase);
            ok = model.insert(this) != -1;
        }
        return ok;
    }

    private boolean updateMessageInContentResolver(String address, String oldPassphrase,
                                                   String newPassphrase) {
        Uri uri = Uri.parse("content://sms/");
        final String[] projections = new String[]{"_id", "body"};
        if (address == null) return false;
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

        Cursor c = null;
        try {
            c = getContentResolver().query(uri, projections, selection, null, "date ASC");
            while (c.moveToNext()) {
                final String _id = String.valueOf(c.getInt(c.getColumnIndex("_id")));
                String body = c.getString(c.getColumnIndex("body"));
                if (body.startsWith(Global.MESSAGE_PREFIX)) {
                    final String plain = AESHelper.decryptFromBase64(oldPassphrase,
                            body.replace(Global.MESSAGE_PREFIX, ""));
                    if (plain == null) continue;
                    body = Global.MESSAGE_PREFIX + AESHelper.encryptToBase64(
                            newPassphrase, plain);

                    ContentValues values = new ContentValues(1);
                    values.put("body", body);
                    if (getContentResolver().update(uri, values, "_id =?",
                            new String[]{_id}) == -1) {
                        Global.error("Update encrypted message in content resolver error" +
                                ". Message id: " + _id +
                                ". Plain text: " + plain);
                        return false;
                   }
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return true;
    }

    private boolean updateMessageInLocalDb(String address, String oldPassphrase,
                                           String newPassphrase) {
        List<SentMessageModel> models = SentMessageModel.findByAddress(this, address);
        for (SentMessageModel model : models) {
            final String plain = AESHelper.decryptFromBase64(oldPassphrase,
                    model.Body.replace(Global.MESSAGE_PREFIX, ""));
            if (plain == null) continue;
            model.Body = Global.MESSAGE_PREFIX + AESHelper.encryptToBase64(
                    newPassphrase, plain);
            if (!model.update(this)) {
                Global.error("Update encrypted message in local database error." +
                        ". Message id: " + model._Id +
                        ". Plain text: " + plain);
                return false;
            }
        }
        return true;
    }
}
