package com.kevinhinds.dontforget.alarmmanager;

import java.util.Random;

import com.kevinhinds.dontforget.ItemsActivity;
import com.kevinhinds.dontforget.R;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;

public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {

	public NotificationManager myNotificationManager;
	public static final String TITLE = "title";
	public static final String MESSAGE = "message";
	public static final CharSequence TICKERTEXT = "Don't Forget! Reminder";

	/**
	 * the main result of the alarm when its called
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Don't Forget Reminder");

		/** Acquire the lock */
		wl.acquire();

		/** You can do the processing here. */
		Bundle extras = intent.getExtras();

		/** Make sure this intent has been sent by the one-time timer button. */
		if (extras != null) {

			CharSequence NotificationTitle = extras.getString(TITLE);
			CharSequence NotificationContent = extras.getString(MESSAGE);

			if (!NotificationTitle.equals(null)) {
				NotificationManager myNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

				Notification notification = new Notification(R.drawable.ic_launcher, TICKERTEXT, System.currentTimeMillis());
				Intent notificationIntent = new Intent(context, ItemsActivity.class);
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
				notification.setLatestEventInfo(context, NotificationTitle, NotificationContent, contentIntent);

				/** we have more than one possible notification so we must have a random identifier for it */
				Random randomGenerator = new Random();
				int randomInt = randomGenerator.nextInt(100);
				myNotificationManager.notify(randomInt, notification);

				/** play the user's default notification sound */
				try {
					Uri notification1 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
					Ringtone r = RingtoneManager.getRingtone(context, notification1);
					r.play();
				} catch (Exception e) {
				}
			}
		}
		/** Release the lock */
		wl.release();
	}

	/**
	 * set a reminder for the future of a specific item
	 * 
	 * @param context
	 * @param title
	 *            title of the item for future reminder
	 * @param message
	 *            contents of the item for the future reminder
	 * @param futureTime
	 *            time in milliseconds into the future for the alarm to be set at
	 */
	public void setReminder(Context context, String title, String message, long futureTime) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
		intent.putExtra(TITLE, title);
		intent.putExtra(MESSAGE, message);
		/** make the intent unique by adding system time to it */
		intent.setData((Uri.parse("custom://" + System.currentTimeMillis())));
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		am.set(AlarmManager.RTC_WAKEUP, futureTime, pi);
	}

	/**
	 * example, set alarm for 5 seconds in the future
	 * 
	 * @param context
	 */
	public void SetAlarm(Context context) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
		intent.putExtra("onetime", Boolean.FALSE);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		/** After after 5 seconds */
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 5, pi);
	}

	/**
	 * example, cancel current alarm set in progress
	 * 
	 * @param context
	 */
	public void CancelAlarm(Context context) {
		Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}

	/**
	 * example, set a one time timer
	 * 
	 * @param context
	 */
	public void setOnetimeTimer(Context context) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
		intent.putExtra("onetime", Boolean.TRUE);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 5, pi);
	}
}