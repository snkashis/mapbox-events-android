package com.mapbox.android.core.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sample LocationEngine using Google Play Services
 */
class GoogleLocationEngine extends LocationEngine implements
  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, Serializable {

  private static final LocationEnginePriority DEFAULT_PRIORITY = LocationEnginePriority.NO_POWER;

  private WeakReference<Context> context;
  private GoogleApiClient googleApiClient;
  private final Map<LocationEnginePriority, UpdateGoogleRequestPriority> REQUEST_PRIORITY = new
    HashMap<LocationEnginePriority, UpdateGoogleRequestPriority>() {
      {
        put(LocationEnginePriority.NO_POWER, new UpdateGoogleRequestPriority() {
          @Override
          public void update(LocationRequest request) {
            request.setPriority(LocationRequest.PRIORITY_NO_POWER);
          }
        });
        put(LocationEnginePriority.LOW_POWER, new UpdateGoogleRequestPriority() {
          @Override
          public void update(LocationRequest request) {
            request.setPriority(LocationRequest.PRIORITY_LOW_POWER);
          }
        });
        put(LocationEnginePriority.BALANCED_POWER_ACCURACY, new UpdateGoogleRequestPriority() {
          @Override
          public void update(LocationRequest request) {
            request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
          }
        });
        put(LocationEnginePriority.HIGH_ACCURACY, new UpdateGoogleRequestPriority() {
          @Override
          public void update(LocationRequest request) {
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
          }
        });
      }
    };

  private GoogleLocationEngine(Context context) {
    super();
    this.context = new WeakReference<>(context);
    googleApiClient = new GoogleApiClient.Builder(this.context.get())
      .addConnectionCallbacks(this)
      .addOnConnectionFailedListener(this)
      .addApi(LocationServices.API)
      .build();
    this.priority = DEFAULT_PRIORITY;
  }

  static synchronized LocationEngine getLocationEngine(Context context) {
    LocationEngine googleLocationEngine = new GoogleLocationEngine(context.getApplicationContext());

    return googleLocationEngine;
  }

  @Override
  public void activate() {
    connect();
  }

  @Override
  public void deactivate() {
    if (googleApiClient != null && googleApiClient.isConnected()) {
      googleApiClient.disconnect();
    }
  }

  @Override
  public boolean isConnected() {
    return googleApiClient.isConnected();
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    for (LocationEngineListener listener : locationListeners) {
      listener.onConnected();
    }
  }

  @Override
  public void onConnectionSuspended(int cause) {
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
  }

  @Override
  public Location getLastLocation() {
    if (googleApiClient.isConnected()) {
      //noinspection MissingPermission
      return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }

    //noinspection MissingPermission
    return null;
  }

  @Override
  public void requestLocationUpdates() {
    LocationRequest request = LocationRequest.create();

    if (interval != null) {
      request.setInterval(interval);
    }
    if (fastestInterval != null) {
      request.setFastestInterval(fastestInterval);
    }
    if (smallestDisplacement != null) {
      request.setSmallestDisplacement(smallestDisplacement);
    }

    updateRequestPriority(request);

    if (googleApiClient.isConnected()) {
      Log.e("test", "request: " + request);
      //noinspection MissingPermission
      Log.e("test", "Starting location updates");
      LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, getPendingIntent());
    }
  }

  @Override
  public void removeLocationUpdates() {
    if (googleApiClient.isConnected()) {
      LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, getPendingIntent());
    }
  }

  @Override
  public Type obtainType() {
    return Type.GOOGLE_PLAY_SERVICES;
  }

  @Override
  public void onLocationChanged(Location location) {
    for (LocationEngineListener listener : locationListeners) {
      listener.onLocationChanged(location);
    }
  }

  private void connect() {
    if (googleApiClient != null) {
      if (googleApiClient.isConnected()) {
        onConnected(null);
      } else {
        googleApiClient.connect();
      }
    }
  }

  private void updateRequestPriority(LocationRequest request) {
    REQUEST_PRIORITY.get(priority).update(request);
  }

  private PendingIntent getPendingIntent() {
    if (context.get() != null) {
      Intent intent = new Intent(context.get(), FusedLocationIntentService.class);
      intent.setAction(FusedLocationIntentService.ACTION_PROCESS_UPDATES);

      FusedLocationIntentService.addListeners(locationListeners);

      return PendingIntent.getService(context.get(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    return null;
  }
}
