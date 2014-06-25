package com.kevinhinds.dontforget.status;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * custom SQLiteOpenHelper helper related to the 'status' table
 * 
 * @author khinds
 */
public class MySQLiteHelper extends SQLiteOpenHelper {

	public static final String TABLE_STATUS = "status";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_CONTENT = "content";

	private static final String DATABASE_NAME = "status.db";
	private static final int DATABASE_VERSION = 1;

	/** Database creation SQL statement */
	private static final String DATABASE_CREATE = "create table " + TABLE_STATUS + "( " + COLUMN_ID + " integer, " + COLUMN_CONTENT + " text not null);";

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
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATUS);
		onCreate(db);
	}
}