package com.kevinhinds.dontforget.reminder;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * helper dataSource for the 'reminder' table
 * 
 * @author khinds
 */
public class ReminderDataSource {

	/** Database fields */
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_TIME };

	/**
	 * construct ReminderDataSource
	 * 
	 * @param context
	 */
	public ReminderDataSource(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	/**
	 * open a connection to the datasource
	 * 
	 * @throws SQLException
	 */
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * close a connection to the datasource
	 */
	public void close() {
		dbHelper.close();
	}

	/**
	 * create a new reminder by associated values
	 * 
	 * @param id
	 * @param time
	 * @return
	 */
	public void createReminder(long id, String time) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_ID, id);
		values.put(MySQLiteHelper.COLUMN_TIME, time);
		database.insert(MySQLiteHelper.TABLE_REMINDER, null, values);
	}

	/**
	 * edit reminder by identifier
	 * 
	 * @param reminder
	 */
	public void editReminder(Reminder reminder) {
		long id = reminder.getId();
		ContentValues args = new ContentValues();
		args.put(MySQLiteHelper.COLUMN_TIME, reminder.getTime());
		database.update(MySQLiteHelper.TABLE_REMINDER, args, MySQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	/**
	 * delete reminder by identifier
	 * 
	 * @param reminder
	 */
	public void deleteReminder(Reminder reminder) {
		long id = reminder.getId();
		database.delete(MySQLiteHelper.TABLE_REMINDER, MySQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	/**
	 * get reminder by ID
	 * 
	 * @param id
	 * @return
	 */
	public Reminder getById(long id) {
		Cursor cursor = database.rawQuery("SELECT * FROM " + MySQLiteHelper.TABLE_REMINDER + " WHERE " + MySQLiteHelper.COLUMN_ID + "='" + id + "'", null);
		try {
			cursor.moveToFirst();
			Reminder reminder = cursorToReminder(cursor);
			return reminder;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * get all reminders in a list from datasource
	 * 
	 * @return
	 */
	public List<Reminder> getAllReminders() {
		List<Reminder> reminders = new ArrayList<Reminder>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_REMINDER, allColumns, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Reminder reminder = cursorToReminder(cursor);
			reminders.add(reminder);
			cursor.moveToNext();
		}
		/** Make sure to close the cursor */
		cursor.close();
		return reminders;
	}

	/**
	 * point the datasource cursor to the reminder in question
	 * 
	 * @param cursor
	 * @return
	 */
	private Reminder cursorToReminder(Cursor cursor) {
		Reminder reminder = new Reminder();
		reminder.setId(cursor.getLong(0));
		reminder.setTime(cursor.getString(1));
		return reminder;
	}
}