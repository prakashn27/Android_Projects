package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
    String TAG = SimpleDhtProvider.class.getSimpleName();
    final String NODE_JOIN = "node_join";
    final String SET_PREV_NEXT = "set_prev_next";
    final String INSERT_IN_HEAD = "insert_in_head";
    final String CHECK_NEXT = "check_next";
    final String QUERY_NEXT = "query_next";
    final String QUERY_FOUND_KEY = "query_found_key";
    final String QUERY_ALL = "query_all";
    final String QUERY_ALL_RESULT = "query_all_result";
    final String QUERY_HM_RESULT = "query_hm_result";
    final String[] ports = { "11108", "11112", "11116", "11120", "11124"};
    final String QUERY_CUSTOM = "query_custom";
    final String DELETE_NEXT = "delete_next";

    String avdNextName, avdPrevName;
    DBHandler mDBhandler;
    Cursor cursor;
    ContentResolver cr = null;
    String NodeId;  //stores the hash value of this emulator
    HashMap<String, String> localDB = new HashMap<String, String>();
    boolean starStop = false;   //indicated the all the data has been added to container when * is called
    private Uri mUri;

    static final int SERVER_PORT = 10000;
    String coordinatorPort = "11108";
    Node curNode = null;
    ArrayList<Node> avdList = new ArrayList<Node>();
    ArrayList<String> container = new ArrayList<String>();
    MatrixCursor mCursor = null;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        SQLiteDatabase db;
        db = mDBhandler.getWritableDatabase();
        String key = selection;
        try {
            Log.i(TAG, "key is " + key);
            //in only than one node
            if(avdPrevName == null && avdNextName == null)  { //if both are null insert in local
                if (selection.equals("\"@\"") || selection.equals("\"*\"") ) {
                    Log.i(TAG, "Delete: selecting all the data from the current AVD");
                    db.delete(DBHandler.TABLE_NAME, null, null);
                } else  {
                    try {
                        db = mDBhandler.getWritableDatabase();
                        localDB.remove(key);
                        db.delete(DBHandler.TABLE_NAME, DBHandler.COL_NAME_KEY + "=" + "'" + key + "'", null);

                    } catch (Exception e) {
                        Log.v(TAG, "Exception while deleting :" + e);
                    }
                }
            } else {
                String valueOfKey = genHash(key);
                String valueOfNext = genHash(avdNextName);
                String valueOfPrev = genHash(avdPrevName);
                int key_cur = valueOfKey.compareTo(NodeId);
                int cur_next = NodeId.compareTo(valueOfNext);
                int key_next = valueOfKey.compareTo(valueOfNext);
                int key_prev = valueOfKey.compareTo(valueOfPrev);
                int prev_cur = valueOfPrev.compareTo(NodeId);
                if (avdPrevName == avdNextName) {    //if only two

                    if (cur_next < 0) {
                        if (key_cur > 0 && key_next < 0) {
                            //send to next node
                            String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                            String msgToSend = DELETE_NEXT + ";;" + key + "==" + portToSend;
                            Log.i(TAG, "delete next  :" + msgToSend);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                        } else {
                            //delete local
                            try {
                                db = mDBhandler.getWritableDatabase();
                                localDB.remove(key);
                                db.delete(DBHandler.TABLE_NAME, DBHandler.COL_NAME_KEY + "=" + "'" + key + "'", null);
                            } catch (Exception e) {
                                Log.v(TAG, "Exception while inserting :" + e);
                            }
                        }
                    } else {
                        if (key_cur < 0 && key_next > 0) {
                            //local delete
                            try {
                                db = mDBhandler.getWritableDatabase();
                                localDB.remove(key);
                                db.delete(DBHandler.TABLE_NAME, DBHandler.COL_NAME_KEY + "=" + "'" + key + "'", null);
                            } catch (Exception e) {
                                Log.v(TAG, "Exception while inserting :" + e);
                            }
                        } else {
                            //send to next
                            String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                            String msgToSend = DELETE_NEXT + ";;" + key  + "==" + portToSend;
                            Log.i(TAG, "delete next  :" + msgToSend);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                        }
                    }
                } else if(key_cur > 0 && key_prev > 0 && prev_cur > 0) {
                    // insert local
                    try {
                        db = mDBhandler.getWritableDatabase();
                        localDB.remove(key);
                        db.delete(DBHandler.TABLE_NAME, DBHandler.COL_NAME_KEY + "=" + "'" + key + "'", null);
                    } catch (Exception e) {
                        Log.v(TAG, "Exception while inserting :" + e);
                    }

                } else if(key_cur < 0 && prev_cur >= 0) {
                    //local delete
                    try {
                        db = mDBhandler.getWritableDatabase();
                        localDB.remove(key);
                        db.delete(DBHandler.TABLE_NAME, DBHandler.COL_NAME_KEY + "=" + "'" + key + "'", null);
                    } catch (Exception e) {
                        Log.v(TAG, "Exception while inserting :" + e);
                    }

                } else if(key_cur < 0 && key_prev > 0) {
                    //local delete
                    try {
                        db = mDBhandler.getWritableDatabase();
                        localDB.remove(key);
                        db.delete(DBHandler.TABLE_NAME, DBHandler.COL_NAME_KEY + "=" + "'" + key + "'", null);
                    } catch (Exception e) {
                        Log.v(TAG, "Exception while inserting :" + e);
                    }
                } else {
                    //send to next
                    String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                    String msgToSend = DELETE_NEXT + ";;" + key + "==" + portToSend;
                    Log.i(TAG, "delete next  :" + msgToSend);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exception in insert " + e);
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        Log.i(TAG, "values are sent to insert " + values.toString());
        try {
            String key = (String) (values.get("key"));
            String value = (String) (values.get("value"));
            Log.i(TAG, "key is " + key + "; value is " + value);
            SQLiteDatabase db;


            //in only than one node
            if(avdPrevName == null && avdNextName == null)  { //if both are null insert in local
                try {
                    db = mDBhandler.getWritableDatabase();
                    localDB.put(key, value);
                    //hack for insert as well as updating the table with single entry
                    long rowId = db.insertWithOnConflict(DBHandler.TABLE_NAME, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                    Log.v("insert", "insert since one node" + values.toString());
                    if(rowId == -1) {   //value already exists
                        Log.i("Conflict", "Error inserting values in DB");
                    } else {
                        Log.i(TAG, "success fully inserted " + values.toString());
                    }
                } catch (Exception e) {
                    Log.v(TAG, "Exception while inserting :" + e);
                }
                return uri;
            } else {
                String valueOfKey = genHash(key);
                String valueOfNext = genHash(avdNextName);
                String valueOfPrev = genHash(avdPrevName);
                int key_cur = valueOfKey.compareTo(NodeId);
                int cur_next = NodeId.compareTo(valueOfNext);
                int key_next = valueOfKey.compareTo(valueOfNext);
                int key_prev = valueOfKey.compareTo(valueOfPrev);
                int prev_cur = valueOfPrev.compareTo(NodeId);
                if (avdPrevName == avdNextName) {    //if only two
                    if (cur_next < 0) {
                        if (key_cur > 0 && key_next < 0) {
                            //send to next node
                            String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                            String msgToSend = CHECK_NEXT + ";;" + key + ";;" + value + "==" + portToSend;
                            Log.i(TAG, "insert  :" + msgToSend);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);

                        } else {
                            //insert local
                            try {
                                db = mDBhandler.getWritableDatabase();
                                localDB.put(key, value);
                                //hack for insert as well as updating the table with single entry
                                long rowId = db.insertWithOnConflict(DBHandler.TABLE_NAME, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                                Log.v("insert", "insert since one node" + values.toString());
                                if(rowId == -1) {   //value already exists
                                    Log.i("Conflict", "Error inserting values in DB");
                                } else {
                                    Log.i(TAG, "success fully inserted " + values.toString());
                                }
                            } catch (Exception e) {
                                Log.v(TAG, "Exception while inserting :" + e);
                            }
                            return uri;
                        }
                    } else {
                        if (key_cur < 0 && key_next > 0) {
                            //local insert
                            try {
                                db = mDBhandler.getWritableDatabase();
                                localDB.put(key, value);
                                //hack for insert as well as updating the table with single entry
                                long rowId = db.insertWithOnConflict(DBHandler.TABLE_NAME, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                                Log.v("insert", "insert since one node" + values.toString());
                                if(rowId == -1) {   //value already exists
                                    Log.i("Conflict", "Error inserting values in DB");
                                } else {
                                    Log.i(TAG, "success fully inserted " + values.toString());
                                }
                            } catch (Exception e) {
                                Log.v(TAG, "Exception while inserting :" + e);
                            }
                            return uri;
                        } else {
                            //send to next
                            String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                            String msgToSend = CHECK_NEXT + ";;" + key + ";;" + value + "==" + portToSend;
                            Log.i(TAG, "insert  :" + msgToSend);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                        }
                    }
                } else if(key_cur > 0 && key_prev > 0 && prev_cur > 0) {
                    // insert local
                    try {
                        db = mDBhandler.getWritableDatabase();
                        localDB.put(key, value);
                        //hack for insert as well as updating the table with single entry
                        long rowId = db.insertWithOnConflict(DBHandler.TABLE_NAME, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                        Log.v("insert", "insert since one node" + values.toString());
                        if(rowId == -1) {   //value already exists
                            Log.i("Conflict", "Error inserting values in DB");
                        } else {
                            Log.i(TAG, "success fully inserted " + values.toString());
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "Exception while inserting :" + e);
                    }
                    return uri;

                } else if(key_cur < 0 && prev_cur >= 0) {
                    //local insert
                    try {
                        db = mDBhandler.getWritableDatabase();
                        localDB.put(key, value);
                        //hack for insert as well as updating the table with single entry
                        long rowId = db.insertWithOnConflict(DBHandler.TABLE_NAME, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                        Log.v("insert", "insert since one node" + values.toString());
                        if(rowId == -1) {   //value already exists
                            Log.i("Conflict", "Error inserting values in DB");
                        } else {
                            Log.i(TAG, "success fully inserted " + values.toString());
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "Exception while inserting :" + e);
                    }
                    return uri;

                } else if(key_cur < 0 && key_prev > 0) {
                    //local insert
                    try {
                        db = mDBhandler.getWritableDatabase();
                        localDB.put(key, value);
                        //hack for insert as well as updating the table with single entry
                        long rowId = db.insertWithOnConflict(DBHandler.TABLE_NAME, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                        Log.v("insert", "insert since one node" + values.toString());
                        if(rowId == -1) {   //value already exists
                            Log.i("Conflict", "Error inserting values in DB");
                        } else {
                            Log.i(TAG, "success fully inserted " + values.toString());
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "Exception while inserting :" + e);
                    }
                    return uri;
                } else {
                    String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                    String msgToSend = CHECK_NEXT + ";;" + key + ";;" + value + "==" + portToSend;
                    Log.i(TAG, "insert  :" + msgToSend);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exception in insert " + e);
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        try {
            mDBhandler = new DBHandler(getContext());
            cr =  (this.getContext()).getContentResolver();
            mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
            TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            String name = String.valueOf((Integer.parseInt(portStr)));
            String curPort = String.valueOf((Integer.parseInt(portStr) * 2));

            try {   //server task
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

            } catch (IOException e) {
                Log.e("Exception", "server Socket Exception");
            }
            NodeId = genHash(name);
            Log.i(TAG, "assigning cur NOde");
            if(curNode == null) {
                avdNextName = null;
                avdPrevName = null;
                Log.i(TAG, "if curNode is null get inside ");
                curNode = new Node(name, curPort, NodeId);
            }

            if(name.equals("5554")) {   //coordinator
                avdList.add(curNode);
                Log.i(TAG, "co ordinator node");
            } else {    //send the msg to coordinator
                String setNodeName =NODE_JOIN + ";;" + curPort + ";;" + name + "==" + coordinatorPort ;
                Log.i(TAG, setNodeName);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, setNodeName);
            }

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException in on create ");
        } catch (Exception e) {
            Log.e(TAG, "Exception in on create :" + e);
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase db;
        mCursor = new MatrixCursor(new String[] {DBHandler.COL_NAME_KEY, DBHandler.COL_NAME_VALUE});
        try {
            db = mDBhandler.getReadableDatabase();
            String key = selection;
//            if(avdPrevName == null && avdNextName == null) {    //only one node search local
//                if (selection.equals("\"@\"") || selection.equals("\"*\"")) {   //single avd is only there so get all
//                    cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                    Log.i(TAG, "Querying all from Single AVD");
//                } else {
//                    Log.i(TAG, "querying from single avd");
//                    cursor = db.query(DBHandler.TABLE_NAME, projection, DBHandler.COL_NAME_KEY + "=" + "'" + selection + "'", null, null, null, null);
//                }
//            }
//            else { //contains more than one AVD
//                String valueOfKey = genHash(key);
//                String valueOfNext = genHash(avdNextName);
//                String valueOfPrev = genHash(avdPrevName);
//                int key_cur = valueOfKey.compareTo(NodeId);
//                int cur_next = NodeId.compareTo(valueOfNext);
//                int key_next = valueOfKey.compareTo(valueOfNext);
//                int key_prev = valueOfKey.compareTo(valueOfPrev);
//                int prev_cur = valueOfPrev.compareTo(NodeId);
//                if (avdPrevName == avdNextName) { //only two avd
//                    if (cur_next < 0) {
//                        if (key_cur > 0 && key_next < 0) {
//                            //check to next node
//
//
//                        } else {
//                            //search local
//                            if (selection.equals("\"@\"")) {
//                                Log.i(TAG, "Query: selecting all the data from the current AVD");
//                                cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                            } else if (selection.equals("\"*\"")) {
//                                Log.i(TAG, "Query: selecting data from ALL the AVD"); //TODO: selecting data from all avd
//                                cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                            } else {
//                                Log.i(TAG, "Query: else part getting one key");
//                                Log.i(TAG, "Query: Selection:" + selection);
//                                cursor = db.query(DBHandler.TABLE_NAME, projection, DBHandler.COL_NAME_KEY + "=" + "'" + selection + "'", null, null, null, null);
//                            }
//                        }
//                    } else {
//                        if (key_cur < 0 && key_next > 0) {
//                            //search local
//                            if (selection.equals("\"@\"")) {
//                                Log.i(TAG, "Query: selecting all the data from the current AVD");
//                                cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                            } else if (selection.equals("\"*\"")) {
//                                Log.i(TAG, "Query: selecting data from ALL the AVD"); //TODO: selecting data from all avd
//                                cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                            } else {
//                                Log.i(TAG, "Query: else part getting one key");
//                                Log.i(TAG, "Query: Selection:" + selection);
//                                cursor = db.query(DBHandler.TABLE_NAME, projection, DBHandler.COL_NAME_KEY + "=" + "'" + selection + "'", null, null, null, null);
//                            }
//                        } else {
//                            //send to next
//
//                        }
//                    }
//                } else if(key_cur > 0 && key_prev > 0 && prev_cur > 0) {
//                    //search local
//                    if (selection.equals("\"@\"")) {
//                        Log.i(TAG, "Query: selecting all the data from the current AVD");
//                        cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                    } else if (selection.equals("\"*\"")) {
//                        Log.i(TAG, "Query: selecting data from ALL the AVD"); //TODO: selecting data from all avd
//                        cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                    } else {
//                        Log.i(TAG, "Query: else part getting one key");
//                        Log.i(TAG, "Query: Selection:" + selection);
//                        cursor = db.query(DBHandler.TABLE_NAME, projection, DBHandler.COL_NAME_KEY + "=" + "'" + selection + "'", null, null, null, null);
//                    }
//                } else if(key_cur < 0 && prev_cur >= 0) {
//                    //search local
//                    if (selection.equals("\"@\"")) {
//                        Log.i(TAG, "Query: selecting all the data from the current AVD");
//                        cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                    } else if (selection.equals("\"*\"")) {
//                        Log.i(TAG, "Query: selecting data from ALL the AVD"); //TODO: selecting data from all avd
//                        cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                    } else {
//                        Log.i(TAG, "Query: else part getting one key");
//                        Log.i(TAG, "Query: Selection:" + selection);
//                        cursor = db.query(DBHandler.TABLE_NAME, projection, DBHandler.COL_NAME_KEY + "=" + "'" + selection + "'", null, null, null, null);
//                    }
//                } else if(key_cur < 0 && key_prev > 0) {
//                    //search local
//                    if (selection.equals("\"@\"")) {
//                        Log.i(TAG, "Query: selecting all the data from the current AVD");
//                        cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                    } else if (selection.equals("\"*\"")) {
//                        Log.i(TAG, "Query: selecting data from ALL the AVD"); //TODO: selecting data from all avd
//                        cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                    } else {
//                        Log.i(TAG, "Query: else part getting one key");
//                        Log.i(TAG, "Query: Selection:" + selection);
//                        cursor = db.query(DBHandler.TABLE_NAME, projection, DBHandler.COL_NAME_KEY + "=" + "'" + selection + "'", null, null, null, null);
//                    }
//                } else {
//                    //send to next
//                }
//           }
//
        //old code with working 4
            if(avdPrevName == null && avdNextName == null) {    //only one node search local
                if (selection.equals("\"@\"") || selection.equals("\"*\"")) {   //since it is just a single node
                    Log.i(TAG, "Query: selecting all the data from the current AVD");
                    cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
                } else {    //select the key value pair
                    cursor = db.query(DBHandler.TABLE_NAME, projection, DBHandler.COL_NAME_KEY + "=" + "'" + selection + "'", null, null, null, null);
//                    if(localDB.containsKey(key)) {
//                        String value = localDB.get()
//                    }else {
//                        Log.e(TAG, "single node with value not present")
//                    }

                }
            } else {    //more than one node
                if (selection.equals("\"@\"")) {
                    Log.i(TAG, "Query: selecting all the data from the current AVD");
                    cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
                } else if (selection.equals("\"*\"")) {

                    Log.i(TAG, "Query: selecting data from ALL the AVD"); //TODO: selecting data from all avd
                    cursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);

                    //add it to the matrix cursor
                    cursor.moveToFirst();
                    for (int move = 0; move < cursor.getCount(); move++) {
                        String keyTemp = cursor.getString(cursor.getColumnIndex(DBHandler.COL_NAME_KEY));
                        String valueTemp = cursor.getString(cursor.getColumnIndex(DBHandler.COL_NAME_VALUE));
                        mCursor.addRow(new String[]{ keyTemp, valueTemp});
                        cursor.moveToNext();
                    }
                    //testing with multicast
                    for(String port : ports) {
//                        String portToSend = String.valueOf((Integer.parseInt(port) * 2));
                        String msgToSend = QUERY_CUSTOM + ";;" + curNode.port;
//                        String msgToSend = QUERY_CUSTOM + ";;" + curNode.port + "==" + portToSend;
                        Log.i(TAG, "query key-valuefor each loop  :" + msgToSend);
//                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port));
                        OutputStream outToAnother = socket.getOutputStream();
                        Log.i(TAG, "the port :" + port);
                        outToAnother.write(msgToSend.getBytes());
                        socket.close();
                    }
                    Thread.sleep(10000); //sleep for 4500 ms
                    //going into infinite loop;
//                    String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
//                    String msgToSend = QUERY_ALL + ";;" + curNode.port + "==" + portToSend;
//                    Log.i(TAG, "query key-value  :" + msgToSend);
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
//                    container.clear();
//                    while(!starStop) { //busy wait while all the items are added and returned to the container
//
//                    }
                    Log.i(TAG, "container is NOT empty;; size:" + container.size());
                    for (String ele : container) {
                        mCursor.addRow(ele.split(";;"));    //adding the elements in the cursor
                    }
                    Log.i(TAG, "matrix cursor count is :" + mCursor.getCount());
                    return mCursor;



                } else {
                    Log.i(TAG, "Query: else part getting one key");
                    Log.i(TAG, "Query: Selection:" + selection);
                    if (localDB.containsKey(key)) {      //contains the key
                        cursor = db.query(DBHandler.TABLE_NAME, projection, DBHandler.COL_NAME_KEY + "=" + "'" + selection + "'", null, null, null, null);
                    } else {    //search in the next node
                        String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                        String msgToSend = QUERY_NEXT + ";;" + key + ";;" + curNode.port + "==" + portToSend;
                        Log.i(TAG, "query key-value  :" + msgToSend);
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                        container.clear();  //empty the arraylist
                        while (container.isEmpty()) {
                            //wait till the elements are added to container
                        }

                        Log.i(TAG, "container is NOT empty");
                        for (String ele : container) {
                            mCursor.addRow(ele.split(";;"));    //adding the elements in the cursor
                        }
                        return mCursor;
                    }
                }
            }



        } catch (NullPointerException e) {
            Log.i(TAG, "null pointer exception om query " + e);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    class avdComparator implements Comparator<Node> {
        @Override
        public int compare(Node o1, Node o2) {
            return o1.hashValue.compareTo(o2.hashValue);
        }

    }
    class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        String TAG = ServerTask.class.getSimpleName();

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            Socket server;
            try {
                //while loop to repeatedly check for socket connectivity
                while (true) {
                    server = serverSocket.accept();
                    InputStream inFromAnother = server.getInputStream();
                    StringBuilder sb = new StringBuilder();
                    int value;
                    while ((value = inFromAnother.read()) != -1) {
                        char ch = (char) value;
                        sb.append(ch);
                    }
                    String msgFromStream = sb.toString();
                    Log.i("output", sb.toString());
                    String msgRead[] = msgFromStream.trim().split(";;");
                    String signal = msgRead[0];
                    switch (signal) {
                        case NODE_JOIN: //NODE_JOIN;;port;;name
                            try {
                                String port = msgRead[1];
                                String name = msgRead[2];
                                String remoteHash = genHash(name);  //generates hash
                                Node newNode = new Node(name, port, remoteHash);
                                Log.i(TAG, "new NOde is " + newNode.toString());
                                avdList.add(newNode);   //TODO: insert in seperate function with multicasting everything to others
                                Collections.sort(avdList, new avdComparator());
                                Log.i(TAG, "avd list is sorted with hashValue");
                                ///test insert
                                updatetoAllAVD();

                            } catch (NoSuchAlgorithmException e) {
                                Log.i(TAG, "exception in Node Join");
                            }
                            break;
                        case SET_PREV_NEXT: //TODO: set prev and next values if that is not same as old values
                            //SET_PREV_NEXT prev next
                            Log.i(TAG, "case set_prev_next  OLD prev node = " + curNode.prevName + "  next node :" + curNode.prevName);
                            curNode.prevName = msgRead[1];
                            avdPrevName = msgRead[1];
                            curNode.nextName = msgRead[2];
                            avdNextName = msgRead[2];
                            Log.i(TAG, "Avd name :" + curNode.curName + " updated the prev and next to " + msgRead[1] + "  " + msgRead[2]);
                            break;
                        case INSERT_IN_HEAD:    //INSERT_IN_HEAD + ";;" + key + ";;" + value + "==" + portToSend;
//                            ContentValues keyValueToInsert = new ContentValues();
//                            keyValueToInsert.put(DBHandler.COL_NAME_KEY, msgRead[1]);
//                            keyValueToInsert.put(DBHandler.COL_NAME_VALUE, msgRead[2]); //message
//                            Log.v("inserting value", keyValueToInsert.toString());
//                            Uri newUri = cr.insert(
//                                    buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider"),
//                                    keyValueToInsert
//                            );
//                            break;
                        case CHECK_NEXT:       //CHECK_NEXT + key + ";;" + value
                            try {
                                String keyRead = msgRead[1];
                                String valueRead = msgRead[2];
                                ContentValues cv = new ContentValues();
                                cv.put(DBHandler.COL_NAME_KEY, keyRead);
                                cv.put(DBHandler.COL_NAME_VALUE, valueRead); //message
                                Uri newUri = cr.insert(
                                    buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider"),
                                    cv
                                );
                            } catch (Exception e) {
                                Log.i(TAG, "Exception in CHECK_NEXT");
                            }
                            break;
                        case QUERY_NEXT: //QUERY_NEXT + ";;" + key  + ";;" + curNode.port + "==" + portToSend;
                            String key = msgRead[1];
                            String queriedPort = msgRead[2];
                            if(localDB.containsKey(key)) { //contains the key value pair send the key value pair to destination
                                String v = localDB.get(key);
                                String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                                String msgToSend = QUERY_FOUND_KEY + ";;" + key  + ";;" + v + "==" + queriedPort;
                                Log.i(TAG, "Query_next: found key  :" + msgToSend);
                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                            } else { //check the next port
                                String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                                String msgToSend = QUERY_NEXT + ";;" + key  + ";;" + queriedPort + "==" + portToSend;
                                Log.i(TAG, "Query_next: checkin the next port  :" + msgToSend);
                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                            }
                            break;
                        case QUERY_FOUND_KEY: //QUERY_FOUND_KEY + ";;" + key  + ";;" + v + "==" + queriedPort;
                            //add to the array list
                            String column1 = msgRead[1];
                            String column2 = msgRead[2];
                            container.add(column1 + ";;" + column2);
                            break;
                        case QUERY_ALL: //QUERY_ALL + ";;" + curNode.port + "==" + portToSend;
                            String originPort = msgRead[1];
                            Log.i(TAG, "values of remote and current port" + originPort + ":" + curNode.port);
                            if(originPort == curNode.port) { //reached the destination back so TERMINATE
                                Log.i(TAG, "start stop reached and set true ");
                                starStop = true;
                            } else {    //get all the elements and send it to the destination
                                SQLiteDatabase db = mDBhandler.getReadableDatabase();
                                String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                                String msgToSend = QUERY_ALL + ";;" + originPort + "==" + portToSend;
                                Log.i(TAG, "query ALL :" + msgToSend);
                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                                Cursor tempCursor = db.query(DBHandler.TABLE_NAME, null, null, null, null, null, null);
//                                Cursor tempCursor = cr.query(mUri, null, "@", null, null);   //get all the values from current db
                                tempCursor.moveToFirst();
                                String msgToOriginPort;
                                for (int move = 0; move < tempCursor.getCount(); move++) {
                                    //msg sent to orgin port
                                    msgToOriginPort = "";
                                    String keyTemp = tempCursor.getString(tempCursor.getColumnIndex(DBHandler.COL_NAME_KEY));
                                    String valueTemp = tempCursor.getString(tempCursor.getColumnIndex(DBHandler.COL_NAME_VALUE));
                                    msgToOriginPort = QUERY_ALL_RESULT + ";;" + keyTemp + ";;" + valueTemp + "==" + originPort;
                                    tempCursor.moveToNext();
                                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToOriginPort);
                                }
                                //send to the next port
//                                String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
//                                String msgToSend = QUERY_ALL + ";;" + remotePort + "==" + portToSend;
//                                Log.i(TAG, "query ALL :" + msgToSend);
//                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                            }
                            break;
                        case QUERY_ALL_RESULT:  //QUERY_ALL_RESULT + "::" + keyTemp + ";;" + valueTemp + "==" + remotePort;
                            String entry = msgRead[1] + ";;" + msgRead[2];
                            container.add(entry);
                            break;
                        case QUERY_CUSTOM:      // QUERY_CUSTOM + ";;" + curNode.port + "==" + portToSend;
                            String oPort = msgRead[1];
                            if(oPort != curNode.port) { //origin port need not be set
                                for (Map.Entry<String, String> map : localDB.entrySet()) {
                                    Log.i(TAG, " map values which are in * :" + map.getKey() + ";;" + map.getValue());
                                    String msgToOrigin = QUERY_HM_RESULT + ";;" + map.getKey() + ";;" + map.getValue();
//                                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToOrigin);
                                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(oPort));
                                    OutputStream outToAnother = socket.getOutputStream();
                                    Log.i(TAG, "the port :" + oPort);
                                    outToAnother.write(msgToOrigin.getBytes());
                                    socket.close();
                                }
                            }
                            break;
                        case QUERY_HM_RESULT: // QUERY_HM_RESULT + ";;" + map.getKey() + ";;" + map.getValue() + "==" + oPort;
                            String result = msgRead[1] + ";;" + msgRead[2];
                            Log.i(TAG, "results stored in the container is " + result);
                            container.add(result);
                            break;
                        case DELETE_NEXT:   //DELETE_NEXT + ";;" + key  + "==" + portToSend;
                            String keyToDelete = msgRead[1];
                            if(localDB.containsKey(keyToDelete)) {  //delete the current
                                SQLiteDatabase db = mDBhandler.getWritableDatabase();
                                localDB.remove(keyToDelete);
                                db.delete(DBHandler.TABLE_NAME, DBHandler.COL_NAME_KEY + "=" + "'" + keyToDelete + "'", null);
                            } else {    //check the next port
                                String portToSend = String.valueOf((Integer.parseInt(avdNextName) * 2));
                                String msgToSend = DELETE_NEXT + ";;" + keyToDelete + "==" + portToSend;
                                Log.i(TAG, "Delete_NEXT: checkin the next port  :" + msgToSend);
                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
                            }
                            break;

                    }
                }
            } catch (IOException e) {
                Log.e("TAG", "Server Socket creation failed");
            }
            return null;
        }
    }
    /*
   Builds the URI for content resolver
    */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    /*
    Update the prev and next for all avds
     */
    void updatetoAllAVD() {
        Log.i(TAG, "Updating to all avd");
        int len = avdList.size();
        for(int i = 0; i < len; i++) {
            Node curAvd = avdList.get(i);
            Node prevAvd = avdList.get((i + len - 1) % len);
            Node nextAvd = avdList.get((i+1) % len);
            Log.i(TAG, " Name :" + curAvd.curName + " ; prev :"+ prevAvd.curName + " ; next: "+ nextAvd.curName);
            String msgtoSend = SET_PREV_NEXT + ";;" + prevAvd.curName + ";;" + nextAvd.curName +"==" + curAvd.port;
            Log.i(TAG, "msg in Update all AVD :" + msgtoSend);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgtoSend);
        }
    }
    /*
     format :   signalType;;msgToSEnd;;Seperatedwitsemicolon==Port
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {

                //print socket address
                Log.i(TAG, "got the socket address");
                String[] msgWithPort = msgs[0].split("==");  //to get the port address
                String remotePort = msgWithPort[1];
                String dataToSend = msgWithPort[0];
                Log.i("msgWithPort", msgWithPort[0]);
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                OutputStream outToAnother = socket.getOutputStream();
                Log.i("Port Name", remotePort);
                outToAnother.write(dataToSend.getBytes());
                socket.close();

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
}
