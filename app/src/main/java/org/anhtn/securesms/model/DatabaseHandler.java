package org.anhtn.securesms.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import org.anhtn.securesms.utils.Global;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "secure-sms.db";
    private static final String TAG = "SecureSMS-SQLiteEx";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS passphrase (" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"address TEXT," +
				"passphrase TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS sent_message (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "address TEXT," +
                "body TEXT," +
                "date TEXT," +
                "status INTEGER);");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS password;");
        db.execSQL("DROP TABLE IF EXISTS sent_message;");
		this.onCreate(db);
	}
	
	public Cursor select(String table, String[] columns, String selections,
                            String[] selectionArgs, String orderBy, String limit) {
		Cursor cursor = null;
		try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(table, columns, selections, selectionArgs,
                    null, null, orderBy, limit);
		} catch (SQLiteException ex) {
            Global.error(TAG, ex);
		}
		return cursor;
	}

    public long insert(String table, ContentValues values) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            return db.insert(table, null, values);
        } catch (SQLiteException ex) {
            Global.error(TAG, ex);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return -1;
    }

    public boolean update(String table, ContentValues values,
                          String whereClause, String[] whereArgs) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            return db.update(table, values, whereClause, whereArgs) != -1;
        } catch (SQLiteException ex) {
            Global.error(TAG, ex);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return false;
    }
	
    public boolean delete(String table, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            return db.delete(table, whereClause, whereArgs) != -1;
        } catch (SQLException ex) {
            Global.error(TAG, ex);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return false;
    }
}
