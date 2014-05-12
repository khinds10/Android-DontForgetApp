package com.kevinhinds.dontforget.widget;

import java.util.Iterator;
import java.util.List;

import com.kevinhinds.dontforget.ItemsActivity;
import com.kevinhinds.dontforget.R;
import com.kevinhinds.dontforget.item.Item;
import com.kevinhinds.dontforget.item.ItemsDataSource;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * create the basic ListWidget widget that will use a service call in a separate thread to update the widget elements
 * 
 * @author khinds
 */
public class ListWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		/** to avoid the ANR "application not responding" error request update for these widgets and launch updater service via a new thread */
		appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ListWidget.class));
		UpdateService.requestUpdate(appWidgetIds);
		context.startService(new Intent(context, UpdateService.class));
	}

	/**
	 * the UpdateService thread is now ready to update the elements on the widget
	 * 
	 * @param context
	 * @param appWidgetUri
	 * @return
	 */
	public static RemoteViews buildUpdate(Context context, Uri appWidgetUri) {

		/** update the widget UI elements based on on the current situation */
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.list_widget);

		/** update the current count of the archived and non-archived messages */
		ItemsDataSource itemsDataSource = new ItemsDataSource(context);
		itemsDataSource.open();

		/** get recent non-archived items to show in the widget as line items */
		final List<Item> itemlist = itemsDataSource.getAllItemsbyArchiveType(0);

		/** iterate over the itemlist to build the menu items to show */
		Iterator<Item> itemlistiterator = itemlist.iterator();
		int count = 0;
		String previousName = "";
		views.setTextViewText(R.id.recentItem1, "");
		views.setTextViewText(R.id.recentItem2, "");
		views.setTextViewText(R.id.recentItem3, "");
		views.setTextViewText(R.id.recentItem4, "");
		while (itemlistiterator.hasNext()) {
			count++;
			Item itemlistitem = itemlistiterator.next();
			String itemName = itemlistitem.getName();
			if (!previousName.equals(itemName)) {
				if (count == 1) {
					views.setTextViewText(R.id.recentItem1, itemName);
				} else if (count == 2) {
					views.setTextViewText(R.id.recentItem2, itemName);
				} else if (count == 3) {
					views.setTextViewText(R.id.recentItem3, itemName);
				} else if (count == 4) {
					views.setTextViewText(R.id.recentItem4, itemName);
				}
			}
			previousName = itemName;
		}

		/** apply an intent to the widget as a whole to launch the MainActivity */
		Intent intent = new Intent(context, ItemsActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.listWidgetContainer, pendingIntent);
		return views;
	}
}
