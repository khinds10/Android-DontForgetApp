package com.kevinhinds.messageme;

import java.util.Iterator;
import java.util.List;

import com.kevinhinds.messageme.item.Item;
import com.kevinhinds.messageme.item.ItemsDataSource;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Archived Activity for items
 * 
 * @author khinds
 */
public class ArchiveActivity extends Activity {

	private ItemsDataSource itemsDataSource;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.archive);

		/** open data connections */
		openDataConnections();

		/** setup the list of items to show the user */
		setupItemsList();

		TextView CurrentMessages = (TextView) findViewById(R.id.CurrentMessages);
		CurrentMessages.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ArchiveActivity.this, ItemsActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				startActivity(intent);
			}
		});

		TextView ArchivedMessages = (TextView) findViewById(R.id.ArchivedMessages);
		ArchivedMessages.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ArchiveActivity.this, ArchiveActivity.class);
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
		int i = 0;
		while (itemlistiterator.hasNext()) {
			i++;
			Item itemlistitem = itemlistiterator.next();
			String itemName = itemlistitem.getName();

			TextView tv = new TextView(this);
			tv.setId(i);
			tv.setTextSize(18);
			tv.setText((CharSequence) itemName);
			tv.setLayoutParams(lp);
			tv.setClickable(true);
			tv.setTextColor(Color.BLACK);
			tv.setPadding(10, 8, 0, 8);
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_launcher), null, null, null);
			tv.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					TextView tv = (TextView) v;
					String itemValue = (String) tv.getText();

					final TextView itemInQuestion = new TextView(ArchiveActivity.this);
					itemInQuestion.setPadding(10, 0, 10, 10);
					itemInQuestion.setText((CharSequence) itemValue);
					itemInQuestion.setTextSize(18);

				}
			});
			ll.addView(tv);
		}
	}

	/**
	 * open our data connections to items/itemslist
	 */
	private void openDataConnections() {
		itemsDataSource = new ItemsDataSource(this);
		itemsDataSource.open();
	}
}
