package com.example.juanf.userslocationrealtime;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.support.annotation.*;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.*;

import org.json.JSONObject;
import org.json.JSONArray;

import java.text.MessageFormat;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    public static final int REQ_PERMISSION = 0;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private Location location;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 5000, FASTES_INTERVAL = 5000;

    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private static final int ALL_PERMISSIONS_RESULT = 1011;

    private final HashMap<String,Marker> markers = new HashMap<>();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
            super.onStart();
            if (googleApiClient != null){
                googleApiClient.connect();
            }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!checkPlayServices()){
            Toast.makeText(MapsActivity.this, "You need to install Google Play Services to use the app",
                    Toast.LENGTH_LONG).show();
        }
    }


    private boolean checkPlayServices(){
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST);
            }else{
                finish();
            }
            return false;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SocketManager.Disconnect();
    }

    private ArrayList<String> permissionsToRequest(ArrayList<String> permissionsToRequest){
        ArrayList<String> result = new ArrayList<>();

        for (String perm: permissionsToRequest){
            result.add(perm);
        }
        return result;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        if(!checkPermission()){
            requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                    ALL_PERMISSIONS_RESULT);
        }else{
            mMap.setMyLocationEnabled(true);
            googleApiClient.connect();
        }


        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        SocketManager.RegisterDeleteCurrentLocation(new Callback() {
            @Override
            public void onSuccess(Object object) {
                try{
                    final String id = (String)object;
                    if(markers.containsKey(id)){
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                markers.get(id).remove();
                                markers.remove(id);
                            }
                        });
                    }
                }catch(Exception e){
                    Log.e("UsersLocationRealTime",e.getMessage());
                }
            }

            @Override
            public void onError(String err) {

            }
        });

        SocketManager.RegisterUpdateCurrentLocation(new Callback() {
            @Override
            public void onSuccess(Object object) {
                try{
                    JSONObject currentLocation = (JSONObject)object;
                    final String email = currentLocation.getString("email");
                    final double latitude = currentLocation.getDouble("latitude");
                    final double longitude = currentLocation.getDouble("longitude");
                    final String id = currentLocation.getString("_id");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(markers.containsKey(id)){
                                LatLng latLng = new LatLng(latitude, longitude);
                                markers.get(id).setPosition(latLng);
                            }else{
                                LatLng latLng = new LatLng(latitude, longitude);
                                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(email));
                                if( marker != null){
                                    markers.put(id, marker);
                                }
                            }
                        }
                    });
                }catch(Exception e){
                    Log.e("UsersLocationRealTime",e.getMessage());
                }
            }

            @Override
            public void onError(String err) {

            }
        });

        SocketManager.GetAllCurrentLocations(new Callback() {
            @Override
            public void onSuccess(Object object) {
                JSONArray currentLocations = (JSONArray)object;
                for(int i=0; i < currentLocations.length(); i++){
                    try{
                        JSONObject currentLocation = (JSONObject) currentLocations.getJSONObject(i);
                        final String email = currentLocation.getString("email");
                        final double latitude = currentLocation.getDouble("latitude");
                        final double longitude = currentLocation.getDouble("longitude");
                        final String id = currentLocation.getString("_id");
                        if(!markers.containsKey(id)){
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    LatLng latLng = new LatLng(latitude, longitude);
                                    Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(email));
                                    if( marker != null){
                                        markers.put(id, marker);
                                    }
                                }
                            });
                        }
                    }catch(Exception e){
                        Log.e("UsersLocationRealTime",e.getMessage());
                    }
                }
            }

            @Override
            public void onError(String err) {

            }
        });
    }


    private boolean hasPermission(String permission){
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d("UsersLocationRealTime", "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED);
    }


    // Asks for permission
    /*private void askPermission() {
        Log.d("UsersLocationRealTime", "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_PERMISSION
        );
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("UsersLocationRealTime", "onRequestPermissionsResult()");
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    if (checkPermission())
                        mMap.setMyLocationEnabled(true);

                } else {
                    // Permission denied

                }
            }
            break;

            case ALL_PERMISSIONS_RESULT:
                for(String perm: permissionsToRequest){
                    if(!hasPermission(perm)){
                        permissionsRejected.add(perm);
                    }

                    if(permissionsRejected.size() > 0){
                        if(shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MapsActivity.this)
                                    .setMessage("These permissions are mandatory to get your location. You need to allow them.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]),
                                                    ALL_PERMISSIONS_RESULT);
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();
                        }
                    }
                    else{
                        if(googleApiClient != null){
                            googleApiClient.connect();
                        }
                    }
                    break;
                }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(!checkPermission())
            return;

        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null){
            Toast.makeText(MapsActivity.this, MessageFormat.format(
                    "Location: Lat:{0}  Long:{1}",location.getLatitude(), location.getLongitude()),
                    Toast.LENGTH_LONG).show();

            if(mMap != null){
                LatLng currentLatLng = new LatLng(location.getLatitude(),
                        location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
            }
        }

        startLocationUpdates();
    }

    private void startLocationUpdates(){
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);

        if(!checkPermission())
            Toast.makeText(MapsActivity.this, "You need to enable permissions to display location",
                    Toast.LENGTH_LONG).show();

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            Log.d("UsersLocationRealTime", MessageFormat.format(
                    "Location: Lat:{0}  Long:{1}",location.getLatitude(), location.getLongitude()));
            SocketManager.UpdateLocation(location.getLatitude(), location.getLongitude());
        }
    }
}
