package com.kevinhinds.dontforget.widget;

import com.kevinhinds.dontforget.ItemsActivity;
import com.kevinhinds.dontforget.R;
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
 * create the basic CountWidget widget that will use a service call in a separate thread to update the widget elements
 * 
 * @author khinds
 */
public class CountWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		/** to avoid the ANR "application not responding" error request update for these widgets and launch updater service via a new thread */
		appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, CountWidget.class));
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
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.count_widget);

		/** update the current count of the archived and non-archived messages */
		ItemsDataSource itemsDataSource = new ItemsDataSource(context);
		itemsDataSource.open();
		views.setTextViewText(R.id.messageCount, "[" + Integer.toString(itemsDataSource.getCountItemsbyArchiveType(0)) + "] Messages");
		views.setTextViewText(R.id.archiveCount, "[" + Integer.toString(itemsDataSource.getCountItemsbyArchiveType(1)) + "] Archived");

		/** apply an intent to the widget as a whole to launch the MainActivity */
		Intent intent = new Intent(context, ItemsActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.countWidgetContainer, pendingIntent);
		return views;
	}
}