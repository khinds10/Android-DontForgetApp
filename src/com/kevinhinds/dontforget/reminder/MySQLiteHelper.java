package com.kevinhinds.dontforget.reminder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * custom SQLiteOpenHelper helper related to the 'reminder' table
 * 
 * @author khinds
 */
public class MySQLiteHelper extends SQLiteOpenHelper {

	public static final String TABLE_REMINDER = "reminder";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TIME = "time";

	private static final String DATABASE_NAME = "reminder.db";
	private static final int DATABASE_VERSION = 1;

	/** Database creation SQL statement */
	private static final String DATABASE_CREATE = "create table " + TABLE_REMINDER + "( " + COLUMN_ID + " integer, " + COLUMN_TIME + " text not null);";

	/**
	 * construct the MySQLiteHelper
	 * 
	 * @param context
	 */
	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDER);
		onCreate(db);
	}
}