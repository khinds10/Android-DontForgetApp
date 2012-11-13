package com.kevinhinds.messageme;

import java.util.Iterator;
import java.util.List;
import com.kevinhinds.messageme.item.ItemsDataSource;
import com.kevinhinds.messageme.itemlist.Itemlist;
import com.kevinhinds.messageme.itemlist.ItemlistDataSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
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
	private ItemlistDataSource itemlistDataSource;

	private View layout = null;
	private PopupWindow pw;

	private String timerTitle;
	private int screenHeight;
	private int screenWidth;

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
		final List<Itemlist> itemlist = itemlistDataSource.getAllItems();

		/** attach to the LinearLayout to add TextViews dynamically via menuValues */
		LinearLayout ll = (LinearLayout) findViewById(R.id.archiveLayout);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

		/** start with a clean slate */
		ll.removeAllViews();

		/** iterate over the itemlist to build the menu items to show */
		Iterator<Itemlist> itemlistiterator = itemlist.iterator();
		int i = 0;
		while (itemlistiterator.hasNext()) {
			i++;
			Itemlist itemlistitem = itemlistiterator.next();
			String itemName = itemlistitem.getName();

			TextView tv = new TextView(this);
			tv.setId(i);
			tv.setTextSize(18);
			tv.setText((CharSequence) itemName);
			tv.setLayoutParams(lp);
			tv.setClickable(true);
			tv.setTextColor(Color.BLACK);
			tv.setPadding(10, 8, 0, 8);
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.remove), null, null, null);
			tv.setOnClickListener(new OnClickListener() {
				/** depending on the item clicked, send it over to the searchActivity with the item clicked as "searchType" */
				public void onClick(View v) {
					TextView tv = (TextView) v;
					String itemValue = (String) tv.getText();

					final TextView itemInQuestion = new TextView(ItemsActivity.this);
					itemInQuestion.setPadding(10, 0, 10, 10);
					itemInQuestion.setText((CharSequence) itemValue);
					itemInQuestion.setTextSize(18);
					new AlertDialog.Builder(ItemsActivity.this).setTitle("Remove Item").setMessage((CharSequence) "Sure you with to remove?").setView(itemInQuestion)
							.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									String enterValue = (String) itemInQuestion.getText();
									if (enterValue.length() != 0) {
										itemlistDataSource.deleteItemByName(enterValue);
										itemsDataSource.deleteItemByName(enterValue);
										ItemsActivity.this.setupItemsList();
									}
								}
							}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
								}
							}).show();
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

				initiateEditMessagePopup();
				// add edit the value in the DB
				// Editable value = input.getText();
				// String enterValue = value.toString();
				// if (enterValue.length() != 0) {
				// itemlistDataSource.createItem(value.toString());
				// ItemsActivity.this.setupItemsList();

			}
		});
		ll.addView(addItemButton);
	}

	/**
	 * show the popup window for the "set" timer preset
	 */
	private void initiateEditMessagePopup() {

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

		/** set button is pressed, set the timer and close the popup */
		Button setButton = (Button) layout.findViewById(R.id.SaveButton);
		setButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// TextView mainTimerCount = (TextView) findViewById(R.id.mainTimerCount);
				// TextView currentTimerName = (TextView) findViewById(R.id.currentTimerName);
				// currentTimerName.setText(timerTitle);
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
	}

	/**
	 * open our data connections to items/itemslist
	 */
	private void openDataConnections() {
		itemsDataSource = new ItemsDataSource(this);
		itemsDataSource.open();
		itemlistDataSource = new ItemlistDataSource(this);
		itemlistDataSource.open();
	}
}
