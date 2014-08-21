package io.thedal;

import com.google.android.gms.location.LocationClient;

import android.location.Location;
import android.util.Log;

public class Logger implements Runnable {

  private boolean shouldRun = true;
  private LocationClient mLocationClient;
  private Location location;
  private String logName;
  private LogstrDataSource datasource;

  public Logger(LocationClient mLocationClient, String logName,
      LogstrDataSource datasource) {
    this.mLocationClient = mLocationClient;
    this.logName = logName;
    this.datasource = datasource;
  }

  @Override
  public void run() {
    String log;
    float accuracy;
    // Make a start of data entry
    datasource.createLog("START_OF_LOG." + logName);
    while (shouldRun) {
      Log.d("Logger", "Loop running!");
      location = mLocationClient.getLastLocation();
      accuracy = location.getAccuracy();
      Log.d("Logger", "Location accuracy: " + accuracy);
      log = logName + "|" + location.getAltitude() + "|"
          + location.getLatitude() + "|" + location.getLongitude() + "|"
          + location.getTime() + "|" + accuracy;
      Log.d("Logger", "Location received as: " + log);
      // TODO
      // Ignore if bad accuracy, else persist.
      datasource.createLog(log);

      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        shouldRun = false;
        Log.i("Logger", "Loop interrupted, stopping!");
        mLocationClient.disconnect();
        datasource.createLog("END_OF_LOG." + logName);
      }
      // Check if below is really required
      if (Thread.currentThread().isInterrupted()) {
        shouldRun = false;
        Log.i("Logger", "Loop stopping!");
        mLocationClient.disconnect();
        datasource.createLog("END_OF_LOG." + logName);
      }
    }
  }

}
