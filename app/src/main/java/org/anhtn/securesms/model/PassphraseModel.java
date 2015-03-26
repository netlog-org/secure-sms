package org.anhtn.securesms.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class PassphraseModel {

    private static final String TABLE_NAME = "passphrase";

    public int _Id;
    public String Address;
    public String Passphrase;

    public static List<PassphraseModel> getAll(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        Cursor cursor = db.select(TABLE_NAME, new String[]{"_id", "passphrase"},
                null, null, null, null);
        List<PassphraseModel> models = new ArrayList<>();
        while (cursor.moveToNext()) {
            PassphraseModel model = new PassphraseModel();
            model._Id = cursor.getInt(cursor.getColumnIndex("_id"));
            model.Passphrase = cursor.getString(cursor.getColumnIndex("passphrase"));
            models.add(model);
        }
        return models;
    }

    public static PassphraseModel findByAddress(Context context, String address) {
        DatabaseHandler db = new DatabaseHandler(context);
        Cursor cursor = db.select(TABLE_NAME, new String[]{"_id", "passphrase"},
                "address =?", new String[]{address}, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            PassphraseModel model = new PassphraseModel();
            model._Id = cursor.getInt(cursor.getColumnIndex("_id"));
            model.Address = address;
            model.Passphrase = cursor.getString(cursor.getColumnIndex("passphrase"));
            cursor.close();
            return model;
        }
        return null;
    }

    public long insert(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        ContentValues values = new ContentValues(2);
        values.put("address", Address);
        values.put("passphrase", Passphrase);
        return db.insert(TABLE_NAME, values);
    }

    public boolean update(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        ContentValues values = new ContentValues(1);
        values.put("passphrase", Passphrase);
        return db.update(TABLE_NAME, values, "_id =?", new String[]{String.valueOf(_Id)});
    }

    @SuppressWarnings("unused")
    public boolean delete(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        return db.delete(TABLE_NAME, "_id = ?", new String[]{String.valueOf(_Id)});
    }
}
