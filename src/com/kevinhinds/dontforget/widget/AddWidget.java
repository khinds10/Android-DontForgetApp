package com.kevinhinds.dontforget.widget;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.kevinhinds.dontforget.ItemsActivity;
import com.kevinhinds.dontforget.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * create the basic AddWidget widget that will use a service call in a separate thread to update the widget elements
 * 
 * @author khinds
 */
public class AddWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		/** to avoid the ANR "application not responding" error request update for these widgets and launch updater service via a new thread */
		appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, AddWidget.class));
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

		/** determine if there is a valid internet connection to process the widget update */
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		boolean isConnected = false;
		if (networkInfo != null && networkInfo.isConnected()) {
			isConnected = true;
		}

		/** randomized URL string value */
		Random randomGenerator = new Random();
		int randomInt = randomGenerator.nextInt(1000000);
		String timeStamp = Integer.toString(randomInt);

		/** update the widget UI elments based on on the current situation */
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.add_widget);

		/** last update time displayed on widget */
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm MM/dd/yyyy");
		String currentDateandTime = sdf.format(new Date());

		/** update widget based on how the internet connectivity is */
		if (!isConnected) {
			//views.setTextViewText(R.id.addwidgetLastUpdate, "No Connection");
		} else {
			/** get the moon live image from the online location */
			InputStream is = null;
			try {
				is = fetch("http://www.kevinhinds.com/solar_system/moon.jpg?random=" + timeStamp);
			} catch (Exception e) {
			}

			/** create a resized image bitmap to show on the widget */
			//Bitmap bm = BitmapFactory.decodeStream(is);
			//Bitmap resizedbitmap = null;
			//resizedbitmap = Bitmap.createScaledBitmap(bm, 190, 190, true);
			//views.setImageViewBitmap(R.id.moonViewWidget, resizedbitmap);
			//views.setTextViewText(R.id.moonwidgetLastUpdate, currentDateandTime);
		}

		/** apply an intent to the widget as a whole to launch the MainActivity */
		Intent intent = new Intent(context, ItemsActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.addWidgetContainer, pendingIntent);

		return views;
	}

	/**
	 * get a URL resource and return contents for further processing
	 * 
	 * @param urlString
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private static InputStream fetch(String urlString) throws MalformedURLException, IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}
}