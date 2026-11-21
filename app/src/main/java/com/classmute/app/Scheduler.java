package com.classmute.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;

import java.util.ArrayList;
import java.util.Calendar;

public class Scheduler {

    // gets called when the alarm goes off, after a reboot, and whenever the user changes something
    static void refresh(Context context) {
        scheduleNextAlarm(context);

        SharedPreferences prefs = ClassStore.getPrefs(context);
        boolean weSilenced = prefs.getBoolean("we_silenced", false);
        boolean shouldBeSilent = ClassStore.isEnabled(context) && getCurrentClass(context) != null;

        if (shouldBeSilent && !weSilenced) {
            silencePhone(context);
        } else if (!shouldBeSilent && weSilenced) {
            restorePhone(context);
        }
    }

    static ClassItem getCurrentClass(Context context) {
        for (ClassItem item : ClassStore.load(context)) {
            if (item.isHappeningNow()) {
                return item;
            }
        }
        return null;
    }

    // java's calendar says sunday = 1 and saturday = 7 but our week starts on monday
    static int getDayIndex(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        return (day + 5) % 7;
    }

    static long getTimeOnDay(Calendar day, int minutes) {
        Calendar time = (Calendar) day.clone();
        time.set(Calendar.HOUR_OF_DAY, minutes / 60);
        time.set(Calendar.MINUTE, minutes % 60);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        return time.getTimeInMillis();
    }

    // finds the next time a class starts or ends
    static long findNextChange(Context context) {
        ArrayList<ClassItem> classes = ClassStore.load(context);
        long now = System.currentTimeMillis();
        long next = -1;

        // check the next 8 days, thats enough even if a class is only once a week
        for (int i = 0; i < 8; i++) {
            Calendar day = Calendar.getInstance();
            day.add(Calendar.DAY_OF_YEAR, i);
            int dayIndex = getDayIndex(day);

            for (ClassItem item : classes) {
                if (!item.days[dayIndex]) {
                    continue;
                }
                long start = getTimeOnDay(day, item.startMinutes);
                long end = getTimeOnDay(day, item.endMinutes);

                if (start > now && (next == -1 || start < next)) {
                    next = start;
                }
                if (end > now && (next == -1 || end < next)) {
                    next = end;
                }
            }
        }
        return next;
    }

    static void scheduleNextAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ClassAlarmReceiver.class);
        // FLAG_IMMUTABLE is required on newer android or it crashes
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);

        if (!ClassStore.isEnabled(context)) {
            return;
        }
        long next = findNextChange(context);
        if (next == -1) {
            return;
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent);
        } catch (SecurityException e) {
            // no exact alarm permision. this one might be a few minutes late but its better then nothing
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent);
        }
    }

    static boolean hasDndAccess(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager.isNotificationPolicyAccessGranted();
    }

    static void silencePhone(Context context) {
        if (!hasDndAccess(context)) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // remember how the phone was before class so we can put it back after
        ClassStore.getPrefs(context).edit()
                .putInt("old_ringer", audioManager.getRingerMode())
                .putInt("old_filter", notificationManager.getCurrentInterruptionFilter())
                .putBoolean("we_silenced", true)
                .apply();

        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        // alarms only, so your morning alarm still works but nothing else makes noise
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS);
    }

    static void restorePhone(Context context) {
        SharedPreferences prefs = ClassStore.getPrefs(context);
        prefs.edit().putBoolean("we_silenced", false).apply();

        if (!hasDndAccess(context)) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        int oldFilter = prefs.getInt("old_filter", NotificationManager.INTERRUPTION_FILTER_ALL);
        int oldRinger = prefs.getInt("old_ringer", AudioManager.RINGER_MODE_NORMAL);
        if (oldFilter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
            oldFilter = NotificationManager.INTERRUPTION_FILTER_ALL;
        }

        notificationManager.setInterruptionFilter(oldFilter);
        audioManager.setRingerMode(oldRinger);
    }

    static String getStatusText(Context context) {
        if (!ClassStore.isEnabled(context)) {
            return "Auto-silent is off";
        }
        if (ClassStore.load(context).isEmpty()) {
            return "Add your classes and your phone will go quiet by itself";
        }
        ClassItem current = getCurrentClass(context);
        if (current != null) {
            return "In " + current.name + " right now. Silent until " + ClassItem.formatTime(current.endMinutes);
        }
        return getNextClassText(context);
    }

    static String getNextClassText(Context context) {
        ArrayList<ClassItem> classes = ClassStore.load(context);
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        long now = System.currentTimeMillis();

        for (int i = 0; i < 8; i++) {
            Calendar day = Calendar.getInstance();
            day.add(Calendar.DAY_OF_YEAR, i);
            int dayIndex = getDayIndex(day);

            // list is sorted by start time already so the first match is the earliest
            for (ClassItem item : classes) {
                if (item.days[dayIndex] && getTimeOnDay(day, item.startMinutes) > now) {
                    String when;
                    if (i == 0) {
                        when = "today";
                    } else if (i == 1) {
                        when = "tomorrow";
                    } else {
                        when = "on " + dayNames[dayIndex];
                    }
                    return "Next up: " + item.name + " " + when + " at " + ClassItem.formatTime(item.startMinutes);
                }
            }
        }
        return "No upcoming classes";
    }
}
