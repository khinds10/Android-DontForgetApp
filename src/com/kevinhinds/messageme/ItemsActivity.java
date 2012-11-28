package com.kevinhinds.messageme;

import java.util.Iterator;
import java.util.List;

import com.kevinhinds.messageme.email.GMailSender;
import com.kevinhinds.messageme.item.Item;
import com.kevinhinds.messageme.item.ItemsDataSource;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

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

	protected int isArchivedMessageView;

	protected TextView CurrentMessagesLabel;
	protected TextView ArchivedMessagesLabel;

	protected ProgressDialog progressDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.items);

		CurrentMessagesLabel = (TextView) findViewById(R.id.CurrentMessages);
		ArchivedMessagesLabel = (TextView) findViewById(R.id.ArchivedMessages);

		/** non-archived messages by default */
		isArchivedMessageView = 0;

		/** open data connections */
		openDataConnections();

		/** setup the list of items to show the user */
		setupItemsList();

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
				ArchivedMessagesLabel.setTextColor(Color.GRAY);
				ArchivedMessagesLabel.setTypeface(null, Typeface.NORMAL);
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
				ArchivedMessagesLabel.setTextColor(Color.BLACK);
				ArchivedMessagesLabel.setTypeface(null, Typeface.BOLD);
				setupItemsList();
			}
		});
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
			public void onClick(View v) {
				if (editMode) {
					editEntryDB();
				} else {
					addEntryDB();
				}
				pw.dismiss();
			}
		});

		/** set button is pressed, set the timer and close the popup */
		TextView archiveOptionButton = (TextView) layout.findViewById(R.id.ArchiveButton);
		archiveOptionButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/** set archive / un-archive by user request */
				if (isArchivedMessageView == 1) {
					archiveOptionEntryDB(0);
				} else {
					archiveOptionEntryDB(1);
				}
				pw.dismiss();
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
				try {
					progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending Email", "Emailing...\n" + "khinds10@gmail.com");
					new Thread() {
						public void run() {
							try {
								String currentTitle = editTextTitle.getText().toString();
								String currentMessage = messageContent.getText().toString();
								GMailSender sender = new GMailSender("khinds10@gmail.com", "$ecurity!123");
								sender.sendMail(currentTitle + "   (Don't Forget! for Android)", currentMessage + "\n\n--\nDon't Forget! for Android", "khinds10@gmail.com", "khinds10@gmail.com");
								if (editMode) {
									editEntryDB();
								} else {
									addEntryDB();
								}
							} catch (Exception e) {
							}
							progressDialog.dismiss();
						}
					}.start();
					pw.dismiss();
				} catch (Exception e) {
				}
			}
		});

		/** SMS yourself button */
		TextView sendSMSButton = (TextView) layout.findViewById(R.id.sendSMSButton);
		sendSMSButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					progressDialog = ProgressDialog.show(ItemsActivity.this, "Sending SMS Message", "Texting...\n" + "16467159249");
					new Thread() {
						public void run() {
							try {
								String currentTitle = editTextTitle.getText().toString();
								String currentMessage = messageContent.getText().toString();
								sendSMS("16467159249", currentTitle + "\n" + currentMessage + "\n\n--\nDon't Forget! for Android");
								if (editMode) {
									editEntryDB();
								} else {
									addEntryDB();
								}
							} catch (Exception e) {
							}
							try {
								sleep(1000);
							} catch (InterruptedException e) {
							}
							progressDialog.dismiss();
						}
					}.start();
					pw.dismiss();
				} catch (Exception e) {
				}
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
		itemsDataSource.open();
	}

	/** sends an SMS message to another device */
	private void sendSMS(String phoneNumber, String message) {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
		PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

		/** when the SMS has been sent */
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(getBaseContext(), "Text Message Could not be Sent", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(getBaseContext(), "Text Message Could not be Sent", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(getBaseContext(), "Text Message Could not be Sent", Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(getBaseContext(), "Text Message Could not be Sent", Toast.LENGTH_SHORT).show();
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
					Toast.makeText(getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
					break;
				case Activity.RESULT_CANCELED:
					Toast.makeText(getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
					break;
				}
			}
		}, new IntentFilter(DELIVERED));

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
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
}