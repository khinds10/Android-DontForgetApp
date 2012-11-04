package com.kevinhinds.messageme;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import com.kevinhinds.messageme.item.Item;
import com.kevinhinds.messageme.item.ItemsDataSource;
import com.kevinhinds.messageme.itemlist.Itemlist;
import com.kevinhinds.messageme.itemlist.ItemlistDataSource;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * XXX
 * 
 * @author khinds
 */
public class ItemsActivity extends Activity {

	private ItemsDataSource itemsDataSource;
	private ItemlistDataSource itemlistDataSource;
	private TextView tvDisplayDate;
	private Button btnChangeDate;
	private PopupWindow pw;

	private int year;
	private int month;
	private int day;

	private static final int DATE_DIALOG_ID = 999;
	private View layout = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.items);

		/** open data connections */
		openDataConnections();

		/** Code to run once on install */
		checkFirstInstall();

		/** get all the items saved in the DB */
		final List<Item> items = itemsDataSource.getAllItems();

		/** get all the itemlist items saved in the DB */
		final List<Itemlist> itemlist = itemlistDataSource.getAllItems();

		/** attach to the LinearLayout to add TextViews dynamically via menuValues */
		LinearLayout ll = (LinearLayout) findViewById(R.id.itemsLayout);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

		Iterator<Itemlist> itemlistiterator = itemlist.iterator();
		int i = 0;
		while (itemlistiterator.hasNext()) {
			i++;
			Itemlist itemlistitem = itemlistiterator.next();
			String itemName = itemlistitem.getName();

			CheckBox chxbox = new CheckBox(this);
			chxbox.setId(i);
			chxbox.setTextSize(18);
			chxbox.setText((CharSequence) itemName);
			chxbox.setLayoutParams(lp);
			chxbox.setClickable(true);
			chxbox.setTextColor(Color.BLACK);
			chxbox.setChecked(false);

			/** check to see if the items has the menu item already saved and check the box if so */
			Iterator<Item> iterator = items.iterator();
			while (iterator.hasNext()) {
				Item checkItem = iterator.next();
				if (checkItem.name.equals(itemName)) {
					chxbox.setChecked(true);
				}
			}
			chxbox.setOnClickListener(new OnClickListener() {
				/** depending on the item clicked, send it over to the searchActivity with the item clicked as "searchType" */
				public void onClick(View v) {
					CheckBox chxbox = (CheckBox) v;
					String itemValue = (String) chxbox.getText();
					if (chxbox.isChecked()) {
						itemsDataSource.createItem(itemValue, itemValue);
					} else {
						itemsDataSource.deleteItemByName(itemValue);
					}
				}
			});
			ll.addView(chxbox);
		}

		/** add the add/remove text button at the bottom */
		TextView addRemoveOption = new TextView(this);
		addRemoveOption.setTextSize(15);
		addRemoveOption.setText((CharSequence) "Add Message...");
		addRemoveOption.setLayoutParams(lp);
		addRemoveOption.setClickable(true);
		addRemoveOption.setPadding(10, 10, 10, 10);
		addRemoveOption.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				initiatePopupWindow();
			}
		});
		ll.addView(addRemoveOption);

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
	 * code that will run if it's the first time we've installed the application
	 */
	private void checkFirstInstall() {
		/** run the code on install only */
		SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);
		if (wmbPreference.getBoolean("FIRSTRUN", true)) {
			SharedPreferences.Editor editor = wmbPreference.edit();
			editor.putBoolean("FIRSTRUN", false);
			editor.commit();
		}
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

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			/** set date picker as current date */
			return new DatePickerDialog(this, datePickerListener, year, month, day);
		}
		return null;
	}

	private DatePickerDialog.OnDateSetListener datePickerListener = new DatePickerDialog.OnDateSetListener() {

		/** when dialog box is closed, below method will be called. */
		public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
			year = selectedYear;
			month = selectedMonth;
			day = selectedDay;

			GregorianCalendar bday = new GregorianCalendar(year, month, day);
			int dayOfWeek = bday.get(Calendar.DAY_OF_WEEK);
			String dayName = null;

			switch (dayOfWeek) {
			case 1:
				dayName = "Sun";
				break;
			case 2:
				dayName = "Mon";
				break;
			case 3:
				dayName = "Tue";
				break;
			case 4:
				dayName = "Wed";
				break;
			case 5:
				dayName = "Thu";
				break;
			case 6:
				dayName = "Fri";
				break;
			case 7:
				dayName = "Sat";
				break;
			default:
				break;
			}

			tvDisplayDate = (TextView) layout.findViewById(R.id.tvDate);

			/** set current date into textView */
			tvDisplayDate.setText(new StringBuilder().append(dayName + " ")
			/** Month is 0 based so add 1 */
			.append(month + 1).append("/").append(day).append("/").append(year).append(" "));
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		itemsDataSource.open();
		itemlistDataSource.open();
	}

	@Override
	protected void onPause() {
		super.onPause();
		itemsDataSource.close();
		itemlistDataSource.close();
	}

	/**
	 * show the popup window
	 */
	private void initiatePopupWindow() {

		/** get screen metrics */
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int height = displaymetrics.heightPixels;
		int width = displaymetrics.widthPixels;

		/** adjust the popup WxH */
		float popupWidth = (float) (width * .85);
		float popupHeight = (float) (height * .75);
		float popupButtonPadding = (float) (height * .55);

		/** We need to get the instance of the LayoutInflater, use the context of this activity */
		LayoutInflater inflater = (LayoutInflater) ItemsActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		/** Inflate the view from a predefined XML layout */
		layout = inflater.inflate(R.layout.popup_layout, (ViewGroup) findViewById(R.id.popup_element));
		/** create a 300px width and 350px height PopupWindow */
		pw = new PopupWindow(layout, (int) popupWidth, (int) popupHeight, true);
		/** display the popup in the center */
		pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

		/** customize the button position of the popup based on the height determined from the screen resolution */
		LinearLayout buttonContainerLayout = (LinearLayout) layout.findViewById(R.id.buttonContainer);
		buttonContainerLayout.setPadding(0, ((int) popupButtonPadding), 0, 0);

		Button cancelButton = (Button) layout.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(cancel_button_click_listener);

		tvDisplayDate = (TextView) layout.findViewById(R.id.tvDate);

		final Calendar c = Calendar.getInstance();
		year = c.get(Calendar.YEAR);
		month = c.get(Calendar.MONTH);
		day = c.get(Calendar.DAY_OF_MONTH);

		/** set current date into textView */
		tvDisplayDate.setText(new StringBuilder().append(String.format("%ta", c)).append(" ").append(String.format("%tD", c)).append(" "));

		btnChangeDate = (Button) layout.findViewById(R.id.btnChangeDate);
		btnChangeDate.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});
	}

	private OnClickListener cancel_button_click_listener = new OnClickListener() {
		public void onClick(View v) {
			pw.dismiss();
		}
	};
}
