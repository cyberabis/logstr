package io.thedal;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;

import android.location.Location;
import android.util.Log;

public class Logger implements Runnable, LocationListener {

  private boolean shouldRun = true;
  private LocationClient mLocationClient;
  private Location prevLocation;
  private String logName;
  private LogstrDataSource datasource;
  private float totalDistance = 0;

  public Logger(LocationClient mLocationClient, String logName,
      LogstrDataSource datasource) {
    this.mLocationClient = mLocationClient;
    this.logName = logName;
    this.datasource = datasource;
  }

  @Override
  public void run() {

    // Make a start of data entry
    datasource.createLog("START_OF_LOG." + logName);
    while (shouldRun) {
      Log.d("Logger", "Loop running!");
      try {
        // Just sleep, the lister method will take care!
        Thread.sleep(300000);
      } catch (InterruptedException e) {
        shouldRun = false;
        Log.i("Logger", "Loop interrupted, stopping!");
        mLocationClient.removeLocationUpdates(this);
        // mLocationClient.disconnect();
        datasource.createLog("END_OF_LOG." + logName);
      }
      // Check if below is really required
      if (Thread.currentThread().isInterrupted()) {
        shouldRun = false;
        Log.i("Logger", "Loop stopping!");
        mLocationClient.removeLocationUpdates(this);
        // mLocationClient.disconnect();
        datasource.createLog("END_OF_LOG." + logName);
      }
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    float[] distanceResult = new float[3];
    // Calc distance
    if (prevLocation != null) {
      Location.distanceBetween(prevLocation.getLatitude(),
          prevLocation.getLongitude(), location.getLatitude(),
          location.getLongitude(), distanceResult);
    }
    totalDistance = totalDistance + distanceResult[0];
    String log;
    float accuracy;
    accuracy = location.getAccuracy();
    log = logName + "|" + accuracy + "|" + location.getAltitude() + "|"
        + location.getLatitude() + "|" + location.getLongitude() + "|"
        + location.getTime() + "|" + distanceResult[0] + "|" + totalDistance;
    Log.d("Logger", "Location received as: " + log);
    datasource.createLog(log);
    prevLocation = location;
  }

}
