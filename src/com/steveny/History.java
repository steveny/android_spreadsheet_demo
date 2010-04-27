package com.steveny;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.steveny.Constants.TABLE_NAME;
import static com.steveny.Constants._ID;
import static com.steveny.Constants.NET_WORTH;
import static com.steveny.Constants.TIME;


public class History extends ListActivity {

    class RowAdapter extends CursorAdapter {

        private final LayoutInflater layoutInflater;

        public RowAdapter(Context context, Cursor c) {
            super(context, c);
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView timeView = (TextView) view.findViewById(R.id.time);
            TextView networthView = (TextView) view.findViewById(R.id.networth);

            double value = cursor.getDouble(2);
            if(Settings.isDemo(History.this)) {
              value = (value * Math.random())/Math.random();
            }

            String networth = NumberFormat.getCurrencyInstance().format(value);
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(cursor.getLong(1)));

            timeView.setText(time);
            networthView.setText(networth);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return layoutInflater.inflate(R.layout.item, parent, false);
        }
    }

    private NetWorthData netWorthData;

    private static String[] FROM = {_ID, TIME, NET_WORTH,};
    private static String ORDER_BY = TIME + " DESC";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        netWorthData = new NetWorthData(this);
        setContentView(R.layout.history);
        showHistory(getHistory());
    }

    private Cursor getHistory() {
        SQLiteDatabase db = netWorthData.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, FROM, null, null, null, null, ORDER_BY);
        startManagingCursor(cursor);
        return cursor;
    }

    private void showHistory(Cursor cursor) {
        setListAdapter(new RowAdapter(this, cursor));
    }
}
