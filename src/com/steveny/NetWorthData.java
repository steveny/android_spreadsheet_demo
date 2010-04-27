package com.steveny;


import static android.provider.BaseColumns._ID;
import static com.steveny.Constants.TABLE_NAME;
import static com.steveny.Constants.NET_WORTH;
import static com.steveny.Constants.TIME;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NetWorthData extends SQLiteOpenHelper {
   private static final String DATABASE_NAME = "networth.db";
   private static final int DATABASE_VERSION = 1;

   public NetWorthData(Context ctx) {
      super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
   }

   @Override
   public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + _ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, " + NET_WORTH
            + " DECIMAL(9,2)," + TIME + " INTEGER NOT NULL);");
   }

   @Override
   public void onUpgrade(SQLiteDatabase db, int oldVersion,
         int newVersion) {
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
      onCreate(db);
   }
}