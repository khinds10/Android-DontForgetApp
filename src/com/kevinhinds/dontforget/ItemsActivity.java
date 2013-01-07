package com.kevinhinds.dontforget;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.kevinhinds.dontforget.alarmmanager.AlarmManagerBroadcastReceiver;
import com.kevinhinds.dontforget.email.GMailSender;
import com.kevinhinds.dontforget.item.Item;
import com.kevinhinds.dontforget.item.ItemsDataSource;
import com.kevinhinds.dontforget.status.Status;
import com.kevinhinds.dontforget.status.StatusDataSource;
import com.kevinhinds.dontforget.widget.CountWidget;
import com.kevinhinds.dontforget.widget.ListWidget;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;

/**
 * Main Activity for items
 * 
 * @author khinds
 */
public class ItemsActivity extends Activity {

	private static final int CONTACT_PICKER_RESULT = 1001;
	private ItemsDataSource itemsDataSource;
	private StatusDataSource statusDataSource;

	private View layout = null;
	private PopupWindow pw;

	private int screenHeight;
	private int screenWidth;

	protected TextView editTextTitle;
	protected TextView messageContent;

	protected long currentID;
	protected String currentTitleValue;
	protected String currentMessageValue;

	private AlarmManagerBroadcastReceiver alarm;

	/**
	 * save the most recently tried to email / SMS item's ID, so if it didn't go through we can change the status to reflect as such
	 */
	protected long recentlyTriedItemID;

	/**
	 * save the most recently tried to email / SMS item's edit type either if it was a "add" or "edit" type of operation to reflect in the status if it fails
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
	protected String friendsEmail;
	protected String friendsSMS;

	/** which type of activity for result request wishes, either to email or SMS */
	protected String activityForResultType = "email";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.items);

		/** get the current settings for the user */
		getUserSettings();

		CurrentMessagesLabel = (TextView) findViewById(R.id.CurrentMessages);
		ArchivedMessagesLabel = (TextView) findViewById(R.id.ArchivedMessages);

		/** non-archived messages by default */
		isArchivedMessageView = 0;

		/** open data connections */
		openDataConnections();

		/** setup the list of items to show the user */
		setupItemsList();

		/** apply font to title */
		TextView AppTitle = (TextView) findViewById(R.id.AppTitle);
		AppTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PermanentMarker.ttf"));

		/** apply font to add new button */
		TextView addNewButtonText = (TextView) findViewById(R.id.addNewButtonText);
		addNewButtonText.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PermanentMarker.ttf"));

		/** get screen metrics */
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		screenHeight = displaymetrics.heightPixels;
		screenWidth = displaymetrics.widthPixels;

		/** we start with the current messages view */
		CurrentMessagesLabel.setTextColor(Color.BLACK);
		CurrentMessagesLabel.setTypeface(null, Typeface.BOLD);

		/** if you click the current messages option */
		TextView CurrentMessages = (TextView) findViewById(R.id.CurrentMessages);
		CurrentMessages.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				isArchivedMessageView = 0;
				CurrentMessagesLabel.setTextColor(Color.BLACK);
				CurrentMessagesLabel.setTypeface(null, Typeface.BOLD);
				CurrentMessagesLabel.setBackgroundColor(Color.parseColor("#E0E0E0"));
				ArchivedMessagesLabel.setTextColor(Color.GRAY);
				ArchivedMessagesLabel.setTypeface(null, Typeface.NORMAL);
				ArchivedMessagesLabel.setBackgroundColor(Color.parseColor("#F0F0F0"));
				setupItemsList();
			}
		});

		/** if you click the archive messages option */
		TextView ArchivedMessages = (TextView) findViewById(R.id.ArchivedMessages);
		ArchivedMessages.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				isArchivedMessageView = 1;
				CurrentMessagesLabel.setTextColor(Color.GRAY);
				CurrentMessagesLabel.setTypeface(null, Typeface.NORMAL);
				CurrentMessagesLabel.setBackgroundColor(Color.parseColor("#F0F0F0"));
				ArchivedMessagesLabel.setTextColor(Color.BLACK);
				ArchivedMessagesLabel.setTypeface(null, Typeface.BOLD);
				ArchivedMessagesLabel.setBackgroundColor(Color.parseColor("#E0E0E0"));
				setupItemsList();
			}
		});

		/** if you click the add new messages button */
		LinearLayout addNewButton = (LinearLayout) findViewById(R.id.addNewButton);
		addNewButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/** popup to add the new item */
				initiateEditMessagePopup(false);
			}
		});

		/** setup the AlarmManagerBroadcastReceiver for the ability to set an alarm item in the future */
		alarm = new AlarmManagerBroadcastReceiver();
	}

	/**
	 * create the list of items on the activity as they currently are saved in the database
	 */
	private void setupItemsList() {

		/** the message count text fields should reflect how many archived / non-archived messages */
		CurrentMessagesLabel.setText("[" + Integer.toString(itemsDataSource.getCountItemsbyArchiveType(0)) + "] Messages");
		ArchivedMessagesLabel.setText("[" + Integer.toString(itemsDataSource.getCountItemsbyArchiveType(1)) + "] Archived");

		/** get all the itemlist items saved in the DB set to archived = false */
		final List<Item> itemlist = itemsDataSource.getAllItemsbyArchiveType(isArchivedMessageView);

		/** attach to the LinearLayout to add TextViews dynamically via menuValues */
		LinearLayout ll = (LinearLayout) findViewById(R.id.itemsLayout);
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
			tv.setGravity(Gravity.CENTER_VERTICAL);
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.bubble), null, null, null);
			tv.setCompoundDrawablePadding(10);
			tv.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PermanentMarker.ttf"));
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

			/** show the status of the item by the current ID being processed */
			TextView textStatus = new TextView(this);
			textStatus.setTextSize(10);
			Status Status = statusDataSource.getById(ID);
			String currentStatusDetails = Status.content;
			textStatus.setText((CharSequence) currentStatusDetails);
			textStatus.setLayoutParams(lp);
			textStatus.setClickable(true);
			textStatus.setTextColor(Color.GRAY);
			textStatus.setPadding(50, 8, 0, 8);
			textStatus.setGravity(Gravity.CENTER_VERTICAL);
			textStatus.setCompoundDrawablePadding(10);
			ll.addView(textStatus);
		}

		/** update the external widgets with any changes that have happened */
		updateWidgets();
	}

	/**
	 * show the popup window for the editing of don't forget messages
	 */
	private void initiateEditMessagePopup(final boolean editMode) {

		/** keep the most recent value of if we're editing global for more involved workflows below */
		currentEditMode = editMode;

		/** get any recent changes to the user settings */
		getUserSettings();

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

		TextView popupTitle = (TextView) layout.findViewById(R.id.popupTitle);
		popupTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PermanentMarker.ttf"));

		editTextTitle = (TextView) layout.findViewById(R.id.editTextTitle);
		messageContent = (TextView) layout.findViewById(R.id.messageContent);
		TextView deleteButton = (TextView) layout.findViewById(R.id.DeleteButton);
		TextView archiveButton = (TextView) layout.findViewById(R.id.ArchiveButton);
		final TextView readOnlyTitletextView = (TextView) layout.findViewById(R.id.readOnlyTitletextView);
		final TextView readOnlyMessagetextView = (TextView) layout.findViewById(R.id.readOnlyMessagetextView);

		/** edit existing memo buttons and container */
		final LinearLayout editButtonContainer = (LinearLayout) layout.findViewById(R.id.editButtonContainer);
		final Button editMemoButton = (Button) layout.findViewById(R.id.editMemoButton);
		final Button cancelEditMemoButton = (Button) layout.findViewById(R.id.cancelEditMemoButton);

		/** if we're editing an existing entry the other buttons are enabled, else you can't use them yet */
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

		/** cancel edit button is pressed, change the popup to be able to edit */
		cancelEditMemoButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
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

		/** set the button text to Archived/Un-Archived by what you're able to do */
		if (isArchivedMessageView == 1) {
			archiveButton.setText("Un-Archive");
			archiveButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.unarchive, 0, 0, 0);
		} else {
			archiveButton.setText("Archive");
			archiveButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.archive, 0, 0, 0);
		}

		/** set button is pressed, set the timer and close the popup */
		TextView setButton = (TextView) layout.findViewById(R.id.SaveButton);
		setButton.setOnClickListener(new OnClickListener() {
			String statusDate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());

			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					showEmptyTitleMessage();
				} else {
					if (editMode) {
						recentlyTriedItemID = editEntryDB("[edited] " + statusDate);
						recentlyTriedEditType = "edit";
					} else {
						recentlyTriedItemID = addEntryDB("[added] " + statusDate);
						recentlyTriedEditType = "add";
					}
					pw.dismiss();
				}
			}
		});

		/** set button is pressed, set the timer and close the popup */
		TextView archiveOptionButton = (TextView) layout.findViewById(R.id.ArchiveButton);
		archiveOptionButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					showEmptyTitleMessage();
				} else {
					/** set archive / un-archive by user request */
					if (isArchivedMessageView == 1) {
						archiveOptionEntryDB(0);
					} else {
						archiveOptionEntryDB(1);
					}
					pw.dismiss();
				}
			}
		});

		/** cancel button to close */
		TextView cancelButton = (TextView) layout.findViewById(R.id.cancelButton);
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

		/** email yourself button */
		TextView sendButton = (TextView) layout.findViewById(R.id.sendEmailButton);
		sendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					showEmptyTitleMessage();
				} else {
					progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending Email", "Emailing...\n" + usersEmail);
					String statusValue = "[emailed yourself] " + java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
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
		sendContactEmailButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					showEmptyTitleMessage();
				} else {
					/** select a friends email to send a reminder to */
					activityForResultType = "email";
					Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
					startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
				}
			}
		});

		/** SMS friend button */
		TextView sendContactSMSButton = (TextView) layout.findViewById(R.id.sendContactSMSButton);
		sendContactSMSButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					showEmptyTitleMessage();
				} else {
					/** select a friends email to send a reminder to */
					activityForResultType = "SMS";
					Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
					startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
				}
			}
		});

		/** SMS yourself button */
		TextView sendSMSButton = (TextView) layout.findViewById(R.id.sendSMSButton);
		sendSMSButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					showEmptyTitleMessage();
				} else {
					progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending SMS Message", "Texting...\n" + usersPhone);
					String statusValue = "[texted yourself] " + java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
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
		RemindButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isTitleEmpty(editTextTitle)) {
					showEmptyTitleMessage();
				} else {
					Context context =getBaseContext();
					alarm.setOnetimeTimer(context);
					
					
					// public void onetimeTimer(View view){
					// 
					// if(alarm != null){
					// alarm.setOnetimeTimer(context);
					// }else{
					// Toast.makeText(context, "Alarm is null", Toast.LENGTH_SHORT).show();
					// }
					// }
					
					// public void startRepeatingTimer(View view) {
					// Context context = this.getApplicationContext();
					// if(alarm != null){
					// alarm.SetAlarm(context);
					// }else{
					// Toast.makeText(context, "Alarm is null", Toast.LENGTH_SHORT).show();
					// }
					// }
					//
					// public void cancelRepeatingTimer(View view){
					// Context context = this.getApplicationContext();
					// if(alarm != null){
					// alarm.CancelAlarm(context);
					// }else{
					// Toast.makeText(context, "Alarm is null", Toast.LENGTH_SHORT).show();
					// }
					// }
					//

				}
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		/** values to get for contact to continue with the activity for result */
		friendsEmail = "";
		friendsSMS = "";
		String id = "";

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				try {

					/**
					 * get email for contact, basically for now, the first one found
					 */
					Uri result = data.getData();
					id = result.getLastPathSegment();
					Cursor cursor = getContentResolver().query(Email.CONTENT_URI, null, Email.CONTACT_ID + "=?", new String[] { id }, null);
					int emailIdx = cursor.getColumnIndex(Email.DATA);
					/** let's just get the first email */
					String email = "";
					if (cursor.moveToFirst()) {
						email = cursor.getString(emailIdx);
					}
					if (email.length() != 0) {
						friendsEmail = email;
					}
					cursor.close();

					/**
					 * get SMS phone number for contact, based on the contacts "primary" phone number set
					 */
					String phoneNo = "";
					int isPrimaryNumber = 0;
					Cursor pCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id },
							null);
					while (pCur.moveToNext()) {
						isPrimaryNumber = pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY));
						phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
						if (isPrimaryNumber == 1) {
							friendsSMS = phoneNo;
						}
					}
					pCur.close();
				} catch (Exception e) {
				}
				break;
			}
		}

		/** based on recent activityForResultType requested, send an SMS or Email respectively */
		if (activityForResultType.equals("SMS")) {

			/** if we have an primary phone number for our friend, then send it out to them, else show the error */
			if (friendsSMS.length() != 0) {
				AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
				alertDialog.setTitle("Send Reminder to Friend");
				alertDialog.setMessage("Send text to contact's primary number -> " + friendsSMS);
				alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending SMS Text Message", "Texting contact's primary number: \n" + friendsSMS);
						String statusValue = "[texted: " + friendsSMS + "] " + java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
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
						dialog.dismiss();
					}
				});
				alertDialog.setIcon(R.drawable.ic_launcher);
				alertDialog.show();
			} else {
				AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
				alertDialog.setTitle("Send Reminder to Friend");
				alertDialog.setMessage("Primary phone number couldn't be located for contact");

				/** Update status to reflect that the entry was simply "edited" or "added" it couldn't be sent via SMS */
				Status status = getStatus();
				status.setId(recentlyTriedItemID);
				String statusDate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
				if (recentlyTriedEditType.equals("edit")) {
					status.setContent("[edited] " + statusDate);
				} else {
					status.setContent("[added] " + statusDate);
				}
				statusDataSource.editStatus(status);

				alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				alertDialog.setIcon(R.drawable.ic_launcher);
				alertDialog.show();
			}

		} else {

			/** if we have an email for our friend, then send it out to them, else show the error */
			if (friendsEmail.length() != 0) {
				AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
				alertDialog.setTitle("Send Reminder to Friend");
				alertDialog.setMessage("Send Message to -> " + friendsEmail);
				alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending Email", "Emailing...\n" + friendsEmail);
						String statusValue = "[emailed: " + friendsEmail + "] " + java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
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
						dialog.dismiss();
					}
				});
				alertDialog.setIcon(R.drawable.ic_launcher);
				alertDialog.show();
			} else {
				AlertDialog alertDialog = new AlertDialog.Builder(ItemsActivity.this).create();
				alertDialog.setTitle("Send Reminder to Friend");
				alertDialog.setMessage("Email address couldn't be located for contact");

				/** Update status to reflect that the entry was simply "edited" or "added" it couldn't be sent via email */
				Status status = getStatus();
				status.setId(recentlyTriedItemID);
				String statusDate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
				if (recentlyTriedEditType.equals("edit")) {
					status.setContent("[edited] " + statusDate);
				} else {
					status.setContent("[added] " + statusDate);
				}
				statusDataSource.editStatus(status);

				alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				alertDialog.setIcon(R.drawable.ic_launcher);
				alertDialog.show();
			}
		}
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
		String displayEmail = "";
		if (usersEmail.equals("")) {
			userGmail.setTextColor(Color.BLACK);
			displayEmail = "(Please configure)";
		} else {
			displayEmail = usersEmail;
		}
		userGmail.setText(displayEmail);

		/** show the user the most current phone number */
		TextView userPhoneNumber = (TextView) findViewById(R.id.userPhoneNumber);
		String displayPhone = "";
		if (usersPhone.equals("")) {
			userPhoneNumber.setTextColor(Color.BLACK);
			displayPhone = "(Please configure)";
		} else {
			displayPhone = usersPhone;
		}
		userPhoneNumber.setText(displayPhone);

		/** click the layout to edit settings */
		LinearLayout subContentAccountSettings = (LinearLayout) findViewById(R.id.subContentAccountSettings);
		subContentAccountSettings.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ItemsActivity.this, SettingsActivity.class);
				startActivity(intent);
			}
		});
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/** Handle item selection */
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(ItemsActivity.this, SettingsActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
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
				alertDialog.setTitle("SMS could not be sent...");
				alertDialog.setMessage("Check your settings and try again.");

				/** Update status to reflect that the entry was simply "edited" or "added" it couldn't be sent via SMS */
				itemStatus.setId(recentlyTriedItemID);
				String statusDate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
				if (recentlyTriedEditType.equals("edit")) {
					itemStatus.setContent("[edited] " + statusDate);
				} else {
					itemStatus.setContent("[added] " + statusDate);
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
				sender.sendMail(currentTitle + "   (Don't Forget! for Android)", currentMessage + "\n\n--\nDon't Forget! for Android", usersEmail, recipient);
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
				alertDialog.setTitle("Email could not be sent...");
				alertDialog.setMessage("Check your settings and try again.");

				/** Update status to reflect that the entry was simply "edited" or "added" it couldn't be sent via Email */
				itemStatus.setId(recentlyTriedItemID);
				String statusDate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
				if (recentlyTriedEditType.equals("edit")) {
					itemStatus.setContent("[edited] " + statusDate);
				} else {
					itemStatus.setContent("[added] " + statusDate);
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
	 * update any currently running widgets with the latest and greatest from this activity that was loaded
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
	@SuppressWarnings("null")
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
}