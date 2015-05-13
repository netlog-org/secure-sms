package org.thanthoai.securesms.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class SentMessageModel {

    private static final String TABLE_NAME = "sent_message";

    public static final int STATUS_SENT_SUCCESS = 1;
    public static final int STATUS_SENT_FAIL = 0;

    public long _Id;
    public String Address;
    public String Body;
    public String Date;
    public int Status;

    public static List<SentMessageModel> findByAddress(Context context, String address) {
        return findByAddress(context, address, "date ASC", null);
    }

    public static List<SentMessageModel> findByAddress(Context context, String address,
                                                       String orderBy, String limit) {
        DatabaseHandler db = new DatabaseHandler(context);
        Cursor cursor = db.select(TABLE_NAME,
                new String[]{"_id", "body", "date", "status"},
                "address =?", new String[]{address}, orderBy, limit);
        if (cursor == null) return null;
        List<SentMessageModel> models = new ArrayList<>();
        while (cursor.moveToNext()) {
            SentMessageModel model = new SentMessageModel();
            model._Id = cursor.getInt(cursor.getColumnIndex("_id"));
            model.Address = address;
            model.Body = cursor.getString(cursor.getColumnIndex("body"));
            model.Date = cursor.getString(cursor.getColumnIndex("date"));
            model.Status = cursor.getInt(cursor.getColumnIndex("status"));
            models.add(model);
        }
        cursor.close();
        db.close();
        return models;
    }

    public static boolean deleteByAddress(Context context, String address) {
        DatabaseHandler db = new DatabaseHandler(context);
        return db.delete(TABLE_NAME, "address = ?", new String[]{address});
    }

    public long insert(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        ContentValues values = new ContentValues(4);
        values.put("address", Address);
        values.put("body", Body);
        values.put("date", Date);
        values.put("status", Status);
        return db.insert(TABLE_NAME, values);
    }

    @SuppressWarnings("unused")
    public boolean update(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        ContentValues values = new ContentValues(3);
        values.put("body", Body);
        values.put("date", Date);
        values.put("status", Status);
        return db.update(TABLE_NAME, values, "_id =?", new String[]{String.valueOf(_Id)});
    }

    public boolean delete(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        return db.delete(TABLE_NAME, "_id = ?", new String[]{String.valueOf(_Id)});
    }
}
