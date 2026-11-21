package com.classmute.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class ClassItem {

    String name;
    // index 0 is monday, 6 is sunday
    boolean[] days = new boolean[7];
    int startMinutes;
    int endMinutes;

    boolean isHappeningNow() {
        Calendar now = Calendar.getInstance();
        int today = Scheduler.getDayIndex(now);
        int minutesNow = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return days[today] && minutesNow >= startMinutes && minutesNow < endMinutes;
    }

    String getDaysText() {
        String[] names = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String text = "";
        for (int i = 0; i < 7; i++) {
            if (days[i]) {
                if (!text.isEmpty()) {
                    text = text + ", ";
                }
                text = text + names[i];
            }
        }
        return text;
    }

    String getTimeText() {
        return formatTime(startMinutes) + " - " + formatTime(endMinutes);
    }

    static String formatTime(int minutes) {
        int hour = minutes / 60;
        int minute = minutes % 60;
        String amPm = "AM";
        if (hour >= 12) {
            amPm = "PM";
        }
        hour = hour % 12;
        if (hour == 0) {
            hour = 12;
        }
        return String.format(Locale.US, "%d:%02d %s", hour, minute, amPm);
    }

    JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("start", startMinutes);
        obj.put("end", endMinutes);
        JSONArray dayArray = new JSONArray();
        for (int i = 0; i < 7; i++) {
            dayArray.put(days[i]);
        }
        obj.put("days", dayArray);
        return obj;
    }

    static ClassItem fromJson(JSONObject obj) throws JSONException {
        ClassItem item = new ClassItem();
        item.name = obj.getString("name");
        item.startMinutes = obj.getInt("start");
        item.endMinutes = obj.getInt("end");
        JSONArray dayArray = obj.getJSONArray("days");
        for (int i = 0; i < 7; i++) {
            item.days[i] = dayArray.getBoolean(i);
        }
        return item;
    }
}
