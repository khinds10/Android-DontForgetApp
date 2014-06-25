package com.kevinhinds.dontforget.reminder;

/**
 * Reminder class related to the SQLiteDB table "reminder"
 * reminder is a corresponding update related to the item i.e. reminder set to "XX/XX/XXXX XX:XX"  
 * 
 * @author khinds
 */
public class Reminder {
	private long id;
	public String time;

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
	 * get time
	 * 
	 * @return
	 */
	public String getTime() {
		return time;
	}

	/**
	 * set time
	 * 
	 * @param time
	 */
	public void setTime(String time) {
		this.time = time;
	}

	/**
	 * Will be used by the ArrayAdapter in the ListView
	 */
	@Override
	public String toString() {
		return time;
	}
}