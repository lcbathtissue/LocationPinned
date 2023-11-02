package com.aldenocain.geolookup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteCRUD {
    private static final String DATABASE_NAME = "Geocoder.sqlite";
    private static final int DATABASE_VERSION = 1;

    private SQLiteDatabase db;

    public SQLiteCRUD(Context context) {
        DBHelper dbHelper = new DBHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void initTable() {
        db.execSQL("CREATE TABLE IF NOT EXISTS locations (id INTEGER PRIMARY KEY, address TEXT, latitude REAL, longitude REAL)");
    }

    public void clearTable() {
        try {
            db.execSQL("DROP TABLE IF EXISTS locations");
            initTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void create(int id, String address, double latitude, double longitude) {
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("address", address);
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        db.insert("locations", null, values);
    }

    public void update(int id, String address, double latitude, double longitude) {
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("latitude", latitude);
        values.put("longitude", longitude);

        String whereClause = "id=?";
        String[] whereArgs = {String.valueOf(id)};
        db.update("locations", values, whereClause, whereArgs);
    }

    public void delete(int id) {
        String whereClause = "id=?";
        String[] whereArgs = {String.valueOf(id)};
        db.delete("locations", whereClause, whereArgs);
    }

    public String read(int id) {
        String address = null;

        // Query the database to retrieve the address based on the ID
        Cursor cursor = db.query("locations", new String[]{"address"}, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                address = cursor.getString(cursor.getColumnIndex("address"));
            }
            cursor.close();
        }

        return address;
    }

    public void readAll() {
        String[] projection = {"id", "address", "latitude", "longitude"};
        Cursor cursor = db.query("locations", projection, null, null, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex("id"));
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                    double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                }
            } finally {
                cursor.close();
            }
        }
    }

    public Cursor getAlllocations() {
        return db.rawQuery("SELECT * FROM locations", null);
    }

    public Cursor getlocationsByAddress(String address) {
        String[] selectionArgs = {"%" + address + "%"};
        return db.rawQuery("SELECT * FROM locations WHERE address LIKE ?", selectionArgs);
    }

    public int getNextAvailableID() {
        int nextID = 1; // Default starting ID

        Cursor cursor = null;
        try {
            // Query the database to find the maximum ID currently in use
            cursor = db.rawQuery("SELECT MAX(id) FROM locations", null);
            if (cursor.moveToFirst()) {
                nextID = cursor.getInt(0) + 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return nextID;
    }

    private static class DBHelper extends SQLiteOpenHelper {
        DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS locations (id INTEGER PRIMARY KEY, address TEXT, latitude REAL, longitude REAL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // possibly used in the future
        }
    }
}
