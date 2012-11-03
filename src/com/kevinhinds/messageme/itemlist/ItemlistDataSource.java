package com.kevinhinds.messageme.itemlist;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * helper dataSource for the 'itemlist' table
 * 
 * @author khinds
 */
public class ItemlistDataSource {

	/** Database fields */
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_NAME };

	/**
	 * contruct ItemslistDataSource
	 * 
	 * @param context
	 */
	public ItemlistDataSource(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public Itemlist createItem(String item) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_NAME, item);
		long insertId = database.insert(MySQLiteHelper.TABLE_ITEM, null, values);
		Cursor cursor = database.query(MySQLiteHelper.TABLE_ITEM, allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Itemlist newItem = cursorToItem(cursor);
		cursor.close();
		return newItem;
	}

	public void deleteItem(Itemlist item) {
		long id = item.getId();
		System.out.println("Item deleted with id: " + id);
		database.delete(MySQLiteHelper.TABLE_ITEM, MySQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	public void deleteItemByName(String name) {
		System.out.println("Item deleted with name: " + name);
		try {
			database.delete(MySQLiteHelper.TABLE_ITEM, MySQLiteHelper.COLUMN_NAME + "=?", new String[] { name });
		} catch (Exception e) {
		}
	}

	public List<Itemlist> getAllItems() {
		List<Itemlist> items = new ArrayList<Itemlist>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_ITEM, allColumns, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Itemlist item = cursorToItem(cursor);
			items.add(item);
			cursor.moveToNext();
		}
		/** Make sure to close the cursor */
		cursor.close();
		return items;
	}

	private Itemlist cursorToItem(Cursor cursor) {
		Itemlist item = new Itemlist();
		item.setId(cursor.getLong(0));
		item.setName(cursor.getString(1));
		return item;
	}
}
