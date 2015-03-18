package org.anhtn.securesms.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.anhtn.securesms.utils.Global;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "secure-sms.db";
    private static final String TABLE_NAME = "passwords";
    private static final String TAG = "SecureSMS-SQLiteEx";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS passwords (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"phone TEXT," +
				"password TEXT);");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS passwords;");
		this.onCreate(db);
	}
	
	public String selectRow(String phoneNumber) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = getReadableDatabase();
			String query = "SELECT phone, password FROM passwords " +
                    "WHERE phone = ?";
			cursor = db.rawQuery(query, new String[] {phoneNumber});
			if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex("password"));
            }
		} catch (SQLiteException ex) {
            Global.error(TAG, ex);
		} finally {
			if (db != null) {
				db.close();
			}
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}

    public boolean updateRow(String phoneNumber, String password) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            ContentValues content = new ContentValues(2);
            content.put("phone", phoneNumber);
            content.put("password", password);
            return db.update(TABLE_NAME, content, "phone = ?",
                    new String[] {phoneNumber}) != -1;
        } catch (SQLiteException ex) {
            Global.error(TAG, ex);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return false;
    }
	
	public boolean insertRow(String phoneNumber, String password) {
        if (selectRow(phoneNumber) != null) {
            return updateRow(phoneNumber, password);
        }

		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			ContentValues content = new ContentValues(2);
			content.put("phone", phoneNumber);
			content.put("password", password);
			return db.insert(TABLE_NAME, null, content) != -1;
		} catch (SQLiteException ex) {
            Global.error(TAG, ex);
		} finally {
			if (db != null) {
				db.close();
			}
		}
		return false;
	}
}
