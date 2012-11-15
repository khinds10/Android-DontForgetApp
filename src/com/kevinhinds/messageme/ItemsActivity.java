package com.kevinhinds.messageme;

import java.util.Iterator;
import java.util.List;

import com.kevinhinds.messageme.item.Item;
import com.kevinhinds.messageme.item.ItemsDataSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Main Activity for items
 * 
 * @author khinds
 */
public class ItemsActivity extends Activity {

	private ItemsDataSource itemsDataSource;

	private View layout = null;
	private PopupWindow pw;

	private int screenHeight;
	private int screenWidth;

	protected TextView editTextTitle;
	protected TextView messageContent;

	protected long currentID;
	protected String currentTitleValue;
	protected String currentMessageValue;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.archive);

		/** open data connections */
		openDataConnections();

		/** setup the list of items to show the user */
		setupItemsList();

		/** get screen metrics */
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		screenHeight = displaymetrics.heightPixels;
		screenWidth = displaymetrics.widthPixels;

		TextView CurrentMessages = (TextView) findViewById(R.id.CurrentMessages);
		CurrentMessages.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ItemsActivity.this, ItemsActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				startActivity(intent);
			}
		});

		TextView ArchivedMessages = (TextView) findViewById(R.id.ArchivedMessages);
		ArchivedMessages.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ItemsActivity.this, ArchiveActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				startActivity(intent);
			}
		});

	}

	/**
	 * create the list of items on the activity as they currently are saved in the database
	 */
	private void setupItemsList() {

		/** get all the itemlist items saved in the DB */
		final List<Item> itemlist = itemsDataSource.getAllItems();

		/** attach to the LinearLayout to add TextViews dynamically via menuValues */
		LinearLayout ll = (LinearLayout) findViewById(R.id.archiveLayout);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

		/** start with a clean slate */
		ll.removeAllViews();

		/** iterate over the itemlist to build the menu items to show */
		Iterator<Item> itemlistiterator = itemlist.iterator();
		while (itemlistiterator.hasNext()) {
			Item itemlistitem = itemlistiterator.next();
			String itemName = itemlistitem.getName();
			long ID = itemlistitem.getId();
			TextView tv = new TextView(this);
			tv.setId((int) ID);
			tv.setTextSize(18);
			tv.setText((CharSequence) itemName);
			tv.setLayoutParams(lp);
			tv.setClickable(true);
			tv.setTextColor(Color.BLACK);
			tv.setPadding(10, 8, 0, 8);
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_title), null, null, null);
			tv.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					currentID = v.getId();
					Item currentEditItem = itemsDataSource.getById(currentID);
					currentTitleValue = currentEditItem.name;
					currentMessageValue = currentEditItem.content;

					/** popup to edit the item */
					initiateEditMessagePopup(true);
				}
			});
			ll.addView(tv);
		}

		/** add the Go Back text button at the bottom */
		TextView addItemButton = new TextView(this);
		addItemButton.setTextSize(15);
		addItemButton.setText((CharSequence) "Add New");
		addItemButton.setLayoutParams(lp);
		addItemButton.setClickable(true);
		addItemButton.setPadding(10, 10, 10, 10);
		addItemButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.add), null, null, null);
		addItemButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/** popup to add the new item */
				initiateEditMessagePopup(false);
			}
		});
		ll.addView(addItemButton);
	}

	/**
	 * show the popup window for the "set" timer preset
	 */
	private void initiateEditMessagePopup(final boolean editMode) {

		/** adjust the popup WxH */
		float popupWidth = (float) (screenWidth * .90);
		float popupHeight = (float) (screenHeight * .90);

		/** We need to get the instance of the LayoutInflater, use the context of this activity */
		LayoutInflater inflater = (LayoutInflater) ItemsActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		/** Inflate the view from a predefined XML layout */
		layout = inflater.inflate(R.layout.popup_layout, (ViewGroup) findViewById(R.id.popup_element));
		/** create a 300px width and 350px height PopupWindow */
		pw = new PopupWindow(layout, (int) popupWidth, (int) popupHeight, true);
		/** display the popup in the center */
		pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

		editTextTitle = (TextView) layout.findViewById(R.id.editTextTitle);
		messageContent = (TextView) layout.findViewById(R.id.messageContent);
		Button deleteButton = (Button) layout.findViewById(R.id.DeleteButton);
		if (editMode) {
			editTextTitle.setText(currentTitleValue);
			messageContent.setText(currentMessageValue);
			deleteButton.setVisibility(View.VISIBLE);
		} else {
			deleteButton.setVisibility(View.INVISIBLE);
		}

		/** set button is pressed, set the timer and close the popup */
		Button setButton = (Button) layout.findViewById(R.id.SaveButton);
		setButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (editMode) {
					editEntryDB();
				} else {
					addEntryDB();
				}
				pw.dismiss();
			}
		});

		/** cancel button to close */
		Button cancelButton = (Button) layout.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				pw.dismiss();
			}
		});

		/** delete button is pressed, confirm and delete */
		deleteButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(ItemsActivity.this);
				builder.setTitle("Delete Message");
				builder.setMessage("Are you sure you want to delete this message?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						deleteEntryDB();
						dialog.dismiss();
						pw.dismiss();
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				builder.setIcon(R.drawable.ic_launcher);
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
	}

	/**
	 * add entry to DB based on user input
	 */
	private void addEntryDB() {
		/** add edit the value in the DB */
		currentTitleValue = editTextTitle.getText().toString();
		currentMessageValue = messageContent.getText().toString();
		if (currentTitleValue.length() != 0) {
			itemsDataSource.createItem(currentTitleValue, currentMessageValue);
		}
		setupItemsList();
	}

	/**
	 * edit existing item in the DB
	 */
	private void editEntryDB() {
		currentTitleValue = editTextTitle.getText().toString();
		currentMessageValue = messageContent.getText().toString();
		if (currentTitleValue.length() != 0) {
			Item editItem = new Item();
			editItem.setId(currentID);
			editItem.setName(currentTitleValue);
			editItem.setContent(currentMessageValue);
			itemsDataSource.editItem(editItem);
		}
		setupItemsList();
	}

	/**
	 * delete existing item from the DB
	 */
	private void deleteEntryDB() {
		currentTitleValue = editTextTitle.getText().toString();
		currentMessageValue = messageContent.getText().toString();
		if (currentTitleValue.length() != 0) {
			Item deleteItem = new Item();
			deleteItem.setId(currentID);
			deleteItem.setName(currentTitleValue);
			deleteItem.setContent(currentMessageValue);
			itemsDataSource.deleteItem(deleteItem);
		}
		setupItemsList();
	}

	/**
	 * open our data connections to items/itemslist
	 */
	private void openDataConnections() {
		itemsDataSource = new ItemsDataSource(this);
		itemsDataSource.open();
	}
}