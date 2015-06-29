package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import java.lang.String;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    DBHandler mDBhandler;
    //authority and path for the content provider
    public static final String AUTHORITY = "edu.buffalo.cse.cse486586.groupmessenger1.provider";
    public static final String PATH = "chat";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);
    public static int key = 0;
    Cursor cursor;
    SQLiteDatabase db;
    final String TAG = GroupMessengerProvider.class.getSimpleName();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        mDBhandler = new DBHandler(getContext());
        return false;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

        SQLiteDatabase db;
        try {
            db = mDBhandler.getWritableDatabase();

            //hack for insert as well as updating the table with single entry
            long rowId = db.insertWithOnConflict(DBHandler.TABLE_NAME, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
            Log.v("insert", values.toString());
            if(rowId == -1) {   //value already exists
                Log.i("Conflict", "Error inserting values in DB");

            }
        } catch (Exception e) {
            Log.v(TAG, "Exception while inserting");
        }
        return uri;
    }



    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */

        /*
        creating a cursor for manipulation
        @author prakashn
        reference: http://developer.android.com/training/basics/data-storage/databases.html
         */
        SQLiteDatabase db;
        try {
            db = mDBhandler.getReadableDatabase();
            // passing the selection key as arguments
            cursor = db.query(DBHandler.TABLE_NAME, projection, DBHandler.COL_NAME_KEY + "=?", new String[] { selection }, null, null, sortOrder);

        } catch (NullPointerException e) {
            Log.i(TAG, "null pointer exception om query");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }


        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }
}
