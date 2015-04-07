package com.example.pistoppr.pitstoppr;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.GeoDataApi;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
A trip has been started when this activity is active

Updates user location every UPDATE_INTERVAL_IN_MILLISECONDS
On location changed:
 */
public class TripActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected static final String TAG = "location-updates-sample";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    public AutocompleteFilter mAutoCompleteFilter;
    /**
     * Radius from current location to search for preferred restaurants (in meters)
     */
    private static int searchRadius = 5;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    private String debuggerString;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";


    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        mRequestingLocationUpdates = true;
        mLastUpdateTime = "";

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_trip, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }


    /**
     * @return the users preferred restaurants as a string of the format:
     *  restaurant1Name|restaurant2Name|...
     */
    private String getPreferredRestaurants(){
        Set<String> defaultRestaurants = new HashSet<String>();
        SharedPreferences restaurantPreferences = getSharedPreferences("restaurantPrefs", MODE_PRIVATE);
        Set<String> mySetOfRestaurants = restaurantPreferences.getStringSet("restaurants", defaultRestaurants);
        String returnString = "";
        for (String restaurant: mySetOfRestaurants){
            returnString += restaurant + "|";
        }
        //return returnString.substring(0,returnString.length() - 1);
        return "food";
    }

    private Set<String> getPreferredRestaurantSet(){
        Set<String> defaultRestaurants = new HashSet<String>();
        SharedPreferences restaurantPreferences = getSharedPreferences("restaurantPrefs", MODE_PRIVATE);
        Set<String> mySetOfRestaurants = restaurantPreferences.getStringSet("restaurants", defaultRestaurants);
        return mySetOfRestaurants;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return getRestaurantHTTP(urls[0]);
            } catch (JSONException e) {
                return "JSONException";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            debuggerString = result;
            System.out.println(result);
        }
    }

    private String getRestaurantHTTP(String url) throws JSONException {
        JSONArray results = null;
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
        HttpConnectionParams.setSoTimeout(httpParams, 30000);
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Content-type", "application/json");
        ResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = (String) httpClient.execute(httpGet, responseHandler);
        } catch (IOException e) {
            return e.toString();
        }
        if (response != null) {
            JSONObject mResult = new JSONObject(response);
            results = mResult.getJSONArray("results");
        }
        if (results != null){
            launchNotification(results);
        }
        return response;
    }

    private void notifyUsersWithHttp(){
        String restaurantChoices = getPreferredRestaurants();
        String mapsAPIKey = "AIzaSyCcWJIhDLElL5-YA-VkMtDOcsfkaaXC10U";
        try {
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+ mCurrentLocation.getLatitude()+"," +mCurrentLocation.getLongitude() + "&radius=" + searchRadius + "&types=food&key=" + mapsAPIKey;
            String url2 = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+ mCurrentLocation.getLatitude()+"," +mCurrentLocation.getLongitude() + "&radius=" + searchRadius + "&rankBy=distance&types="+ URLEncoder.encode(restaurantChoices, "UTF-8")+"&sensor=true&key=" + mapsAPIKey;
            new HttpAsyncTask().execute(url);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a change in latitude from a given search radius
     */
    private double getDeltaLatitude(){
        return searchRadius/110.54;
    }

    private double getDeltaLongitude(){
        double cosLat = Math.cos(mCurrentLocation.getLatitude());
        cosLat = cosLat*111.320;
        return searchRadius/cosLat;
    }

    private LatLngBounds getLatLngBounds(){
        double deltaLat = getDeltaLatitude();
        double deltaLon = getDeltaLongitude();
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(new LatLng(mCurrentLocation.getLatitude() + deltaLat, mCurrentLocation.getLongitude()))
                .include(new LatLng(mCurrentLocation.getLatitude() - deltaLat, mCurrentLocation.getLongitude()))
                .include(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude() + deltaLon))
                .include(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude() - deltaLon))
                .build();
        return bounds;
    }

    private void notifyUsersWithPlacesApi(){
        Set<String> restaurantSet = getPreferredRestaurantSet();
        String query = "chipotle";
        LatLngBounds mBounds = getLatLngBounds();
        mAutoCompleteFilter = AutocompleteFilter.create(new HashSet<Integer>());
        PendingResult<AutocompletePredictionBuffer> result = Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, query, mBounds, mAutoCompleteFilter);
        result.setResultCallback(mUpdatePlaceDetailsCallback);
    }

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<AutocompletePredictionBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<AutocompletePredictionBuffer>() {
        @Override
        public void onResult(AutocompletePredictionBuffer buffer) {
            if (!buffer.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(TAG, "Place query did not complete. Error: " + buffer.getStatus().toString());

                return;
            }
            // Get the Place object from the buffer.
            final AutocompletePrediction place = buffer.get(0);
            String placeId = place.getPlaceId();
            Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
                    .setResultCallback(new ResultCallback<PlaceBuffer>() {
                        @Override
                        public void onResult(PlaceBuffer places) {
                            if (places.getStatus().isSuccess()) {
                                final Place myPlace = places.get(0);
                                launchNotificationFromPlaces(myPlace);
                            }
                            places.release();
                        }
                    });
        }
    };


    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //notifyUsersWithHttp();
        //notifyUsersWithPlacesApi();
        mockLaunchNotification();
        //If restaurantResults != null, check if empty. If not empty, give notification
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    private void launchNotificationFromPlaces(Place myPlace) {
        NotificationCompat.Builder mBuilder;

        CharSequence restaurantName = myPlace.getName();
        LatLng latlng = myPlace.getLatLng();
        Double myLat = latlng.latitude;
        Double myLong = latlng.longitude;
        if (restaurantName != null && myLat != null && myLong != null){
            mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("Restaurant Ahead!")
                    .setContentText(restaurantName + ". Click on me to go!");

            String uri = String.format("http://maps.google.com/maps?daddr=%f,%f", myLat, myLong);
            Intent resultIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(uri));
            resultIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");

            // Because clicking the notification opens a new ("special") activity, there's
            // no need to create an artificial back stack.
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            // Sets an ID for the notification
            int mNotificationId = 001;
            // Gets an instance of the NotificationManager service
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

    //TODO
    //Currently being used
    private void mockLaunchNotification(){
        NotificationCompat.Builder mBuilder;
        //TODO check if chipotle
        Set<String> preferredRestaurantSet = getPreferredRestaurantSet();
        String restaurantName = "";
        Double destinationLatitude;
        Double destinationLongitude;
        if (preferredRestaurantSet.contains("chipotle")){
            restaurantName = "Chipotle";
            destinationLatitude = 42.362576;
            destinationLongitude = -71.085349;
        } else if (preferredRestaurantSet.contains("starbucks")) {
            restaurantName = "Starbucks";
            destinationLatitude = 42.3624239;
            destinationLongitude = -71.0877053;
        } else if (preferredRestaurantSet.contains("cosi")){
            restaurantName = "Cosi";
            destinationLatitude = 42.362521;
            destinationLongitude = -71.0876597;
        } else if (preferredRestaurantSet.contains("dunkin donuts")){
            restaurantName = "Dunkin' Donuts";
            destinationLatitude = 42.3599624;
            destinationLongitude = -71.0957251;
        } else if (preferredRestaurantSet.contains("subway")){
            restaurantName = "Subway";
            destinationLatitude = 42.3606192;
            destinationLongitude = -71.0967217;
        } else if (preferredRestaurantSet.contains("sebastians")){
            restaurantName = "Sebastians";
            destinationLatitude = 42.3628686;
            destinationLongitude = -71.0891123;
        }
        else {
            return;
        }
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Restaurant Ahead!")
                .setContentText(restaurantName + ". Click on me to go!");

        String uri = String.format("http://maps.google.com/maps?daddr=%f,%f", destinationLatitude, destinationLongitude);
        Intent resultIntent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse(uri));
        resultIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        // Sets an ID for the notification
        int mNotificationId = 001;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }


    private void launchNotification(JSONArray arr) {
        NotificationCompat.Builder mBuilder;
        JSONObject jsonRestaurant;
        try {
            jsonRestaurant = arr.getJSONObject(0);
            String restaurantName = jsonRestaurant.getString("name");
            JSONObject restaurantGeometry = jsonRestaurant.getJSONObject("geometry");
            Double destinationLatitude = restaurantGeometry.getDouble("lat");
            Double destinationLongitude = restaurantGeometry.getDouble("lng");
            if (restaurantName != null && destinationLatitude != null && destinationLongitude != null){
                mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Restaurant Ahead!")
                        .setContentText(restaurantName + ". Click on me to go!");

                String uri = String.format("http://maps.google.com/maps?daddr=%f,%f", destinationLatitude, destinationLongitude);
                Intent resultIntent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(uri));
                resultIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");

                // Because clicking the notification opens a new ("special") activity, there's
                // no need to create an artificial back stack.
                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                this,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);
                // Sets an ID for the notification
                int mNotificationId = 001;
                // Gets an instance of the NotificationManager service
                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                // Builds the notification and issues it.
                mNotifyMgr.notify(mNotificationId, mBuilder.build());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    /** Called when the user clicks the End Trip Button */
    public void endTrip(View view){
        //Brings back to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Settings Button */
    public void openSettings(View view){
        //Opens the settings
        Intent intent = new Intent(this, Settings2Activity.class);
        startActivity(intent);
    }

    public void displayText(View view) {
        Set<String> defaultRestaurants = new HashSet<String>();
        SharedPreferences restaurantPreferences = getSharedPreferences("restaurantPrefs", MODE_PRIVATE);
        Set<String> mySetOfRestaurants = restaurantPreferences.getStringSet("restaurants", defaultRestaurants);
        StringBuilder restaurantString = new StringBuilder();
        for (String res : mySetOfRestaurants) {
            restaurantString.append(res);
            restaurantString.append("\r\n");
        }
        String locationString = mCurrentLocation.toString();
        TextView restaurantTextView = (TextView) findViewById(R.id.tripTextView);
        restaurantTextView.setText(debuggerString);
    }
}
