package com.classmute.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ClassStore {

    static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("classmute", Context.MODE_PRIVATE);
    }

    static ArrayList<ClassItem> load(Context context) {
        ArrayList<ClassItem> list = new ArrayList<>();
        String saved = getPrefs(context).getString("classes", "[]");

        try {
            JSONArray array = new JSONArray(saved);
            for (int i = 0; i < array.length(); i++) {
                list.add(ClassItem.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // shouldnt happen but if the saved data is broken just start fresh
            e.printStackTrace();
        }

        // sort by start time so the list looks nice
        Collections.sort(list, new Comparator<ClassItem>() {
            @Override
            public int compare(ClassItem a, ClassItem b) {
                return a.startMinutes - b.startMinutes;
            }
        });
        return list;
    }

    static void save(Context context, ArrayList<ClassItem> list) {
        JSONArray array = new JSONArray();
        try {
            for (ClassItem item : list) {
                array.put(item.toJson());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getPrefs(context).edit().putString("classes", array.toString()).apply();
    }

    static boolean isEnabled(Context context) {
        return getPrefs(context).getBoolean("enabled", true);
    }

    static void setEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean("enabled", enabled).apply();
    }
}
