package com.steveny;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

    public static String getRow(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("row", "");
    }

    public static String getColumn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("column", "");
    }

    public static String getPassword(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("google_pass", "");
    }

    public static String getEmail(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("google_id", "");
    }

    public static String getSpreadsheet(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("spreadsheet", "");
    }

    public static boolean isDemo(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("demo_mode", false);
    }

    public static boolean isSettingsProvided(Context context) {
        return !getSpreadsheet(context).equals("") && !getRow(context).equals("") && !getColumn(context).equals("") && !getPassword(context).equals("") && !getEmail(context).equals("");
    }

}