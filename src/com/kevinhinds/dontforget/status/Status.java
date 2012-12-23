package com.kevinhinds.dontforget.status;

/**
 * Item class related to the SQLiteDB table "status"
 * status is a cooresponding update related to the item i.e. "last emailed to X on XX/YY/ZZZZ" 
 * 
 * @author khinds
 */
public class Status {
	private long id;
	public String content;

	/**
	 * get ID
	 * 
	 * @return
	 */
	public long getId() {
		return id;
	}

	/**
	 * set ID
	 * 
	 * @param id
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * get content
	 * 
	 * @return
	 */
	public String getContent() {
		return content;
	}

	/**
	 * set content
	 * 
	 * @param content
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Will be used by the ArrayAdapter in the ListView
	 */
	@Override
	public String toString() {
		return content;
	}
}