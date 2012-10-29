package com.kevinhinds.messageme.item;

/**
 * Item class related to the SQLiteDB table "item"
 * 
 * @author khinds
 */
public class Item {
	private long id;
	public String name;
	public String content;
	private int archived;

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
	 * get name
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * set name
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
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
	 * get archived
	 * 
	 * @return
	 */
	public int getArchived() {
		return archived;
	}

	/**
	 * set archived
	 * 
	 * @param archived
	 */
	public void setArchived(int archived) {
		this.archived = archived;
	}

	/**
	 * Will be used by the ArrayAdapter in the ListView
	 */
	@Override
	public String toString() {
		return name;
	}
}