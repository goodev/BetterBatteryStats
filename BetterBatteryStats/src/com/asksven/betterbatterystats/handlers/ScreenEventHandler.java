/*
 * Copyright (C) 2011 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterbatterystats.handlers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.SysUtils;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.services.EventWatcherService;
import com.asksven.betterbatterystats.services.WatchdogProcessingService;
import com.asksven.betterbatterystats.services.WriteScreenOffReferenceService;
import com.asksven.betterbatterystats.services.WriteScreenOnReferenceService;
import com.asksven.betterbatterystats.widgetproviders.LargeWidgetProvider;
import com.asksven.betterbatterystats.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * @author sven
 *
 */
public class ScreenEventHandler extends BroadcastReceiver
{

	private static final String TAG = "ScreenEventHandler";

    @Override
    public void onReceive(Context context, Intent intent)
    {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
		{
			Log.i(TAG, "Received Broadcast ACTION_SCREEN_OFF");
			boolean watchdogActive = sharedPrefs.getBoolean("ref_for_screen_off", false);
			boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);

			// if on kitkat make sure that we always collect screen on time: if no root then count the time
			if ( !rootEnabled && !SysUtils.hasBatteryStatsPermission(context) )
			{
				// total time since boot including time spent in sleep
				long elapsedRealtime = SystemClock.elapsedRealtime();
				
				// time screen went on
				long elapsedRealtimeScreenOn = sharedPrefs.getLong("time_screen_on", 0);
				long screenOnTime = sharedPrefs.getLong("screen_on_counter", 0);

				// add to te counter
				screenOnTime += (elapsedRealtime - elapsedRealtimeScreenOn);
				
		        SharedPreferences.Editor updater = sharedPrefs.edit();
		        updater.putLong("screen_on_counter", screenOnTime);
		        updater.commit();
			}

			if (watchdogActive)
			{
				// start service to persist reference
				Intent serviceIntent = new Intent(context, WriteScreenOffReferenceService.class);
				context.startService(serviceIntent);
			}

		}

        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
		{
			Log.i(TAG, "Received Broadcast ACTION_SCREEN_ON");
			boolean watchdogActive = sharedPrefs.getBoolean("ref_for_screen_off", false);
			boolean bRunOnUnlock = sharedPrefs.getBoolean("watchdog_on_unlock", false);
			boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);

			// if on kitkat make sure that we always collect screen on time: if no root then count the time
			if ( !rootEnabled && !SysUtils.hasBatteryStatsPermission(context) )
			{
				// total time since boot including time spent in sleep
				long elapsedRealtime = SystemClock.elapsedRealtime();
		        SharedPreferences.Editor updater = sharedPrefs.edit();
		        updater.putLong("time_screen_on", elapsedRealtime);
		        updater.commit();
			}
			
			if (watchdogActive && !bRunOnUnlock)
			{
				// start service to process watchdog
				Intent serviceIntent = new Intent(context, WatchdogProcessingService.class);
				context.startService(serviceIntent);

			}

			// Build the intent to update widgets
			Intent intentRefreshWidgets = new Intent(LargeWidgetProvider.WIDGET_UPDATE);
			context.sendBroadcast(intentRefreshWidgets);
			
		}
        
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT))
		{
			Log.i(TAG, "Received Broadcast ACTION_USER_PRESENT");
			boolean watchdogActive = sharedPrefs.getBoolean("ref_for_screen_off", false);		
			boolean bRunOnUnlock = sharedPrefs.getBoolean("watchdog_on_unlock", false);
			
			if (watchdogActive && bRunOnUnlock)
			{
				// start service to process watchdog
				Intent serviceIntent = new Intent(context, WatchdogProcessingService.class);
				context.startService(serviceIntent);

			}
			

		}

        Intent i = new Intent(context, EventWatcherService.class);
        context.startService(i);
    }
    
}