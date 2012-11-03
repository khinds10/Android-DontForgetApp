package com.kevinhinds.messageme.itemlist;

/**
 * Itemlist class related to the SQLiteDB table "itemlist"
 * 
 * @author khinds
 */
public class Itemlist {
	private long id;
	public String name;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Will be used by the ArrayAdapter in the ListView
	 */
	@Override
	public String toString() {
		return name;
	}
}
