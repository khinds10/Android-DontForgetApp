package com.kevinhinds.dontforget.status;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * helper dataSource for the 'status' table
 * 
 * @author khinds
 */
public class StatusDataSource {

	/** Database fields */
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_CONTENT };

	/**
	 * contruct StatusDataSource
	 * 
	 * @param context
	 */
	public StatusDataSource(Context context) {
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
	 * create a new status by associated values
	 * 
	 * @param id
	 * @param content
	 * @return
	 */
	public Status createStatus(long id, String content) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_ID, id);
		values.put(MySQLiteHelper.COLUMN_CONTENT, content);
		long insertId = database.insert(MySQLiteHelper.TABLE_STATUS, null, values);
		Cursor cursor = database.query(MySQLiteHelper.TABLE_STATUS, allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Status newStatus = cursorToStatus(cursor);
		cursor.close();
		return newStatus;
	}

	/**
	 * edit status by identifier
	 * 
	 * @param status
	 */
	public void editStatus(Status status) {
		long id = status.getId();
		ContentValues args = new ContentValues();
		args.put(MySQLiteHelper.COLUMN_CONTENT, status.getContent());
		database.update(MySQLiteHelper.TABLE_STATUS, args, MySQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	/**
	 * delete status by identifier
	 * 
	 * @param status
	 */
	public void deleteStatus(Status status) {
		long id = status.getId();
		database.delete(MySQLiteHelper.TABLE_STATUS, MySQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	/**
	 * get status by ID
	 * 
	 * @param id
	 * @return
	 */
	public Status getById(long id) {
		Cursor cursor = database.rawQuery("SELECT * FROM " + MySQLiteHelper.TABLE_STATUS + " WHERE " + MySQLiteHelper.COLUMN_ID + "='" + id + "'", null);
		cursor.moveToFirst();
		Status status = cursorToStatus(cursor);
		return status;
	}

	/**
	 * get all status in a list from datasource
	 * 
	 * @return
	 */
	public List<Status> getAllStatus() {
		List<Status> statuses = new ArrayList<Status>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_STATUS, allColumns, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Status status = cursorToStatus(cursor);
			statuses.add(status);
			cursor.moveToNext();
		}
		/** Make sure to close the cursor */
		cursor.close();
		return statuses;
	}

	/**
	 * point the datasource cursor to the status in question
	 * 
	 * @param cursor
	 * @return
	 */
	private Status cursorToStatus(Cursor cursor) {
		Status status = new Status();
		status.setId(cursor.getLong(0));
		status.setContent(cursor.getString(1));
		return status;
	}
}