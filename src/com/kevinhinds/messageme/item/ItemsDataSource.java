package com.kevinhinds.messageme.item;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * helper dataSource for the 'items' table
 * 
 * @author khinds
 */
public class ItemsDataSource {

	/** Database fields */
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_NAME, MySQLiteHelper.COLUMN_CONTENT, MySQLiteHelper.COLUMN_ARCHIVED };

	/**
	 * contruct ItemsDataSource
	 * 
	 * @param context
	 */
	public ItemsDataSource(Context context) {
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
	 * create a new item by associated values
	 * 
	 * @param name
	 * @param content
	 * @return
	 */
	public Item createItem(String name, String content) {

		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_NAME, name);
		values.put(MySQLiteHelper.COLUMN_CONTENT, content);
		values.put(MySQLiteHelper.COLUMN_ARCHIVED, 0);

		long insertId = database.insert(MySQLiteHelper.TABLE_ITEM, null, values);
		Cursor cursor = database.query(MySQLiteHelper.TABLE_ITEM, allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Item newItem = cursorToItem(cursor);
		cursor.close();
		return newItem;
	}

	/**
	 * delete item by identifier
	 * 
	 * @param item
	 */
	public void deleteItem(Item item) {
		long id = item.getId();
		System.out.println("Item deleted with id: " + id);
		database.delete(MySQLiteHelper.TABLE_ITEM, MySQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	/**
	 * delete item by its name
	 * 
	 * @param name
	 */
	public void deleteItemByName(String name) {
		System.out.println("Item deleted with name: " + name);
		try {
			database.delete(MySQLiteHelper.TABLE_ITEM, MySQLiteHelper.COLUMN_NAME + "=?", new String[] { name });
		} catch (Exception e) {
		}
	}

	/**
	 * get item by ID
	 * 
	 * @param id
	 * @return
	 */
	public Item getById(long id) {
		Cursor cursor = database.rawQuery("select * from " + MySQLiteHelper.TABLE_ITEM + " where " + MySQLiteHelper.COLUMN_ID + "='" + id + "'", null);
		cursor.moveToFirst();
		Item item = cursorToItem(cursor);
		return item;
	}

	/**
	 * get all items in a list from datasource
	 * 
	 * @return
	 */
	public List<Item> getAllItems() {
		List<Item> items = new ArrayList<Item>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_ITEM, allColumns, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Item item = cursorToItem(cursor);
			items.add(item);
			cursor.moveToNext();
		}
		/** Make sure to close the cursor */
		cursor.close();
		return items;
	}

	/**
	 * point the datasource cursor to the item in question
	 * 
	 * @param cursor
	 * @return
	 */
	private Item cursorToItem(Cursor cursor) {
		Item item = new Item();
		item.setId(cursor.getLong(0));
		item.setName(cursor.getString(1));
		item.setContent(cursor.getString(2));
		item.setArchived(cursor.getInt(3));
		return item;
	}
}