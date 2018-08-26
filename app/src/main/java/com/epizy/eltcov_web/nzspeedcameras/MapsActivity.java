package com.epizy.eltcov_web.nzspeedcameras;

        import android.Manifest;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.pm.ActivityInfo;
        import android.content.pm.PackageManager;
        import android.graphics.Color;
        import android.location.Location;
        import android.media.Ringtone;
        import android.media.RingtoneManager;
        import android.net.Uri;
        import android.os.Build;
        import android.os.Bundle;
        import android.os.CountDownTimer;
        import android.os.Looper;
        import android.support.v4.app.ActivityCompat;
        import android.support.v4.content.ContextCompat;
        import android.support.v7.app.AlertDialog;
        import android.support.v7.app.AppCompatActivity;
        import android.util.Log;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.android.volley.Request;
        import com.android.volley.RequestQueue;
        import com.android.volley.Response;
        import com.android.volley.VolleyError;
        import com.android.volley.toolbox.JsonObjectRequest;
        import com.android.volley.toolbox.Volley;
        import com.google.android.gms.location.FusedLocationProviderClient;
        import com.google.android.gms.location.LocationCallback;
        import com.google.android.gms.location.LocationRequest;
        import com.google.android.gms.location.LocationResult;
        import com.google.android.gms.location.LocationServices;
        import com.google.android.gms.maps.CameraUpdate;
        import com.google.android.gms.maps.CameraUpdateFactory;
        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.OnMapReadyCallback;
        import com.google.android.gms.maps.SupportMapFragment;
        import com.google.android.gms.maps.model.BitmapDescriptorFactory;
        import com.google.android.gms.maps.model.Circle;
        import com.google.android.gms.maps.model.CircleOptions;
        import com.google.android.gms.maps.model.LatLng;
        import com.google.android.gms.maps.model.Marker;
        import com.google.android.gms.maps.model.MarkerOptions;

        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.util.ArrayList;
        import java.util.Iterator;
        import java.util.List;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    static int CAMERA_QUANTITY = 100;
    static int SEARCH_RADIUS = 50000;
    static double CAMERA_RADIUS = 200;
    static String MAP_LAYER = "242"; // Layer for speed cameras
    static double EARTH_RADIUS = 6366000;

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    List<Circle> mCamerasCircles = new ArrayList<>();
    List<LatLng> mCamerasLocations = new ArrayList<>();
    FusedLocationProviderClient mFusedLocationClient;
    double mDistance;
    TextView mCamInfo;
    TextView mLocInfo;

    Toast mAlert;

    RequestQueue queue = null;

    public RequestQueue getRequestQueue(Context context)
    {
        if(queue == null)
        {
            queue = Volley.newRequestQueue(this);
        }

        return queue;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getSupportActionBar().setTitle("Speed cameras");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        queue = getRequestQueue(getApplicationContext());

        mCamInfo = findViewById(R.id.caminfo);
        mLocInfo = findViewById(R.id.locinfo);
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); // five seconds interval
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Getting location accessed
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Trying to get access
                checkLocationPermission();
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());


                //Current location marker was needed for tests, actual app uses
                // setMyLocationEnabled to display user's location

                /*if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);*/



                //Get cameras coordinates for current position
                getCameras(String.valueOf(latLng.latitude), String.valueOf(latLng.longitude));


                //Clear old circles from the map
                for (Iterator<Circle> i = mCamerasCircles.iterator(); i.hasNext();) {
                    i.next().remove();
                }
                mCamerasCircles.clear();

                //Draw new circles
                for (Iterator<LatLng> i = mCamerasLocations.iterator(); i.hasNext();) {
                    LatLng cameraLocation = i.next();
                    //MarkerOptions cameraMarkerOptions = new MarkerOptions();
                    //cameraMarkerOptions.position(cameraLocation);
                    //mGoogleMap.addMarker(cameraMarkerOptions);
                    Circle circle = mGoogleMap.addCircle(new CircleOptions()
                            .center(cameraLocation)
                            .radius(CAMERA_RADIUS)
                            .strokeColor(Color.argb(100,0,0,255))
                            .strokeWidth(2)
                            .fillColor(Color.argb(50,255,0,0)));

                    mCamerasCircles.add(circle);

                    double distance = getDistance(cameraLocation.latitude, cameraLocation.longitude, latLng.latitude, latLng.longitude);
                    if ( distance <= mDistance || mDistance == 0.0d)
                    {
                        mDistance = distance;
                    }
                }

                //move map camera
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                mGoogleMap.animateCamera(cameraUpdate);

                //display info about closets camera
                if (mDistance != 0.0d)
                {
                    String camResponse = "Closest camera is in " + String.valueOf((int)mDistance) + " m" ;
                    mCamInfo.setText(camResponse);
                }

                //display current user's coordinates
                String locResponse = "Your coordinates: " + String.valueOf(latLng.latitude) + " , " + String.valueOf(latLng.longitude);
                mLocInfo.setText(locResponse);

                //makes alert in case user is in camera zone
                if (mDistance <= CAMERA_RADIUS && mDistance != 0.0d)
                {
                    makeAlert();
                }
            }
        }
    };

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("The app only will work if you allow to locate yourself")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }



    public void getCameras(String lat, String lon) {

        String url ="https://koordinates.com/services/query/v1/vector.json?key=961b003dda884e1f834ed4dd98eb7ffd" +
                "&layer="+ MAP_LAYER +
                "&x=" + lon +
                "&y=" + lat +
                "&max_results=" + String.valueOf(CAMERA_QUANTITY) +
                "&radius="+ String.valueOf(SEARCH_RADIUS) +
                "&geometry=true&with_field_names=true";
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {


                        try {


                            JSONObject vectorQuery = response.getJSONObject("vectorQuery");
                            JSONObject layers = vectorQuery.getJSONObject("layers");
                            JSONObject layer = layers.getJSONObject(MAP_LAYER);
                            JSONArray data = layer.getJSONArray("features");

                            //Clear previous data
                            mCamerasLocations.clear();

                            for(int index = 0; index < data.length(); index++)
                            {
                                JSONObject camera = data.getJSONObject(index);

                                JSONObject geometry = camera.getJSONObject("geometry");
                                JSONArray coordinates = geometry.getJSONArray("coordinates");
                                LatLng latLng = new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));

                                mCamerasLocations.add(latLng);

                            }

                            System.err.println(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error", error.getMessage());

                    }
                });
        queue.add(jsObjRequest);
    }


    //Shows notification and play sound for user
    public void makeAlert() {

        //Notification sound for the alert
        try {
            Uri notify = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notify);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mAlert = Toast.makeText(getApplicationContext(),
                "There is a camera somewhere nearby, you better to slow down!", Toast.LENGTH_LONG);
        // Set the toast and duration
        int toastDurationInMilliSeconds = 20000;

        // Set the countdown to display the toast
        CountDownTimer toastCountDown;
        toastCountDown = new CountDownTimer(toastDurationInMilliSeconds, 1000 /*Tick duration*/) {
            public void onTick(long millisUntilFinished) {
                mAlert.show();
            }
            public void onFinish() {
                mAlert.cancel();
            }
        };

        // Show the toast and starts the countdown
        mAlert.show();
        toastCountDown.start();
    }

    //translate gps coordinates to meters and get distance between 2 points
    private double getDistance(double lat_a, double lng_a, double lat_b, double lng_b) {

        double pk = (180/3.14169); //Degrees to rads

        double a1 = lat_a / pk;
        double a2 = lng_a / pk;
        double b1 = lat_b / pk;
        double b2 = lng_b / pk;

        double t1 = Math.cos(a1)*Math.cos(a2)*Math.cos(b1)*Math.cos(b2);
        double t2 = Math.cos(a1)*Math.sin(a2)*Math.cos(b1)*Math.sin(b2);
        double t3 = Math.sin(a1)*Math.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);

        return EARTH_RADIUS*tt;
    }
}