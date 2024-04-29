package com.pitechitsolutions.kneumayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DBHelper";
    private static DatabaseHelper instance;


    public static final String TABLE_USERS = "users";
    public static final String NAME = "name";
    public static final String SURNAME = "surname";
    public static final String EMPLOYEE_NUMBER = "employee_number";
    public static final String USER_PIN = "user_pin";
    public static final String IS_CLOCKED_IN = "is_clocked_in";

    // Constructor and getInstance for singleton pattern
    // Constructor and getInstance for singleton pattern
    private DatabaseHelper(Context context) {
        super(context, "timeAttendanceApp.db", null, 2);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating new database tables");
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "surname TEXT," +
                "employee_number TEXT UNIQUE," +
                "user_pin TEXT UNIQUE," +
                "is_clocked_in INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE clock_times (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "clock_in_time TEXT," +
                "clock_out_time TEXT," +
                "photo_path TEXT," +
                "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id))");
        Log.d(TAG, "Tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion < 3) {  // assuming the current version is 2 and needs upgrading
            Log.d(TAG, "Database requires schema update");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS clock_times");
            onCreate(db);
            Log.d(TAG, "Database tables recreated");
        }
    }


    // Implement additional methods including isUserClockedIn
    public boolean isUserClockedIn(String userPin) {
        Log.d(TAG, "Checking if user is clocked in: " + userPin);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{IS_CLOCKED_IN}, "user_pin = ?", new String[]{userPin}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(IS_CLOCKED_IN);
                if (columnIndex != -1) {
                    boolean isClockedIn = cursor.getInt(columnIndex) > 0;
                    Log.d(TAG, "isClockedIn result for user " + userPin + ": " + isClockedIn);
                    cursor.close();
                    return isClockedIn;
                } else {
                    Log.d(TAG, "Column IS_CLOCKED_IN not found.");
                }
            } else {
                Log.d(TAG, "Cursor is empty or not moving to first. User might not exist.");
            }
            cursor.close();
        }
        return false;
    }

    // CRUD operations for users
    public boolean addUser(String name, String surname, String employeeNumber, String userPin) {
        Log.d(TAG, "Adding new user: " + userPin);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("surname", surname);
        values.put("employee_number", employeeNumber);
        values.put("user_pin", userPin);
        long result = db.insert("users", null, values);
        return result != -1;

    }

    public boolean editUser(String name, String surname, String employeeNumber, String userPin) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.NAME, name);
        values.put(DatabaseHelper.SURNAME, surname);
        values.put(DatabaseHelper.EMPLOYEE_NUMBER, employeeNumber);
        // Ensure userPin is used correctly in the WHERE clause
        return db.update(DatabaseHelper.TABLE_USERS, values, DatabaseHelper.USER_PIN + " = ?", new String[]{userPin}) > 0;
    }


    public boolean deleteUser(String userPin) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("users", "user_pin = ?", new String[]{userPin}) > 0;
    }

    public Cursor getUserDetails(String userPin) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("users", new String[]{"id", "name", "surname", "employee_number", "user_pin"}, "user_pin = ?", new String[]{userPin}, null, null, null);
    }

    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("users", new String[]{"id", "name", "surname", "employee_number", "user_pin"}, null, null, null, null, null);
    }

    public boolean validateUserPin(String userPin) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"id"}, "user_pin = ?", new String[]{userPin}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Clocking operations
    // Update existing methods to manage is_clocked_in status
    public boolean recordClockIn(String userPin, String time, String photoPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            long userId = getUserId(userPin, db);
            if (userId == -1) return false;  // User ID not found

            // Insert new clock time record
            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("clock_in_time", time);
            values.put("photo_path", photoPath);
            long insertId = db.insert("clock_times", null, values);
            if (insertId == -1) {
                return false;  // Failed to insert clock in record
            }

            // Update user's clocked in status
            ContentValues userStatusUpdate = new ContentValues();
            userStatusUpdate.put(IS_CLOCKED_IN, 1);
            int updateCount = db.update(TABLE_USERS, userStatusUpdate, "id = ?", new String[]{String.valueOf(userId)});
            if (updateCount != 1) {
                return false;  // Failed to update user status
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<Schedule> getAllSchedules() {
        ArrayList<Schedule> schedules = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("clock_times", new String[]{"user_id", "clock_in_time", "clock_out_time", "photo_path"}, null, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int userId = cursor.getInt(cursor.getColumnIndex("user_id"));
                String userName = findUserNameById(userId);
                String clockInTime = cursor.getString(cursor.getColumnIndex("clock_in_time"));
                String clockOutTime = cursor.getString(cursor.getColumnIndex("clock_out_time"));
                String photoPath = cursor.getString(cursor.getColumnIndex("photo_path"));  // New field

                String date = extractDateFromDateTime(clockInTime);

                Schedule schedule = new Schedule(userName, date, clockInTime, clockOutTime, photoPath);  // Including photoPath
                schedules.add(schedule);
            }
            cursor.close();
        }
        return schedules;
    }
    public ArrayList<Schedule> getSchedulesByDate(String date) {
        ArrayList<Schedule> schedules = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sqlQuery = "SELECT clock_times.*, users.name FROM clock_times JOIN users ON clock_times.user_id = users.id WHERE DATE(clock_in_time) = ?";
        Cursor cursor = db.rawQuery(sqlQuery, new String[]{date});

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String userName = cursor.getString(cursor.getColumnIndex("name"));
                String clockInTime = cursor.getString(cursor.getColumnIndex("clock_in_time"));
                String clockOutTime = cursor.getString(cursor.getColumnIndex("clock_out_time"));
                String photoPath = cursor.getString(cursor.getColumnIndex("photo_path"));  // New field

                String datePart = clockInTime.split(" ")[0];

                Schedule schedule = new Schedule(userName, datePart, clockInTime, clockOutTime, photoPath);  // Including photoPath
                schedules.add(schedule);
            }
            cursor.close();
        }
        return schedules;
    }

    public ArrayList<Schedule> getSchedulesByDateRange(String startDate, String endDate) {
        ArrayList<Schedule> schedules = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT users.name, clock_times.clock_in_time, clock_times.clock_out_time, clock_times.photo_path FROM clock_times JOIN users ON clock_times.user_id = users.id WHERE strftime('%Y-%m-%d', clock_in_time) BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(sql, new String[]{startDate, endDate});

        while (cursor.moveToNext()) {
            String userName = cursor.getString(cursor.getColumnIndex("name"));
            String clockInTime = cursor.getString(cursor.getColumnIndex("clock_in_time"));
            String clockOutTime = cursor.getString(cursor.getColumnIndex("clock_out_time"));
            String photoPath = cursor.getString(cursor.getColumnIndex("photo_path"));  // New field

            schedules.add(new Schedule(userName, extractDateFromDateTime(clockInTime), clockInTime, clockOutTime, photoPath));  // Including photoPath
        }
        cursor.close();
        return schedules;
    }

    private String findUserNameById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"name"}, "id = ?", new String[]{String.valueOf(userId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex("name"));
            cursor.close();
            return name;
        }
        return "Unknown";
    }

    private String extractDateFromDateTime(String dateTime) {
        // Assuming your dateTime format is something like "yyyy-MM-dd HH:mm:ss"
        if (dateTime != null && !dateTime.isEmpty()) {
            return dateTime.split(" ")[0];  // Splits the dateTime string and returns only the date part
        }
        return "";
    }

    public boolean recordClockOut(String userPin, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            long userId = getUserId(userPin, db);
            if (userId == -1) return false;  // User ID not found

            // Update the most recent clock-in record without a clock-out time
            ContentValues values = new ContentValues();
            values.put("clock_out_time", time);
            int updateCount = db.update("clock_times", values, "user_id = ? AND clock_out_time IS NULL", new String[]{String.valueOf(userId)});
            if (updateCount != 1) {
                return false;  // No record updated, might be an error
            }

            // Update user's clocked in status
            ContentValues userStatusUpdate = new ContentValues();
            userStatusUpdate.put(IS_CLOCKED_IN, 0);
            updateCount = db.update(TABLE_USERS, userStatusUpdate, "id = ?", new String[]{String.valueOf(userId)});
            if (updateCount != 1) {
                return false;  // Failed to update user status
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }


    // Update the clock-in record with a photo path
    public boolean updateClockInRecord(String userPin, String photoPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("photo_path", photoPath);

        long userId = getUserId(userPin, db);
        int updatedRows = db.update("clock_times", values, "user_id = ? AND clock_out_time IS NULL", new String[]{String.valueOf(userId)});
        return updatedRows > 0;
    }

    // Utility method
    private long getUserId(String userPin, SQLiteDatabase db) {
        Log.d(TAG, "getUserId called with userPin: " + userPin); // Added debug log
        long userId = -1;
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{"id"}, "user_pin = ?", new String[]{userPin}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("id");
                if (columnIndex != -1) {
                    userId = cursor.getLong(columnIndex);
                } else {
                    Log.d(TAG, "Column 'id' not found in 'users' table.");
                }
            } else {
                Log.d(TAG, "No user found with user_pin: " + userPin);
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get user ID for user_pin: " + userPin, e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return userId;
    }
}