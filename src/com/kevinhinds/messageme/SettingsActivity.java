package com.kevinhinds.messageme;

import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;

/**
 * Settings Activity for items
 * 
 * @author khinds
 */
public class SettingsActivity extends Activity {

	public final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" + "\\@" + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\." + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}"
			+ ")+");

	protected String usersEmail;
	protected String usersPhone;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		usersEmail = lookupUserPossibleEmail();
	}

	/**
	 * get users email from possibilities based on accounts
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