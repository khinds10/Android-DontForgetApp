package com.kevinhinds.dontforget;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.kevinhinds.dontforget.alarmmanager.AlarmManagerBroadcastReceiver;
import com.kevinhinds.dontforget.email.GMailSender;
import com.kevinhinds.dontforget.item.Item;
import com.kevinhinds.dontforget.item.ItemsDataSource;
import com.kevinhinds.dontforget.marketplace.MarketPlace;
import com.kevinhinds.dontforget.reminder.Reminder;
import com.kevinhinds.dontforget.reminder.ReminderDataSource;
import com.kevinhinds.dontforget.status.Status;
import com.kevinhinds.dontforget.status.StatusDataSource;
import com.kevinhinds.dontforget.updates.LatestUpdates;
import com.kevinhinds.dontforget.views.GifDecoderView;
import com.kevinhinds.dontforget.widget.CountWidget;
import com.kevinhinds.dontforget.widget.ListWidget;
import com.kevinhinds.dontforget.sound.SoundManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.view.KeyEvent;

/**
 * Main Activity for items
 * 
 * @author khinds
 */
public class ItemsActivity extends Activity {

	private static final int CONTACT_PICKER_RESULT = 1001;
	private ItemsDataSource itemsDataSource;
	private StatusDataSource statusDataSource;
	private ReminderDataSource reminderDataSource;
	private SoundManager mSoundManager;
	ArrayList<Integer> rawIDs;
	ArrayList<String> rawNames;

	private View layout = null;
	private PopupWindow pw;

	private double screenHeight;
	private double screenWidth;

	protected TextView editTextTitle;
	protected TextView messageContent;

	protected long currentID;
	protected String currentTitleValue;
	protected String currentMessageValue;

	private AlarmManagerBroadcastReceiver alarm;

	/**
	 * boolean for if the sounds are turned on or not
	 */
	protected boolean soundsTurnedOn;

	/**
	 * the current options for future reminders can change during the day, keep track of the current
	 * list here
	 */
	private CharSequence[] currentReminderOptions = null;

	/**
	 * save the most recently tried to email / SMS item's ID, so if it didn't go through we can
	 * change the status to reflect as such
	 */
	protected long recentlyTriedItemID;

	/**
	 * save the most recently tried to email / SMS item's edit type either if it was a "add" or
	 * "edit" type of operation to reflect in the status if it fails
	 */
	protected String recentlyTriedEditType;

	protected int isArchivedMessageView;

	protected Status itemStatus;

	protected TextView CurrentMessagesLabel;
	protected TextView ArchivedMessagesLabel;

	protected ProgressDialog progressDialog;

	protected String usersEmail = "";
	protected String usersPhone = "";
	protected String usersPassword = "";
	SharedPreferences wmbPreference;

	boolean emailSent;
	boolean textSent;
	boolean textSentNotify;
	boolean textDeliveredNotify;
	boolean currentEditMode;

	/** friends email and SMS information */
	protected CharSequence[] friendsEmailList;
	protected String friendsEmail;
	protected CharSequence[] friendsSMSList;
	protected String friendsSMS;

	/**
	 * which type of activity for result request wishes, either to email or SMS
	 */
	protected String activityForResultType = "email";

	/**
	 * current 24 hour time values for user reminder preferences without the "AM"/"PM"
	 */
	private int morningTime = 0;
	private int afternoonTime = 0;
	private int eveningTime = 0;

	/** the font for the buttons */
	public String buttonFont = "fonts/Muro.otf";

	/** font for the titles */
	public String titleFont = "fonts/talldark.ttf";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.items);

		/** if first install then need some default options created */
		checkFirstInstall();

		/** check if the user wants to have app sounds enabled or not */
		SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);
		if (wmbPreference.getBoolean("FIRSTINSTALL", true)) {
			soundsTurnedOn = true;
			SharedPreferences.Editor editor = wmbPreference.edit();
			editor.putBoolean("FIRSTINSTALL", false);
			editor.putBoolean("soundsTurnedOn", soundsTurnedOn);
			editor.commit();
		} else {
			soundsTurnedOn = wmbPreference.getBoolean("soundsTurnedOn", true);
		}
		setSilentModeMessage();

		/** get the current settings for the user */
		getUserSettings();

		CurrentMessagesLabel = (TextView) findViewById(R.id.CurrentMessages);
		ArchivedMessagesLabel = (TextView) findViewById(R.id.ArchivedMessages);
		CurrentMessagesLabel.setPadding(16, 16, 38, 16);
		ArchivedMessagesLabel.setPadding(16, 16, 38, 16);

		/** non-archived messages by default */
		isArchivedMessageView = 0;

		/** open data connections */
		openDataConnections();

		/** setup the list of items to show the user */
		setupItemsList();

		/** title font to localDate */
		TextView localDateTextView = (TextView) findViewById(R.id.localDate);
		localDateTextView.setTypeface(Typeface.createFromAsset(this.getAssets(), titleFont));
		SimpleDateFormat localDateDF = new SimpleDateFormat("HmzZ", Locale.getDefault());
		String localDateValue = localDateDF.format(Calendar.getInstance().getTime());
		localDateTextView.setText(localDateValue);

		/** local time font */
		TextView localTime = (TextView) findViewById(R.id.localTime);
		localTime.setTypeface(Typeface.createFromAsset(this.getAssets(), titleFont));
		SimpleDateFormat localTimeDF = new SimpleDateFormat("D,F,W d.w", Locale.getDefault());
		String localTimeValue = localTimeDF.format(Calendar.getInstance().getTime());
		localTime.setText("Planetary Stats " + localTimeValue + " ");

		/** set the stardate below */
		TextView starDate = (TextView) findViewById(R.id.starDate);
		starDate.setText("Stardate " + getStarDate());
		starDate.setTypeface(Typeface.createFromAsset(this.getAssets(), titleFont));

		/** apply font to add new button */
		TextView addNewButtonText = (TextView) findViewById(R.id.addNewButtonText);
		addNewButtonText.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		/** if you click the current messages option */
		CurrentMessagesLabel.setTypeface(null, Typeface.BOLD);
		CurrentMessagesLabel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				soundEvent("click_current_messages");
				isArchivedMessageView = 0;
				CurrentMessagesLabel.setBackgroundDrawable(getResources().getDrawable(R.drawable.lg_orange_button));
				CurrentMessagesLabel.setTypeface(null, Typeface.BOLD);
				ArchivedMessagesLabel.setBackgroundDrawable(getResources().getDrawable(R.drawable.extrlg_orange_button));
				ArchivedMessagesLabel.setTypeface(null, Typeface.NORMAL);
				CurrentMessagesLabel.setPadding(16, 16, 38, 16);
				ArchivedMessagesLabel.setPadding(16, 16, 38, 16);
				setupItemsList();
			}
		});

		/** if you click the archive messages option */
		ArchivedMessagesLabel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				soundEvent("click_archived_messages");
				isArchivedMessageView = 1;
				CurrentMessagesLabel.setBackgroundDrawable(getResources().getDrawable(R.drawable.extrlg_orange_button));
				CurrentMessagesLabel.setTypeface(null, Typeface.NORMAL);
				ArchivedMessagesLabel.setBackgroundDrawable(getResources().getDrawable(R.drawable.lg_orange_button));
				ArchivedMessagesLabel.setTypeface(null, Typeface.BOLD);
				CurrentMessagesLabel.setPadding(16, 16, 38, 16);
				ArchivedMessagesLabel.setPadding(16, 16, 38, 16);
				setupItemsList();
			}
		});

		/** if you click the add new messages button */
		LinearLayout addNewButton = (LinearLayout) findViewById(R.id.addNewButton);
		addNewButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/** popup to add the new item */
				soundEvent("click_add_new");
				initiateEditMessagePopup(false);
			}
		});

		/** get the display metrics */
		getDisplayMetrics();

		/** create the line animated GIF */
		InputStream stream = null;
		stream = getResources().openRawResource(R.drawable.line);
		GifDecoderView staticView = new GifDecoderView(this, stream);
		RelativeLayout.LayoutParams lhw = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		staticView.setLayoutParams(lhw);
		staticView.setPadding(0, 0, 0, 0);
		RelativeLayout lineGifBox = (RelativeLayout) findViewById(R.id.lineGifBox);
		lineGifBox.addView(staticView);

		/** create the blue alert animated gif */
		InputStream streamAlert = null;
		streamAlert = getResources().openRawResource(R.drawable.blue);
		GifDecoderView staticViewAlert = new GifDecoderView(this, streamAlert);
		RelativeLayout alertGif = (RelativeLayout) findViewById(R.id.alertGif);
		alertGif.addView(staticViewAlert);

		/**
		 * setup the AlarmManagerBroadcastReceiver for the ability to set an alarm item in the
		 * future
		 */
		alarm = new AlarmManagerBroadcastReceiver();

		/** display the sounds list through a different thread */
		LoadSoundsTask displaySoundsTask = new LoadSoundsTask();
		displaySoundsTask.execute(new String[] { "" });

		/**
		 * show the latest update notes if the application was just installed
		 */
		LatestUpdates.showFirstInstalledNotes(this);

	}

	/**
	 * get the raw sounds and assets via background thread
	 */
	private class LoadSoundsTask extends AsyncTask<String, Void, String[]> {
		@Override
		protected String[] doInBackground(String... params) {
			/** get the raw assets and setup the sound */
			getRawAssets();
			setupSounds();
			return params;
		}

		@Override
		protected void onPostExecute(String result[]) {
			/** play the open app sound */
			soundEvent("openapp");
		}
	}

	/**
	 * code that will run if it's the first time we've installed the application
	 */
	private void checkFirstInstall() {
		/** run the code on install only */
		SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);
		if (wmbPreference.getBoolean("FIRSTRUN", true)) {
			SharedPreferences.Editor editor = wmbPreference.edit();
			editor.putString("MORNING", "9 AM");
			editor.putString("AFTERNOON", "2 PM");
			editor.putString("EVENING", "6 PM");
			editor.putBoolean("FIRSTRUN", false);
			editor.commit();
		}
	}

	/**
	 * create the list of items on the activity as they currently are saved in the database
	 */
	private void setupItemsList() {

		getDisplayMetrics();

		/**
		 * the message count text fields should reflect how many archived / non-archived messages
		 */
		CurrentMessagesLabel.setText("STATUS [" + Integer.toString(itemsDataSource.getCountItemsbyArchiveType(0)) + "]");
		ArchivedMessagesLabel.setText("STANDBY [" + Integer.toString(itemsDataSource.getCountItemsbyArchiveType(1)) + "]");

		/**
		 * get all the itemlist items saved in the DB set to archived = false
		 */
		final List<Item> itemlist = itemsDataSource.getAllItemsbyArchiveType(isArchivedMessageView);

		/**
		 * attach to the LinearLayout to add TextViews dynamically via current user message values
		 */
		LinearLayout itemsLayout = (LinearLayout) findViewById(R.id.itemsLayout);

		/** create the layout parameters with the margins applied */
		LinearLayout.LayoutParams textFillContent = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		textFillContent.setMargins(6, 0, 0, 0);
		LinearLayout.LayoutParams textWrapContent = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
		textWrapContent.setMargins(6, 0, 0, 0);
		LinearLayout.LayoutParams itemContainerFillContent = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 12);
		LinearLayout.LayoutParams itemTextContainer = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 60);

		/** swap item colors based on archived list or not */
		Drawable squareColor1;
		Drawable squareColor2;
		Drawable squareColor3;

		if (isArchivedMessageView == 0) {
			squareColor1 = getResources().getDrawable(R.drawable.grey_square);
			squareColor2 = getResources().getDrawable(R.drawable.md_grey_square);
			squareColor3 = getResources().getDrawable(R.drawable.lg_grey_square);
		} else {
			squareColor1 = getResources().getDrawable(R.drawable.dk_grey_square);
			squareColor2 = getResources().getDrawable(R.drawable.md_grey_square);
			squareColor3 = getResources().getDrawable(R.drawable.grey_square);
		}

		/** start with a clean slate */
		itemsLayout.removeAllViews();

		/** iterate over the itemlist to build the menu items to show */
		Iterator<Item> itemlistiterator = itemlist.iterator();
		while (itemlistiterator.hasNext()) {
			Item itemlistitem = itemlistiterator.next();
			String itemName = itemlistitem.getName();
			long ID = itemlistitem.getId();

			/**
			 * create the linear layout for the item itself that's horizontal and assign the click
			 */
			LinearLayout itemMainLL = new LinearLayout(this);
			itemMainLL.setId((int) ID);
			itemMainLL.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					soundEvent("click_view_item");
					currentID = v.getId();
					Item currentEditItem = itemsDataSource.getById(currentID);
					currentTitleValue = currentEditItem.name;
					currentMessageValue = currentEditItem.content;
					/** popup to edit the item */
					initiateEditMessagePopup(true);
				}
			});
			itemMainLL.setOrientation(LinearLayout.HORIZONTAL);
			itemMainLL.setLayoutParams(itemTextContainer);

			/**
			 * create the smaller layout for the bar to fit inside
			 */
			LinearLayout itemLL = new LinearLayout(this);
			itemLL.setId((int) ID);
			itemLL.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					soundEvent("click_view_item");
					currentID = v.getId();
					Item currentEditItem = itemsDataSource.getById(currentID);
					currentTitleValue = currentEditItem.name;
					currentMessageValue = currentEditItem.content;
					/** popup to edit the item */
					initiateEditMessagePopup(true);
				}
			});
			itemLL.setOrientation(LinearLayout.HORIZONTAL);
			itemLL.setLayoutParams(itemContainerFillContent);
			itemLL.setPadding(4, 4, 4, 4);

			/** create the actual message text */
			TextView itemText = new TextView(this);
			itemText.setTextSize(22);
			itemText.setText((CharSequence) itemName);
			itemText.setLayoutParams(textWrapContent);
			itemText.setPadding(10, 20, 0, 0);
			itemText.setMaxLines(1);
			itemText.setWidth((int) (screenWidth * 0.5));
			if (isArchivedMessageView == 0) {
				itemText.setTextColor(Color.parseColor("#FFFFFF"));
			} else {
				itemText.setTextColor(Color.parseColor("#999999"));
			}
			itemText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			itemText.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
			itemMainLL.addView(itemText);

			/**
			 * get status of the item to populate the other elements of the row: X,MM.d,H:m,INFO
			 */
			Status Status = statusDataSource.getById(ID);
			String currentStatusDetails = Status.content;
			String[] statusElements = currentStatusDetails.split(",");
			String rowDate = "";
			try {
				rowDate = statusElements[1];
			} catch (IndexOutOfBoundsException e) {
				rowDate = "";
			}
			String rowChar = "";
			try {
				rowChar = statusElements[0];
			} catch (IndexOutOfBoundsException e) {
				rowChar = "";
			}
			String rowDateString = "";
			try {
				rowDateString = statusElements[2];
			} catch (IndexOutOfBoundsException e) {
				rowDateString = "";
			}
			/** create the status message text */
			TextView itemStatusText = new TextView(this);
			itemStatusText.setTextSize(12);
			itemStatusText.setText(rowChar + " - " + rowDate + " - " + rowDateString);
			itemStatusText.setLayoutParams(textWrapContent);
			itemStatusText.setMaxLines(1);
			if (isArchivedMessageView == 0) {
				itemStatusText.setTextColor(Color.parseColor("#CC9966"));
			} else {
				itemStatusText.setTextColor(Color.parseColor("#999999"));
			}
			itemStatusText.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			itemStatusText.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
			itemStatusText.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
			itemMainLL.addView(itemStatusText);

			/** create the right hand box */
			TextView dateBox = new TextView(this);
			dateBox.setLayoutParams(textWrapContent);
			dateBox.setWidth(30);
			dateBox.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL | Gravity.RIGHT);
			dateBox.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
			dateBox.setBackgroundDrawable(squareColor1);
			itemLL.addView(dateBox);

			/** create box next to message */
			TextView rightMemoSquare = new TextView(this);
			rightMemoSquare.setLayoutParams(textWrapContent);
			rightMemoSquare.setWidth(10);
			rightMemoSquare.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			rightMemoSquare.setBackgroundDrawable(squareColor3);
			itemLL.addView(rightMemoSquare);

			/** create status item box */
			TextView rightMemoStatusBox = new TextView(this);
			rightMemoStatusBox.setLayoutParams(textWrapContent);
			rightMemoStatusBox.setWidth(20);
			rightMemoStatusBox.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			rightMemoStatusBox.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
			rightMemoStatusBox.setBackgroundDrawable(squareColor2);
			itemLL.addView(rightMemoStatusBox);

			/** set the fill parent box with the time */
			TextView rightMemoSquareTimeBox = new TextView(this);
			rightMemoSquareTimeBox.setLayoutParams(textFillContent);
			rightMemoSquareTimeBox.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL | Gravity.RIGHT);
			rightMemoSquareTimeBox.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
			rightMemoSquareTimeBox.setBackgroundDrawable(squareColor1);
			itemLL.addView(rightMemoSquareTimeBox);
			itemsLayout.addView(itemMainLL);
			itemsLayout.addView(itemLL);

			/**
			 * show the possible reminder of the item by the current ID being processed
			 */
			Reminder reminder = reminderDataSource.getById(ID);
			if (reminder != null) {
				TextView textReminder = new TextView(this);
				textReminder.setTextSize(10);
				textReminder.setText("REMINDER - " + (CharSequence) reminder.time.toUpperCase());
				textReminder.setLayoutParams(textFillContent);
				textReminder.setTextColor(Color.parseColor("#FF9966"));
				textReminder.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL | Gravity.RIGHT);
				itemsLayout.addView(textReminder);
			}
		}

		/** update the external widgets with any changes that have happened */
		updateWidgets();
	}

	/**
	 * check if a current reminder exists for a certain item identifier
	 * 
	 * @param id
	 * @return
	 */
	private Reminder checkReminderEntry(long id) {
		return reminderDataSource.getById(id);
	}

	/**
	 * show the popup window for the editing of don't forget messages
	 */
	private void initiateEditMessagePopup(final boolean editMode) {

		/**
		 * keep the most recent value of if we're editing global for more involved workflows below
		 */
		currentEditMode = editMode;

		/** get any recent changes to the user settings */
		getUserSettings();

		/** adjust the popup WxH */
		getDisplayMetrics();
		float popupWidth = (float) (screenWidth * .90);
		float popupHeight = (float) (screenHeight * .90);

		/**
		 * We need to get the instance of the LayoutInflater, use the context of this activity
		 */
		LayoutInflater inflater = (LayoutInflater) ItemsActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		/** Inflate the view from a predefined XML layout */
		layout = inflater.inflate(R.layout.popup_layout, (ViewGroup) findViewById(R.id.popup_element));
		/** create a 300px width and 350px height PopupWindow */
		pw = new PopupWindow(layout, (int) popupWidth, (int) popupHeight, true);
		/** display the popup in the center */
		pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

		TextView manualInputText = (TextView) layout.findViewById(R.id.manualInputText);
		manualInputText.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		TextView textViewTitle = (TextView) layout.findViewById(R.id.textViewTitle);
		textViewTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		TextView messageTitle = (TextView) layout.findViewById(R.id.messageTitle);
		messageTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		TextView processMessageTitle = (TextView) layout.findViewById(R.id.processMessageTitle);
		processMessageTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		TextView quickEmailTitle = (TextView) layout.findViewById(R.id.quickEmailTitle);
		quickEmailTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		TextView quickTextTitle = (TextView) layout.findViewById(R.id.quickTextTitle);
		quickTextTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		TextView popupTitle = (TextView) layout.findViewById(R.id.popupTitle);
		popupTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		editTextTitle = (TextView) layout.findViewById(R.id.editTextTitle);
		messageContent = (TextView) layout.findViewById(R.id.messageContent);

		TextView deleteButton = (TextView) layout.findViewById(R.id.DeleteButton);
		deleteButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		TextView archiveButton = (TextView) layout.findViewById(R.id.ArchiveButton);
		archiveButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		final TextView readOnlyTitletextView = (TextView) layout.findViewById(R.id.readOnlyTitletextView);
		readOnlyTitletextView.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		final TextView readOnlyMessagetextView = (TextView) layout.findViewById(R.id.readOnlyMessagetextView);
		readOnlyMessagetextView.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		/** edit existing memo buttons and container */
		final LinearLayout editButtonContainer = (LinearLayout) layout.findViewById(R.id.editButtonContainer);

		final TextView editMemoButton = (TextView) layout.findViewById(R.id.editMemoButton);
		editMemoButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		final TextView cancelEditMemoButton = (TextView) layout.findViewById(R.id.cancelEditMemoButton);
		cancelEditMemoButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		/** reminder reminder elements */
		final TextView cancelReminderButton = (TextView) layout.findViewById(R.id.cancelReminderButton);
		cancelReminderButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));

		final LinearLayout reminderLayout = (LinearLayout) layout.findViewById(R.id.reminderReminder);
		final TextView reminderReminderInfo = (TextView) layout.findViewById(R.id.reminderReminderInfo);

		/**
		 * if we're editing an existing entry the other buttons are enabled, else you can't use them
		 * yet
		 */
		if (editMode) {

			deleteButton.setVisibility(View.VISIBLE);
			archiveButton.setVisibility(View.VISIBLE);

			editButtonContainer.setVisibility(View.VISIBLE);
			editMemoButton.setVisibility(View.VISIBLE);
			cancelEditMemoButton.setVisibility(View.GONE);

			readOnlyTitletextView.setVisibility(View.VISIBLE);
			readOnlyMessagetextView.setVisibility(View.VISIBLE);
			readOnlyTitletextView.setText(currentTitleValue);
			readOnlyMessagetextView.setText(currentMessageValue);

			editTextTitle.setVisibility(View.GONE);
			messageContent.setVisibility(View.GONE);
			editTextTitle.setText(currentTitleValue);
			messageContent.setText(currentMessageValue);

			final Reminder currentReminder = checkReminderEntry(currentID);
			if (currentReminder != null) {
				reminderLayout.setVisibility(View.VISIBLE);
				reminderReminderInfo.setText("REMINDER - " + currentReminder.getTime().toUpperCase());
			}
			cancelReminderButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					soundEvent("click_cancel_timer");
					reminderDataSource.deleteReminder(currentReminder);
					cancelReminderButton.setVisibility(View.GONE);
					reminderReminderInfo.setText("REMINDER CANCELLED");
					alarm.cancelReminder(getBaseContext(), editTextTitle.getText().toString(), messageContent.getText().toString(), currentID);
					setupItemsList();
				}
			});
		} else {

			deleteButton.setVisibility(View.GONE);
			archiveButton.setVisibility(View.GONE);

			editButtonContainer.setVisibility(View.GONE);
			editMemoButton.setVisibility(View.GONE);
			cancelEditMemoButton.setVisibility(View.GONE);

			readOnlyTitletextView.setVisibility(View.GONE);
			readOnlyMessagetextView.setVisibility(View.GONE);

			editTextTitle.setVisibility(View.VISIBLE);
			messageContent.setVisibility(View.VISIBLE);
			editTextTitle.requestFocus();
		}

		/** edit button is pressed, change the popup to be able to edit */
		editMemoButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				soundEvent("click_edit_message");
				readOnlyTitletextView.setVisibility(View.GONE);
				readOnlyMessagetextView.setVisibility(View.GONE);
				editTextTitle.setVisibility(View.VISIBLE);
				messageContent.setVisibility(View.VISIBLE);
				editMemoButton.setVisibility(View.GONE);
				cancelEditMemoButton.setVisibility(View.VISIBLE);
				editTextTitle.setText(currentTitleValue);
				messageContent.setText(currentMessageValue);
				editTextTitle.requestFocus();
			}
		});

		/**
		 * cancel edit button is pressed, change the popup to be able to edit
		 */
		cancelEditMemoButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				soundEvent("click_cancel_edit_message");
				readOnlyTitletextView.setVisibility(View.VISIBLE);
				readOnlyMessagetextView.setVisibility(View.VISIBLE);
				editTextTitle.setVisibility(View.GONE);
				messageContent.setVisibility(View.GONE);
				editMemoButton.setVisibility(View.VISIBLE);
				cancelEditMemoButton.setVisibility(View.GONE);
				editTextTitle.setText(currentTitleValue);
				messageContent.setText(currentMessageValue);
			}
		});

		/**
		 * set the button text to Archived/Un-Archived by what you're able to do
		 */
		if (isArchivedMessageView == 1) {
			archiveButton.setText("Un-Archive");
		} else {
			archiveButton.setText("Archive");
		}

		/** set button is pressed, set the timer and close the popup */
		TextView setButton = (TextView) layout.findViewById(R.id.SaveButton);
		setButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		setButton.setOnClickListener(new OnClickListener() {
			String statusDate = getLastUpdateTime();

			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					soundEvent("error_no_title_save");
					showEmptyTitleMessage();
				} else {
					soundEvent("click_save_message");
					if (editMode) {
						recentlyTriedItemID = editEntryDB("E," + statusDate);
						recentlyTriedEditType = "edit";
					} else {
						recentlyTriedItemID = addEntryDB("A," + statusDate);
						recentlyTriedEditType = "add";
					}
					pw.dismiss();
				}
			}
		});

		/** set button is pressed, set the timer and close the popup */
		archiveButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		archiveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					soundEvent("error_no_title_archive");
					showEmptyTitleMessage();
				} else {
					/** set archive / un-archive by user request */
					if (isArchivedMessageView == 1) {
						soundEvent("click_archive_message");
						archiveOptionEntryDB(0);
					} else {
						soundEvent("click_unarchive_message");
						archiveOptionEntryDB(1);
					}
					pw.dismiss();
				}
			}
		});

		/** cancel button to close */
		TextView cancelButton = (TextView) layout.findViewById(R.id.cancelButton);
		cancelButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				soundEvent("click_cancel_button");
				pw.dismiss();
			}
		});

		/** delete button is pressed, confirm and delete */
		deleteButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				soundEvent("click_delete_button");
				AlertDialog.Builder builder = new AlertDialog.Builder(ItemsActivity.this);
				builder.setTitle("Delete Message");
				builder.setMessage("Are you sure you want to delete this message?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						soundEvent("click_confirm_delete");
						deleteReminderEntry(currentID);
						deleteEntryDB();
						dialog.dismiss();
						pw.dismiss();
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						soundEvent("click_cancel_delete");
						dialog.cancel();
					}
				});
				builder.setIcon(R.drawable.ic_launcher);
				AlertDialog alert = builder.create();
				alert.show();
			}
		});

		/** email yourself button */
		TextView sendButton = (TextView) layout.findViewById(R.id.sendEmailButton);
		sendButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		sendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					soundEvent("error_no_title_send");
					showEmptyTitleMessage();
				} else {
					soundEvent("sending_email_yourself");
					progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending Email", "Emailing...\n" + usersEmail);
					String statusValue = "M," + getLastUpdateTime();
					if (editMode) {
						recentlyTriedItemID = editEntryDB(statusValue);
						recentlyTriedEditType = "edit";
					} else {
						recentlyTriedItemID = addEntryDB(statusValue);
						recentlyTriedEditType = "add";
					}
					itemStatus = getStatus();
					EmailUserTask task = new EmailUserTask();
					task.execute(new String[] { usersEmail });
					pw.dismiss();
					setupItemsList();
				}
			}
		});

		/** email friend button */
		TextView sendContactEmailButton = (TextView) layout.findViewById(R.id.sendContactEmailButton);
		sendContactEmailButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		sendContactEmailButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					soundEvent("error_no_title_contact_email");
					showEmptyTitleMessage();
				} else {
					/** select a friends email to send a reminder to */
					soundEvent("find_contact_email");
					activityForResultType = "email";
					Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
					startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
				}
			}
		});

		/** SMS friend button */
		TextView sendContactSMSButton = (TextView) layout.findViewById(R.id.sendContactSMSButton);
		sendContactSMSButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		sendContactSMSButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					soundEvent("error_no_title_contact_sms");
					showEmptyTitleMessage();
				} else {
					/** select a friends email to send a reminder to */
					soundEvent("find_contact_sms");
					activityForResultType = "SMS";
					Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
					startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
				}
			}
		});

		/** SMS yourself button */
		TextView sendSMSButton = (TextView) layout.findViewById(R.id.sendSMSButton);
		sendSMSButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		sendSMSButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					soundEvent("error_no_title_send_sms");
					showEmptyTitleMessage();
				} else {
					soundEvent("sending_sms_yourself");
					progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending SMS Message", "Texting...\n" + usersPhone);
					String statusValue = "T," + getLastUpdateTime();
					if (editMode) {
						recentlyTriedItemID = editEntryDB(statusValue);
						recentlyTriedEditType = "edit";
					} else {
						recentlyTriedItemID = addEntryDB(statusValue);
						recentlyTriedEditType = "add";
					}
					itemStatus = getStatus();
					SMSUserTask task = new SMSUserTask();
					task.execute(new String[] { usersPhone });
					pw.dismiss();
					setupItemsList();
				}
			}
		});

		/** remind me later button */
		TextView RemindButton = (TextView) layout.findViewById(R.id.RemindButton);
		RemindButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		RemindButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					soundEvent("error_no_title_remind");
					showEmptyTitleMessage();
				} else {
					soundEvent("click_remind");
					String statusDate = getLastUpdateTime();
					if (editMode) {
						currentID = editEntryDB("E," + statusDate);
						recentlyTriedEditType = "edit";
					} else {
						currentID = addEntryDB("A, " + statusDate);
						recentlyTriedEditType = "add";
					}
					AlertDialog.Builder alert = new AlertDialog.Builder(ItemsActivity.this);
					alert.setTitle("Choose Reminder Time:");
					alert.setIcon(R.drawable.ic_launcher);
					/** setup the list of choices with click events */
					alert.setSingleChoiceItems(getReminderOptions(), -1, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							/**
							 * based on the dialog selection either set the reminder or continue to
							 * show the custom dialog if they chose "custom"
							 */
							String selectedItem = (String) currentReminderOptions[item];
							if (selectedItem.equals("Custom...")) {
								soundEvent("event_choose_reminder_time_custom_select");
								final Dialog customDialog = new Dialog(ItemsActivity.this);
								customDialog.setContentView(R.layout.custom_time_dialog);
								customDialog.setTitle("Choose Custom Reminder:");
								customDialog.show();
								TextView cancelCustomReminderButton = (TextView) customDialog.findViewById(R.id.cancelCustomReminderButton);
								cancelCustomReminderButton.setOnClickListener(new OnClickListener() {
									public void onClick(View v) {
										customDialog.dismiss();
									}
								});
								TextView setCustomReminderButton = (TextView) customDialog.findViewById(R.id.setCustomReminderButton);
								setCustomReminderButton.setOnClickListener(new OnClickListener() {
									public void onClick(View v) {

										/**
										 * get custom date and time selected
										 */
										DatePicker customDatePicker = (DatePicker) customDialog.findViewById(R.id.customDatePicker);
										TimePicker customTimePicker = (TimePicker) customDialog.findViewById(R.id.customTimePicker);
										Date currentTime = new Date();
										Date customDate = new Date(customDatePicker.getYear() - 1900, customDatePicker.getMonth(), customDatePicker.getDayOfMonth(), customTimePicker.getCurrentHour(),
												customTimePicker.getCurrentMinute());

										/**
										 * get the diff of the future custom time against the
										 * current time to set the future reminder
										 */
										long diff = customDate.getTime() - currentTime.getTime();
										setReminder(diff);
										customDialog.dismiss();
									}
								});

								dialog.dismiss();
							} else {
								/**
								 * get the amount of time until the future selected reminder
								 * datetime
								 */
								soundEvent("event_choose_reminder_time_standard");
								long diff = getFutureTime(currentReminderOptions[item]);
								setReminder(diff);
								dialog.dismiss();
							}
						}
					});
					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog ad = alert.create();
					ad.show();
					pw.dismiss();
				}
			}
		});
	}

	/**
	 * set reminder for current time plus the diff to time in the future
	 * 
	 * @param diff
	 */
	@SuppressLint("SimpleDateFormat")
	private void setReminder(long diff) {

		/**
		 * get the future date as a string to save as status for the item, also
		 */
		long futureDateTime = System.currentTimeMillis() + diff;
		Date futureDate = new Date(futureDateTime);
		DateFormat todaysDateFormat = new SimpleDateFormat("EEE, d MMM h:mm a");
		String todaysDate = todaysDateFormat.format(futureDate);
		currentID = editEntryDB("E," + getLastUpdateTime());

		/**
		 * set the reminder for this item, and add/update the flag in the reminder DB that it's been
		 * added/updated
		 */
		alarm.setReminder(getBaseContext(), editTextTitle.getText().toString(), messageContent.getText().toString(), futureDateTime, currentID);
		deleteReminderEntry(currentID);
		addReminderEntry(currentID, todaysDate);
	}

	/**
	 * remove the reminder entry and setup the items list with the new situation
	 * 
	 * @param recentlyTriedItemID
	 */
	private void deleteReminderEntry(long recentlyTriedItemID) {
		try {
			Reminder reminder = new Reminder();
			reminder.setId(recentlyTriedItemID);
			reminderDataSource.deleteReminder(reminder);
			setupItemsList();
		} catch (Exception e) {
		}
	}

	/**
	 * add the existing reminder entry table for this particular item "id" and setup the items list
	 * with the new situation
	 * 
	 * @param recentlyTriedItemID
	 * @param todaysDate
	 */
	private void addReminderEntry(long recentlyTriedItemID, String todaysDate) {
		reminderDataSource.createReminder(recentlyTriedItemID, todaysDate);
		setupItemsList();
	}

	/**
	 * get the reminder options as CharSequence[] to pass to the dialog as choice items list
	 * 
	 * @example get the reminder options but only the ones that haven't transpired i.e. you can't
	 *          set "This Morning" if it's already passed the time that "This Morning" is supposed
	 *          to remind you at
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	private CharSequence[] getReminderOptions() {

		SimpleDateFormat sdf = new SimpleDateFormat("H");
		String currentDateandTime = sdf.format(new Date());

		/** get preference 24 hour time values without the "AM"/"PM" */
		getTwentyFourHourTimeForPreferences();

		TypedArray reminderOptions = getBaseContext().getResources().obtainTypedArray(R.array.array_reminder_options);
		int len = reminderOptions.length();
		CharSequence[] items = new CharSequence[len];
		int finalItemsCount = 0;
		for (int i = 0; i < len; i++) {
			if (reminderOptions.getString(i).equals("This Morning")) {
				if (Integer.parseInt(currentDateandTime) < morningTime) {
					items[i] = reminderOptions.getString(i);
					finalItemsCount++;
				} else {
					items[i] = "NOT IN FUTURE";
				}
			} else if (reminderOptions.getString(i).equals("This Afternoon")) {
				if (Integer.parseInt(currentDateandTime) < afternoonTime) {
					items[i] = reminderOptions.getString(i);
					finalItemsCount++;
				} else {
					items[i] = "NOT IN FUTURE";
				}
			} else if (reminderOptions.getString(i).equals("This Evening")) {
				if (Integer.parseInt(currentDateandTime) < eveningTime) {
					items[i] = reminderOptions.getString(i);
					finalItemsCount++;
				} else {
					items[i] = "NOT IN FUTURE";
				}
			} else {
				items[i] = reminderOptions.getString(i);
				finalItemsCount++;
			}
		}
		reminderOptions.recycle();
		finalItemsCount++;

		CharSequence[] finalItems = new CharSequence[finalItemsCount];
		currentReminderOptions = new CharSequence[finalItemsCount];
		int finalItemsIteratorCount = 0;
		for (int i = 0; i < len; i++) {
			String checkedItemValue = (String) items[i];
			if (!checkedItemValue.contains("NOT IN FUTURE")) {
				finalItems[finalItemsIteratorCount] = items[i];
				currentReminderOptions[finalItemsIteratorCount] = items[i];
				finalItemsIteratorCount = finalItemsIteratorCount + 1;
			}
		}
		String customOption = "Custom...";
		finalItems[finalItemsIteratorCount] = customOption;
		currentReminderOptions[finalItemsIteratorCount] = customOption;
		return finalItems;
	}

	/**
	 * get the 24 hour time for all three user reminder preferences
	 */
	private void getTwentyFourHourTimeForPreferences() {

		String morning = wmbPreference.getString("MORNING", "9 AM");
		morning = parseSpecialHours(morning);

		String afternoon = wmbPreference.getString("AFTERNOON", "2 PM");
		afternoon = parseSpecialHours(afternoon);

		String evening = wmbPreference.getString("EVENING", "6 PM");
		evening = parseSpecialHours(evening);

		boolean isMorningPM = false;
		if (morning.contains("PM")) {
			isMorningPM = true;
		}
		morning = morning.replace("AM", "").replace("PM", "").trim();
		if (isMorningPM) {
			morningTime = Integer.parseInt(morning) + 12;
		} else {
			morningTime = Integer.parseInt(morning);
		}

		boolean isAfternoonPM = false;
		if (afternoon.contains("PM")) {
			isAfternoonPM = true;
		}
		afternoon = afternoon.replace("AM", "").replace("PM", "").trim();
		if (isAfternoonPM) {
			afternoonTime = Integer.parseInt(afternoon) + 12;
		} else {
			afternoonTime = Integer.parseInt(afternoon);
		}

		boolean isEveningPM = false;
		if (evening.contains("PM")) {
			isEveningPM = true;
		}
		evening = evening.replace("AM", "").replace("PM", "").trim();
		if (isEveningPM) {
			eveningTime = Integer.parseInt(evening) + 12;
		} else {
			eveningTime = Integer.parseInt(evening);
		}
	}

	/**
	 * parse the special cases for string "hours" which may have a "noon" or "midnight" instead of
	 * "AM" / "PM"
	 * 
	 * @param hour
	 * @return
	 */
	private String parseSpecialHours(String hour) {
		if (hour.equals("12 Noon")) {
			hour = "12 PM";
		}
		if (hour.equals("12 Midnight")) {
			hour = "12 AM";
		}
		return hour;
	}

	/**
	 * show the dialog to confirm sending the SMS
	 */
	private void sendSMSConfirm() {
		AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
		alertDialog.setTitle("Send Reminder to Friend");
		alertDialog.setMessage("Send text to contact's number -> " + friendsSMS);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				soundEvent("click_sms_contact_confirm");
				progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending SMS Text Message", "Texting contact's primary number: \n" + friendsSMS);
				String statusValue = "T," + getLastUpdateTime() + "," + friendsSMS;
				if (currentEditMode) {
					recentlyTriedItemID = editEntryDB(statusValue);
					recentlyTriedEditType = "edit";
				} else {
					recentlyTriedItemID = addEntryDB(statusValue);
					recentlyTriedEditType = "add";
				}
				itemStatus = getStatus();
				SMSUserTask task = new SMSUserTask();
				task.execute(new String[] { friendsSMS });
				pw.dismiss();
				setupItemsList();
				dialog.dismiss();
			}
		});
		alertDialog.setButton2("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				soundEvent("click_sms_contact_cancel");
				dialog.dismiss();
			}
		});
		alertDialog.setIcon(R.drawable.ic_launcher);
		alertDialog.show();
	}

	/**
	 * show the dialog to confirm sending the email to contact
	 */
	private void showEmailConfirm() {
		AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
		alertDialog.setTitle("Send Reminder to Friend");
		alertDialog.setMessage("Send Message to -> " + friendsEmail);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				soundEvent("click_email_contact_confirm");
				progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending Email", "Emailing...\n" + friendsEmail);
				String statusValue = "M," + getLastUpdateTime() + "," + friendsEmail;
				if (currentEditMode) {
					recentlyTriedItemID = editEntryDB(statusValue);
					recentlyTriedEditType = "edit";
				} else {
					recentlyTriedItemID = addEntryDB(statusValue);
					recentlyTriedEditType = "add";
				}
				itemStatus = getStatus();
				EmailUserTask task = new EmailUserTask();
				task.execute(new String[] { friendsEmail });
				pw.dismiss();
				setupItemsList();
				dialog.dismiss();
			}
		});
		alertDialog.setButton2("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				soundEvent("click_email_contact_cancel");
				dialog.dismiss();
			}
		});
		alertDialog.setIcon(R.drawable.ic_launcher);
		alertDialog.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		/**
		 * values to get for contact to continue with the activity for result
		 */
		friendsEmail = "";
		friendsSMS = "";
		String id = "";

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				try {

					/**
					 * get email for contact as a list for options for the user
					 */
					Uri result = data.getData();
					id = result.getLastPathSegment();

					/** get count of total number of email addresses */
					Cursor cursor = getContentResolver().query(Email.CONTENT_URI, null, Email.CONTACT_ID + "=?", new String[] { id }, null);
					int emailAddressCount = 0;
					while (cursor.moveToNext()) {
						emailAddressCount++;
					}
					cursor.close();

					/** setup the list of email addresses */
					friendsEmailList = new CharSequence[emailAddressCount];
					Cursor cursor2 = getContentResolver().query(Email.CONTENT_URI, null, Email.CONTACT_ID + "=?", new String[] { id }, null);
					int emailIdx = cursor2.getColumnIndex(Email.DATA);
					int emailCountLoop = 0;
					while (cursor2.moveToNext()) {
						String email = cursor2.getString(emailIdx);
						if (email.length() != 0) {
							friendsEmailList[emailCountLoop] = email;
						}
						emailCountLoop++;
					}
					cursor2.close();

					/** get the count of SMS phone number(s) for contact */
					Cursor pCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id },
							null);
					int phoneNumberCount = 0;
					while (pCur.moveToNext()) {
						phoneNumberCount++;
					}
					pCur.close();

					/**
					 * create the String[] list of phone numbers for the contact
					 */
					friendsSMSList = new CharSequence[phoneNumberCount];
					int isPrimaryNumber = 0;
					Cursor pCur2 = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
							new String[] { id }, null);
					int phoneNumberCountLoop = 0;
					while (pCur2.moveToNext()) {
						isPrimaryNumber = pCur2.getInt(pCur2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY));
						String phoneNumber = pCur2.getString(pCur2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
						if (isPrimaryNumber == 1) {
							friendsSMSList[phoneNumberCountLoop] = phoneNumber + " [primary]";
						} else {
							friendsSMSList[phoneNumberCountLoop] = phoneNumber;
						}
						phoneNumberCountLoop++;
					}
					pCur2.close();

				} catch (Exception e) {
				}
				break;
			}
		}

		/**
		 * remove the duplicate values from the array that may have come back from the contact
		 * selection
		 */
		friendsSMSList = getDistinct(friendsSMSList);
		friendsEmailList = getDistinct(friendsEmailList);

		/**
		 * based on recent activityForResultType requested, send an SMS or Email respectively
		 */
		if (activityForResultType.equals("SMS")) {

			/**
			 * if we have an primary phone number for our friend, then send it out to them, else
			 * show the error
			 */
			if (friendsSMSList.length != 0) {
				if (friendsSMSList.length == 1) {
					friendsSMS = (String) friendsSMSList[0];
					friendsSMS = friendsSMS.replace(" [primary]", "");
					sendSMSConfirm();
				} else {
					AlertDialog.Builder chooseAlert = new AlertDialog.Builder(ItemsActivity.this);
					chooseAlert.setTitle("Choose Number:");
					chooseAlert.setIcon(R.drawable.ic_launcher);
					/** setup the list of choices with click events */
					chooseAlert.setSingleChoiceItems(friendsSMSList, -1, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							/**
							 * friends SMS becomes what was selected from the dialog removing the
							 * message about if the phone number is primary
							 */
							soundEvent("click_choose_which_number_sms");
							friendsSMS = (String) friendsSMSList[item];
							friendsSMS = friendsSMS.replace(" [primary]", "");
							sendSMSConfirm();
							dialog.dismiss();
						}
					});
					/** show the choose phone number dialog */
					AlertDialog ad = chooseAlert.create();
					ad.show();
				}
			} else {
				AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
				alertDialog.setTitle("Send Reminder to Friend");
				alertDialog.setMessage("Phone number couldn't be located for contact");
				soundEvent("event_no_phone_number_found_sms");

				/**
				 * Update status to reflect that the entry was simply "edited" or "added" it
				 * couldn't be sent via SMS
				 */
				addEditItemStatus();

				alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						soundEvent("event_no_phone_number_found_sms_confirm");
						dialog.dismiss();
					}
				});
				alertDialog.setIcon(R.drawable.ic_launcher);
				alertDialog.show();
			}
		} else {

			/**
			 * if we have an email for our friend, then send it out to them, else show the error
			 */
			if (friendsEmailList.length != 0) {
				if (friendsEmailList.length == 1) {
					friendsEmail = (String) friendsEmailList[0];
					showEmailConfirm();
				} else {
					AlertDialog.Builder chooseEmailAlert = new AlertDialog.Builder(ItemsActivity.this);
					chooseEmailAlert.setTitle("Choose Email:");
					chooseEmailAlert.setIcon(R.drawable.ic_launcher);
					/** setup the list of choices with click events */
					chooseEmailAlert.setSingleChoiceItems(friendsEmailList, -1, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							/**
							 * friends email becomes what was selected from the dialog
							 */
							soundEvent("click_choose_which_email");
							friendsEmail = (String) friendsEmailList[item];
							showEmailConfirm();
							dialog.dismiss();
						}
					});
					/** show the choose phone number dialog */
					AlertDialog ad = chooseEmailAlert.create();
					ad.show();
				}
			} else {
				AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
				alertDialog.setTitle("Send Reminder to Friend");
				alertDialog.setMessage("Email address couldn't be located for contact");
				soundEvent("event_email_not_found_contact");

				/**
				 * Update status to reflect that the entry was simply "edited" or "added" it
				 * couldn't be sent via email
				 */
				addEditItemStatus();

				alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						soundEvent("event_email_not_found_contact_confirm");
						dialog.dismiss();
					}
				});
				alertDialog.setIcon(R.drawable.ic_launcher);
				alertDialog.show();
			}
		}
	}

	/**
	 * for a given array of CharSequence, remove duplicate values
	 * 
	 * @param input
	 * @return
	 */
	protected static CharSequence[] getDistinct(CharSequence[] input) {
		Set<CharSequence> distinct = new HashSet<CharSequence>();
		for (CharSequence element : input) {
			distinct.add(element);
		}
		return distinct.toArray(new CharSequence[0]);
	}

	/**
	 * get a nicely formated datetime string with pipe separated date and time values to later
	 * render in the item rows formatted to: MM.d|H:m
	 * 
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	private String getLastUpdateTime() {
		Date currentTime = new Date();
		DateFormat todaysDateFormat = new SimpleDateFormat("MM.d,H.m");
		return todaysDateFormat.format(currentTime);
	}

	/**
	 * get the most recent settings for the user
	 */
	private void getUserSettings() {

		/** get the sharedPreferences to edit via user's request */
		wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);
		usersEmail = wmbPreference.getString("USER_EMAIL", "");
		usersPhone = wmbPreference.getString("USER_PHONE", "");
		usersPassword = wmbPreference.getString("USER_PASSWORD", "");

		/** show the user the most current email settings */
		TextView userGmail = (TextView) findViewById(R.id.userGmail);
		userGmail.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		String displayEmail = "";
		if (usersEmail.equals("")) {
			userGmail.setTextColor(Color.WHITE);
			displayEmail = "(press to configure)";
		} else {
			displayEmail = usersEmail;
		}
		userGmail.setText(displayEmail);

		/** show the user the most current phone number */
		TextView userPhoneNumber = (TextView) findViewById(R.id.userPhoneNumber);
		userPhoneNumber.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		String displayPhone = "";
		if (usersPhone.equals("")) {
			userPhoneNumber.setTextColor(Color.WHITE);
			displayPhone = "(press to configure)";
		} else {
			displayPhone = usersPhone;
		}
		userPhoneNumber.setText(displayPhone);

		/** click the layout to edit settings */
		LinearLayout subContentAccountSettings = (LinearLayout) findViewById(R.id.subContentAccountSettings);
		subContentAccountSettings.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				soundEvent("click_edit_settings");
				Intent intent = new Intent(ItemsActivity.this, SettingsActivity.class);
				startActivity(intent);
			}
		});
	}

	/**
	 * archive existing item in the DB
	 */
	private void archiveOptionEntryDB(int archiveOption) {
		currentTitleValue = editTextTitle.getText().toString();
		currentMessageValue = messageContent.getText().toString();
		if (currentTitleValue.length() != 0) {
			Item archiveItem = new Item();
			archiveItem.setId(currentID);
			archiveItem.setName(currentTitleValue);
			archiveItem.setContent(currentMessageValue);
			itemsDataSource.archiveItembyOption(archiveItem, archiveOption);
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
		statusDataSource = new StatusDataSource(this);
		reminderDataSource = new ReminderDataSource(this);
		reminderDataSource.open();
		itemsDataSource.open();
		statusDataSource.open();
	}

	/**
	 * sends an SMS message to another device
	 * 
	 * @throws Exception
	 */
	private void sendSMS(String phoneNumber, String message) throws Exception {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";
		textSentNotify = false;
		textDeliveredNotify = false;

		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
		PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

		/** when the SMS has been sent */
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					if (!textSentNotify) {
						Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
						textSentNotify = true;
					}
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					textSent = false;
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					textSent = false;
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					textSent = false;
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					textSent = false;
					break;
				}
			}
		}, new IntentFilter(SENT));

		/** when the SMS has been delivered */
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					if (!textDeliveredNotify) {
						Toast.makeText(getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
						textDeliveredNotify = true;
					}
					break;
				case Activity.RESULT_CANCELED:
					textSent = false;
					break;
				}
			}
		}, new IntentFilter(DELIVERED));

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
		if (!textSent) {
			throw new Exception();
		}
	}

	private class SMSUserTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			String recipient = params[0];
			textSent = true;
			try {
				String currentTitle = editTextTitle.getText().toString();
				String currentMessage = messageContent.getText().toString();
				sendSMS(recipient, currentTitle + "\n" + currentMessage + "\n\n--\nDon't Forget! for Android");
			} catch (Exception e) {
				textSent = false;
			}
			return textSent;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			super.onPostExecute(success);
			progressDialog.dismiss();
			if (!success) {
				AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
				alertDialog.setTitle("SMS could not be sent.");
				alertDialog.setMessage("Check your settings and try again.");

				/**
				 * Update status to reflect that the entry was simply "edited" or "added" it
				 * couldn't be sent via SMS
				 */
				itemStatus.setId(recentlyTriedItemID);
				String statusDate = getLastUpdateTime();
				if (recentlyTriedEditType.equals("edit")) {
					itemStatus.setContent("E," + statusDate);
				} else {
					itemStatus.setContent("A," + statusDate);
				}
				statusDataSource.editStatus(itemStatus);
				setupItemsList();

				alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				alertDialog.setButton2("Settings...", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(ItemsActivity.this, SettingsActivity.class);
						startActivity(intent);
					}
				});
				alertDialog.setIcon(R.drawable.ic_launcher);
				alertDialog.show();
			}
		}
	}

	private class EmailUserTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			String recipient = params[0];
			emailSent = true;
			try {
				String currentTitle = editTextTitle.getText().toString();
				String currentMessage = messageContent.getText().toString();
				GMailSender sender = new GMailSender(usersEmail, usersPassword);
				sender.sendMail(currentTitle + "   (Captain's Log for Android)", currentMessage + "\n\n--\nCaptain's Log for Android", usersEmail, recipient);
			} catch (Exception e) {
				emailSent = false;
			}
			return emailSent;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			super.onPostExecute(success);
			progressDialog.dismiss();
			if (!success) {
				AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
				alertDialog.setTitle("Email could not be sent.");
				alertDialog.setMessage("Check your settings and try again.");

				/**
				 * Update status to reflect that the entry was simply "edited" or "added" it
				 * couldn't be sent via Email
				 */
				itemStatus.setId(recentlyTriedItemID);
				String statusDate = getLastUpdateTime();
				if (recentlyTriedEditType.equals("edit")) {
					itemStatus.setContent("E," + statusDate);
				} else {
					itemStatus.setContent("A," + statusDate);
				}
				statusDataSource.editStatus(itemStatus);
				setupItemsList();

				alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				alertDialog.setButton2("Settings...", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(ItemsActivity.this, SettingsActivity.class);
						startActivity(intent);
					}
				});
				alertDialog.setIcon(R.drawable.ic_launcher);
				alertDialog.show();
			} else {
				Toast.makeText(getBaseContext(), "Email Sent", Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * update any currently running widgets with the latest and greatest from this activity that was
	 * loaded
	 */
	private void updateWidgets() {

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

		/** update the count widget */
		RemoteViews countViews = new RemoteViews(this.getPackageName(), R.layout.count_widget);
		countViews.setTextViewText(R.id.messageCount, "[" + Integer.toString(itemsDataSource.getCountItemsbyArchiveType(0)) + "] Messages");
		countViews.setTextViewText(R.id.archiveCount, "[" + Integer.toString(itemsDataSource.getCountItemsbyArchiveType(1)) + "] Archived");
		appWidgetManager.updateAppWidget(new ComponentName(this.getPackageName(), CountWidget.class.getName()), countViews);

		/** update the list widget */
		RemoteViews listViews = new RemoteViews(this.getPackageName(), R.layout.list_widget);
		final List<Item> itemlist = itemsDataSource.getAllItemsbyArchiveType(0);
		Iterator<Item> itemlistiterator = itemlist.iterator();
		int count = 0;
		String previousName = "";
		listViews.setTextViewText(R.id.recentItem1, "");
		listViews.setTextViewText(R.id.recentItem2, "");
		listViews.setTextViewText(R.id.recentItem3, "");
		listViews.setTextViewText(R.id.recentItem4, "");
		while (itemlistiterator.hasNext()) {
			count++;
			Item itemlistitem = itemlistiterator.next();
			String itemName = itemlistitem.getName();
			if (!previousName.equals(itemName)) {
				if (count == 1) {
					listViews.setTextViewText(R.id.recentItem1, itemName);
				} else if (count == 2) {
					listViews.setTextViewText(R.id.recentItem2, itemName);
				} else if (count == 3) {
					listViews.setTextViewText(R.id.recentItem3, itemName);
				} else if (count == 4) {
					listViews.setTextViewText(R.id.recentItem4, itemName);
				}
			}
			previousName = itemName;
		}
		appWidgetManager.updateAppWidget(new ComponentName(this.getPackageName(), ListWidget.class.getName()), listViews);
	}

	/**
	 * check if the textView for item title is empty or not
	 * 
	 * @param editTextTitle
	 * @return
	 */
	protected boolean isTitleEmpty(TextView editTextTitle) {
		if (editTextTitle == null || editTextTitle.length() == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * show the missing title dialog to the user
	 */
	protected void showEmptyTitleMessage() {
		AlertDialog.Builder builder = new AlertDialog.Builder(ItemsActivity.this);
		builder.setTitle("Please enter a title");
		builder.setMessage("You must enter a title for your reminder item.").setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		builder.setIcon(R.drawable.ic_launcher);
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * get a new instance of a Status object for use
	 * 
	 * @return Status
	 */
	protected Status getStatus() {
		Status status = new Status();
		return status;
	}

	/**
	 * get the current amount of seconds until the future reminder time selected
	 * 
	 * @param currentReminderOption
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	private long getFutureTime(CharSequence currentReminderOption) {

		/** get current datetime and format for today in time */
		Date currentTime = new Date();
		DateFormat todaysDateFormat = new SimpleDateFormat("yyyy/MM/dd");
		String todaysDate = todaysDateFormat.format(currentTime);

		/** get preference 24 hour time values without the "AM"/"PM" */
		getTwentyFourHourTimeForPreferences();

		/**
		 * build datetime string out of the current date plus the hour in the future the reminder is
		 * scheduled for
		 */
		String reminderDateString = "";
		if (currentReminderOption.equals("This Morning") || currentReminderOption.equals("Tomorrow Morning")) {
			reminderDateString = todaysDate + " " + Integer.toString(morningTime) + ":00:00";
		} else if (currentReminderOption.equals("This Afternoon") || currentReminderOption.equals("Tomorrow Afternoon")) {
			reminderDateString = todaysDate + " " + Integer.toString(afternoonTime) + ":00:00";
		} else if (currentReminderOption.equals("This Evening") || currentReminderOption.equals("Tomorrow Evening")) {
			reminderDateString = todaysDate + " " + Integer.toString(eveningTime) + ":00:00";
		}

		/**
		 * convert the reminder time in the future to milleseconds for time diff
		 */
		Date reminderTime = null;
		try {
			DateFormat completedDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			reminderTime = completedDateFormat.parse(reminderDateString);
		} catch (ParseException e) {
		}
		long reminderTimeMilleseconds = reminderTime.getTime();

		/**
		 * if it's tomorrow we need to add 24 hours of time in milleseconds to it
		 */
		if (currentReminderOption.equals("Tomorrow Morning") || currentReminderOption.equals("Tomorrow Afternoon") || currentReminderOption.equals("Tomorrow Evening")) {
			reminderTimeMilleseconds = reminderTimeMilleseconds + 86400000;
		}
		return reminderTimeMilleseconds - currentTime.getTime();
	}

	/**
	 * get all the raw assets sound files in the raw folder
	 */
	private void getRawAssets() {
		rawIDs = new ArrayList<Integer>();
		rawNames = new ArrayList<String>();
		Field[] fields = R.raw.class.getFields();
		for (Field f : fields)
			try {
				rawIDs.add(f.getInt(null));
				rawNames.add(f.getName());
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			}
	}

	/**
	 * setup the new soundmanager with the context being "this"
	 */
	private void setupSounds() {
		mSoundManager = new SoundManager();
		mSoundManager.initSounds(this);
		for (int i = 0; i < rawIDs.size(); i++) {
			mSoundManager.addSound(i, rawIDs.get(i));
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		soundEvent("keypress");
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * capture a screen event to then map the correct sound to play
	 * 
	 * @param eventName
	 */
	private void soundEvent(String eventName) {

		/** don't play sounds if they're not enabled by user settings */
		if (!soundsTurnedOn) {
			return;
		}

		// archive.wav
		// beep.wav
		// beep2.wav
		// call.wav
		// cancel.wav
		// commit.wav
		// confirm.wav
		// error.wav
		// hail.wav
		// keypress.wav
		// open.wav
		// ping.wav
		// processing.wav
		// save.wav
		// thinking.wav
		// unarchive.wav

		if (eventName.equals("keypress")) {
			playSound("keypress");
		}
		if (eventName.equals("openapp")) {
			playSound("openapp");
		}
		if (eventName.equals("error_no_title_save")) {
			playSound("error");
		}
		if (eventName.equals("click_current_messages")) {
			playSound("left");
		}
		if (eventName.equals("click_archived_messages")) {
			playSound("right");
		}
		if (eventName.equals("click_add_new")) {
			playSound("open");
		}
		if (eventName.equals("click_view_item")) {
			playSound("open");
		}
		if (eventName.equals("click_cancel_timer")) {
			playSound("canel");
		}
		if (eventName.equals("click_edit_message")) {
			playSound("beepbeep");
		}
		if (eventName.equals("click_cancel_edit_message")) {
			playSound("beep2");
		}
		if (eventName.equals("click_save_message")) {
			playSound("commit");
		}
		if (eventName.equals("error_no_title_archive")) {
			playSound("error");
		}
		if (eventName.equals("click_archive_message")) {
			playSound("archive");
		}
		if (eventName.equals("click_unarchive_message")) {
			playSound("unarchive");
		}
		if (eventName.equals("click_cancel_button")) {
			playSound("cancel");
		}
		if (eventName.equals("click_delete_button")) {
			playSound("beep");
		}
		if (eventName.equals("click_confirm_delete")) {
			playSound("processing");
		}
		if (eventName.equals("click_cancel_delete")) {
			playSound("beep");
		}
		if (eventName.equals("error_no_title_send")) {
			playSound("error");
		}
		if (eventName.equals("sending_email_yourself")) {
			playSound("sending");
		}
		if (eventName.equals("error_no_title_contact_email")) {
			playSound("error");
		}
		if (eventName.equals("find_contact_email")) {
			playSound("chirp");
		}
		if (eventName.equals("error_no_title_contact_sms")) {
			playSound("error");
		}
		if (eventName.equals("find_contact_sms")) {
			playSound("chirp");
		}
		if (eventName.equals("error_no_title_send_sms")) {
			playSound("error");
		}
		if (eventName.equals("sending_sms_yourself")) {
			playSound("sending");
		}
		if (eventName.equals("error_no_title_remind")) {
			playSound("error");
		}
		if (eventName.equals("click_remind")) {
			playSound("hail");
		}
		if (eventName.equals("event_choose_reminder_time_custom_select")) {
			playSound("beep");
		}
		if (eventName.equals("event_choose_reminder_time_standard")) {
			playSound("call");
		}
		if (eventName.equals("click_sms_contact_confirm")) {
			playSound("sending");
		}
		if (eventName.equals("click_sms_contact_cancel")) {
			playSound("beep");
		}
		if (eventName.equals("click_email_contact_confirm")) {
			playSound("sending");
		}
		if (eventName.equals("click_email_contact_cancel")) {
			playSound("beep");
		}
		if (eventName.equals("click_choose_which_number_sms")) {
			playSound("beep");
		}
		if (eventName.equals("event_no_phone_number_found_sms")) {
			playSound("thinking");
		}
		if (eventName.equals("event_no_phone_number_found_sms_confirm")) {
			playSound("beep");
		}
		if (eventName.equals("click_choose_which_email")) {
			playSound("beep");
		}
		if (eventName.equals("event_email_not_found_contact")) {
			playSound("thinking");
		}
		if (eventName.equals("event_email_not_found_contact_confirm")) {
			playSound("beep");
		}
		if (eventName.equals("click_edit_settings")) {
			playSound("beep");
		}
	}

	/**
	 * play the sound
	 */
	private void playSound(String soundName) {
		int i = 0;
		int index = 0;
		for (String s : rawNames) {
			if (s.equals(soundName)) {
				index = i;
				break;
			}
			i++;
		}
		try {
			mSoundManager.playSound(index);
		} catch (Exception e) {
		}
	}

	/** create the main menu based on if the app is the full version or not */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		String isFullVersion = getResources().getString(R.string.is_full_version);
		if (isFullVersion.toLowerCase().equals("true")) {
			getMenuInflater().inflate(R.menu.main_full, menu);
		} else {
			getMenuInflater().inflate(R.menu.main, menu);
		}
		return true;
	}

	/** handle user selecting a menu item */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		/** Handle item selection */
		switch (item.getItemId()) {
		case R.id.soundsTurnedOnOff:
			soundsTurnedOn = !soundsTurnedOn;
			SharedPreferences.Editor editor = wmbPreference.edit();
			editor.putBoolean("soundsTurnedOn", soundsTurnedOn);
			editor.commit();
			setSilentModeMessage();
			return true;
		case R.id.menu_settings:
			Intent intent = new Intent(ItemsActivity.this, SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_bitstreet:
			viewAllPublisherApps();
			break;
		case R.id.menu_fullversion:
			viewPremiumApp();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/**
	 * view all apps on the device marketplace for current publisher
	 */
	public void viewAllPublisherApps() {
		MarketPlace marketPlace = new MarketPlace(this);
		Intent intent = marketPlace.getViewAllPublisherAppsIntent(this);
		if (intent != null) {
			startActivity(intent);
		}
	}

	/**
	 * view the premium version of this app
	 */
	public void viewPremiumApp() {
		MarketPlace marketPlace = new MarketPlace(this);
		Intent intent = marketPlace.getViewPremiumAppIntent(this);
		if (intent != null) {
			startActivity(intent);
		}
	}

	/**
	 * According to Roberto Orci, stardates were revised again for the 2009 film Star Trek so that
	 * the first four digits correspond to the year, while the remainder was intended to stand for
	 * the day of the year. For example, stardate 2233.04 would be January 4, 2233. Star Trek Into
	 * Darkness begins on stardate 2259.55, or February 24, 2259.
	 * 
	 * @return
	 */
	public static String getStarDate() {
		SimpleDateFormat localDateDF = new SimpleDateFormat("yyyy.D", Locale.getDefault());
		return localDateDF.format(Calendar.getInstance().getTime());
	}

	/**
	 * add or edit a recently tried item via recently tried type
	 */
	protected void addEditItemStatus() {
		Status status = getStatus();
		status.setId(recentlyTriedItemID);
		String statusDate = getLastUpdateTime();
		if (recentlyTriedEditType.equals("edit")) {
			status.setContent("E," + statusDate);
		} else {
			status.setContent("A," + statusDate);
		}
		statusDataSource.editStatus(status);
	}

	/**
	 * add entry to DB based on user input
	 * 
	 * @param statusType
	 *            the message to be stored in the item's status table
	 * @return the last entered ID of the item
	 */
	private long addEntryDB(String statusType) {
		/** add edit the value in the DB */
		currentTitleValue = editTextTitle.getText().toString();
		currentMessageValue = messageContent.getText().toString();
		Item newItem = null;
		if (currentTitleValue.length() != 0) {
			newItem = itemsDataSource.createItem(currentTitleValue, currentMessageValue);
			statusDataSource.createStatus(newItem.getId(), statusType);
		}
		setupItemsList();
		return newItem.getId();
	}

	/**
	 * edit existing item in the DB
	 * 
	 * @param statusType
	 *            the message to be stored in the item's status table
	 * @return the last entered ID of the item
	 */
	private long editEntryDB(String statusType) {
		currentTitleValue = editTextTitle.getText().toString();
		currentMessageValue = messageContent.getText().toString();
		if (currentTitleValue.length() != 0) {

			/** edit the existing item */
			Item editItem = new Item();
			editItem.setId(currentID);
			editItem.setName(currentTitleValue);
			editItem.setContent(currentMessageValue);
			itemsDataSource.editItem(editItem);

			/** edit the existing item's status */
			Status status = getStatus();
			status.setId(currentID);
			status.setContent(statusType);
			statusDataSource.editStatus(status);
		}
		setupItemsList();
		return currentID;
	}

	/**
	 * get screen metrics
	 */
	private void getDisplayMetrics() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
	}

	/**
	 * set the footer message about if the app is in silent mode or not
	 */
	protected void setSilentModeMessage() {
		TextView silentModeMessage = (TextView) findViewById(R.id.silentModeMessage);
		silentModeMessage.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		silentModeMessage.setText("");
		if (!soundsTurnedOn) {
			silentModeMessage.setText("Silent Mode - On -");
		}
	}
}