package edu.buffalo.cse.cse486586.groupmessenger1;

import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.TextView;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.os.AsyncTask;
import java.net.ServerSocket;
import java.net.Socket;
import android.util.Log;
import android.view.View.OnClickListener;
import android.content.ContentValues;
import android.net.Uri;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Scanner;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final int SERVER_PORT = 10000;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    final String[] ports = { "11108", "11112", "11116", "11120", "11124"};
    public static int keyIndex = 0; //to set the values for the KEY in insert operation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
         /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that professor came up with to get around the networking limitations of AVDs.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            /*
            AsyncTask.THREAD_POOL_EXECUTOR : An Executor that can be used to execute tasks in
                                            parallel.

             */
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        String msg = editText.getText().toString() + "\n";
                        editText.setText("");
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    }
                }
        );

    }

    @Override
    protected void onStop() {

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        String TAG = ServerTask.class.getSimpleName();

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TO_DO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             * param = ServerSocket
             * progress = String
             * result = void
             *
             * onProgressUpdate gets called on call to publishProgress call
             * reference: http://developer.android.com/reference/android/os/AsyncTask.html
             *
             * @author: PrakashN
             */

            //Waits for an incoming request and blocks until the connection is opened.
            Socket server;
            try {
                //while loop to repeatedly check for socket connectivity
                while(true) {
                    server = serverSocket.accept();
                    InputStream inFromAnother = server.getInputStream();
                    StringBuilder sb = new StringBuilder();
                    int value;
                    while((value = inFromAnother.read()) != -1) {
                        char ch = (char)value;
                        sb.append(ch);
                    }
                    publishProgress(sb.toString());
                    server.close();
                    inFromAnother.close();
                }
            } catch (IOException e) {
                Log.e("TAG", "Server Socket creation failed");
            }

            return null;
        }

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            //save it in the DB
            //write it to text view TextView
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\n");
            //reference : code from PA2 ref

            ContentValues keyValueToInsert = new ContentValues();

            // inserting <”key-to-insert”, “value-to-insert”>
            keyValueToInsert.put(DBHandler.COL_NAME_KEY, Integer.toString(keyIndex++));
            keyValueToInsert.put(DBHandler.COL_NAME_VALUE, strReceived);
            Log.i(TAG, "key value is " + keyValueToInsert.get("key") + "  value is " + keyValueToInsert.get("value"));
            Uri newUri = getContentResolver().insert(buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider"),
                    keyValueToInsert
            );

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "chatFile";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {


            Socket socket;
            for(String remotePort : ports) {
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                    Log.e(TAG, "sent to the port" + remotePort);
                    String msgToSend = msgs[0];

                    //Set the output Stream of the socket
                    OutputStream outToAnother = socket.getOutputStream();
                    outToAnother.write(msgToSend.getBytes());

                    socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
            }

            return null;
        }
    }
}
