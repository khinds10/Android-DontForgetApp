package com.kevinhinds.dontforget;

import java.io.InputStream;
import java.util.regex.Pattern;

import com.kevinhinds.dontforget.views.GifDecoderView;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Settings Activity for items
 * 
 * @author khinds
 */
public class SettingsActivity extends Activity {

	public final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" + "\\@" + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\." + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}"
			+ ")+");

	protected String usersEmail = "";
	protected String usersPhone = "";
	protected String usersPassword = "";
	SharedPreferences wmbPreference;
	EditText emailPassword;
	EditText phoneNumber;
	EditText emailAddress;

	/** references to the 3 spinners in activity */
	Spinner morningSpinner;
	Spinner afternoonSpinner;
	Spinner eveningSpinner;

	/** the font for the buttons */
	public String buttonFont = "fonts/Muro.otf";

	/** font for the titles */
	public String titleFont = "fonts/talldark.ttf";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		/** get the sharedPreferences to edit via user's request */
		wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.array_times, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		/** create morning spinner values and set to the current use selection */
		morningSpinner = (Spinner) findViewById(R.id.morningSpinner);
		morningSpinner.setAdapter(adapter);
		String morning = wmbPreference.getString("MORNING", "9 AM");
		int morningPosition = adapter.getPosition(morning);
		morningSpinner.setSelection(morningPosition);

		/** create afternoon spinner values and set to the current use selection */
		afternoonSpinner = (Spinner) findViewById(R.id.afternoonSpinner);
		afternoonSpinner.setAdapter(adapter);
		String afternoon = wmbPreference.getString("AFTERNOON", "2 PM");
		int afternoonPosition = adapter.getPosition(afternoon);
		afternoonSpinner.setSelection(afternoonPosition);

		/** create evening spinner values and set to the current use selection */
		eveningSpinner = (Spinner) findViewById(R.id.eveningSpinner);
		eveningSpinner.setAdapter(adapter);
		String evening = wmbPreference.getString("EVENING", "6 PM");
		int eveningPosition = adapter.getPosition(evening);
		eveningSpinner.setSelection(eveningPosition);

		/** get the references to the EditText Values */
		emailPassword = (EditText) findViewById(R.id.emailPassword);
		phoneNumber = (EditText) findViewById(R.id.phoneNumber);
		emailAddress = (EditText) findViewById(R.id.emailAddress);

		/** populate the email from settings or accounts */
		usersEmail = wmbPreference.getString("USER_EMAIL", "");
		if (usersEmail.equals("")) {
			usersEmail = lookupUserPossibleEmail();
		}
		emailAddress.setText(usersEmail);

		/** populate the phone number from settings or accounts */
		usersPhone = wmbPreference.getString("USER_PHONE", "");
		if (usersPhone.equals("")) {
			usersPhone = getMyPhoneNumber();
		}
		phoneNumber.setText(usersPhone);

		usersPassword = wmbPreference.getString("USER_PASSWORD", "");
		emailPassword.setText(usersPassword);

		/** if you click the archive messages option */
		TextView saveButton = (TextView) findViewById(R.id.saveButton);
		saveButton.setTypeface(Typeface.createFromAsset(this.getAssets(), buttonFont));
		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setValues();
			}
		});

		/** title font to localDate and display stardate */
		TextView localDateTextView = (TextView) findViewById(R.id.localDate);
		localDateTextView.setTypeface(Typeface.createFromAsset(this.getAssets(), titleFont));
		localDateTextView.setText(ItemsActivity.getStarDate());

		/** create the graph animated GIF */
		InputStream stream = null;
		stream = getResources().openRawResource(R.drawable.graph);
		GifDecoderView staticView = new GifDecoderView(this, stream);
		RelativeLayout.LayoutParams lhw = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		staticView.setLayoutParams(lhw);
		staticView.setPadding(0, 0, 0, 0);
		RelativeLayout lineGifBox = (RelativeLayout) findViewById(R.id.alertGif);
		lineGifBox.addView(staticView);
	}

	/**
	 * set the values the user wishes
	 */
	private void setValues() {
		/** get the preferences editor */
		SharedPreferences.Editor editor = wmbPreference.edit();

		/** save the values in the user preferences */
		editor.putString("USER_EMAIL", emailAddress.getText().toString());
		editor.putString("USER_PHONE", phoneNumber.getText().toString());
		editor.putString("USER_PASSWORD", emailPassword.getText().toString());

		/** set selected spinner values */
		editor.putString("MORNING", morningSpinner.getSelectedItem().toString());
		editor.putString("AFTERNOON", afternoonSpinner.getSelectedItem().toString());
		editor.putString("EVENING", eveningSpinner.getSelectedItem().toString());
		editor.commit();

		Intent intent = new Intent(SettingsActivity.this, ItemsActivity.class);
		startActivity(intent);
	}

	/**
	 * get user's cellphone number
	 * 
	 * @return
	 */
	private String getMyPhoneNumber() {
		TelephonyManager mTelephonyMgr;
		mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		return mTelephonyMgr.getLine1Number();
	}

	/**
	 * get users email from possibilities based on accounts
	 * 
	 * @return
	 */
	private String lookupUserPossibleEmail() {
		String possibleEmail = null;
		String probableEmail = null;
		Pattern emailPattern = EMAIL_ADDRESS_PATTERN;
		Account[] accounts = AccountManager.get(getBaseContext()).getAccounts();
		for (Account account : accounts) {
			if (emailPattern.matcher(account.name).matches()) {
				possibleEmail = account.name;
				if (possibleEmail.contains("gmail.com")) {
					probableEmail = account.name;
				}
			}
		}
		return probableEmail;
	}
}