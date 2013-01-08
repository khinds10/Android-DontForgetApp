package com.kevinhinds.dontforget;

import java.util.regex.Pattern;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.array_times, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Spinner morningSpinner = (Spinner) findViewById(R.id.morningSpinner);
		morningSpinner.setAdapter(adapter);

		Spinner afternoonSpinner = (Spinner) findViewById(R.id.afternoonSpinner);
		afternoonSpinner.setAdapter(adapter);

		Spinner eveningSpinner = (Spinner) findViewById(R.id.eveningSpinner);
		eveningSpinner.setAdapter(adapter);

		/** apply font to title */
		TextView AppTitle = (TextView) findViewById(R.id.AppTitle);
		AppTitle.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PermanentMarker.ttf"));

		/** get the references to the EditText Values */
		emailPassword = (EditText) findViewById(R.id.emailPassword);
		phoneNumber = (EditText) findViewById(R.id.phoneNumber);
		emailAddress = (EditText) findViewById(R.id.emailAddress);

		/** get the sharedPreferences to edit via user's request */
		wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);

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
		Button saveButton = (Button) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setValues();
			}
		});
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