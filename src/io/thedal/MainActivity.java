package io.thedal;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity implements
    GooglePlayServicesClient.ConnectionCallbacks,
    GooglePlayServicesClient.OnConnectionFailedListener {

  private static boolean running = false;
  private static Thread thread;
  private static LocationClient mLocationClient;
  private LogstrDataSource datasource;
  private static LocationRequest mLocationRequest;

  private static final int MILLISECONDS_PER_SECOND = 1000;
  public static final int UPDATE_INTERVAL_IN_SECONDS = 2;
  private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
      * UPDATE_INTERVAL_IN_SECONDS;
  private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
  private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
      * FASTEST_INTERVAL_IN_SECONDS;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mLocationRequest = LocationRequest.create();
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    mLocationRequest.setInterval(UPDATE_INTERVAL);
    mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
    mLocationClient = new LocationClient(this, this, this);
    datasource = new LogstrDataSource(this);
    datasource.open();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mLocationClient.connect();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /** Called when the user clicks the Sync button */
  public void syncData(View view) {
    Button syncButton = (Button) findViewById(R.id.button_sync);
    if (!syncButton.getText().equals("Sync")) {
      Log.i("MainActivity", "Resetting button to Sync");
      syncButton.setText("Sync");
    } else {
      Log.i("MainActivity", "Going to sync data to server");
      syncButton.setEnabled(false);
      List<io.thedal.Log> logs = datasource.getAllLogs();
      if ((logs != null) && (logs.size() > 0)) {
        ArrayList<String> list = new ArrayList<String>();
        for (io.thedal.Log log : logs) {
          list.add(log.getLog());
        }
        JSONArray jsArray = new JSONArray(list);
        Log.i("MainActivity", "Sending logs to server: " + jsArray);
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
          // post data
          Log.i("MainActivity", "Network available");
          if (!postDataToServer(jsArray)) {
            Log.i("MainActivity", "Error returned from server");
            flashBtnMsg(syncButton, "CErr");
          } else {
            // Successful post!
            Log.i("MainActivity", "Going to delete logs: " + logs.size());
            for (io.thedal.Log log : logs) {
              datasource.deleteLog(log);
            }
          }
        } else {
          // display error
          Log.i("MainActivity", "No Network!");
          flashBtnMsg(syncButton, "NNtw");
        }
      } else {
        // Display nothing to sync
        Log.i("MainActivity", "Nothing to sync");
        flashBtnMsg(syncButton, "NTSy");
      }
      syncButton.setEnabled(true);
    }
  }

  private boolean postDataToServer(JSONArray jsArray) {

    boolean postStatus = false;
    try {
      Log.d("MainActivity", "Attempting post");
      URL url = new URL("http://logstr.herokuapp.com/post");
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
          .permitAll().build();
      StrictMode.setThreadPolicy(policy);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setDoOutput(true);
      // Starts the query
      // conn.connect();
      DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
      wr.writeBytes(jsArray.toString());
      wr.flush();
      wr.close();
      int responseCode = conn.getResponseCode();
      Log.i("MainActivity", "Response Code : " + responseCode);
      if (responseCode == 200)
        postStatus = true;
    } catch (Exception e) {
      Log.d("MainActivity", "Unable to connect to server for post: " + e);
    }
    return postStatus;
  }

  private void flashBtnMsg(Button button, String string) {
    button.setText(string);
    Log.d("MainActivity", "Text in button: " + button.getText());
  }

  /** Called when the user clicks the Send button */
  public void sendMessage(View view) {
    // Do something in response to button
    Log.i("MainActivity", "Ok, Running: " + running);
    Button button = (Button) findViewById(R.id.button_send);
    EditText editText = (EditText) findViewById(R.id.edit_message);
    String logName = editText.getText().toString();
    Log.i("MainActivity", "Got msg: " + logName);
    String command = button.getText().toString();
    Log.i("MainActivity", "Got command: " + command);
    if (running) {
      // Stop
      thread.interrupt();
      running = false;
      button.setText("Start");
      editText.setText("");
      editText.setEnabled(true);
    } else {
      // Start
      // Check play services
      int resultCode = GooglePlayServicesUtil
          .isGooglePlayServicesAvailable(this);
      if (ConnectionResult.SUCCESS == resultCode) {
        if (logName == null) {
          logName = "";
        }
        Logger logger = new Logger(mLocationClient, logName + "."
            + System.currentTimeMillis(), datasource);
        mLocationClient.requestLocationUpdates(mLocationRequest, logger);
        thread = new Thread(logger);
        thread.start();
        running = true;
        editText.setEnabled(false);
        button.setText("Stop");
      } else {
        button.setText("Incompatible Device");
        Log.e("MainActivity", "Incompatible Device");
      }

    }
    Log.i("MainActivity", "Finally, Running: " + running);
  }

  @Override
  public void onConnectionFailed(ConnectionResult connResut) {
    Log.i("MainActivity", "Location Connection failed");
  }

  @Override
  public void onConnected(Bundle bundle) {
    Log.i("MainActivity", "Location Connection established");
  }

  @Override
  public void onDisconnected() {
    Log.i("MainActivity", "Location Connection disconnected");
  }

}
