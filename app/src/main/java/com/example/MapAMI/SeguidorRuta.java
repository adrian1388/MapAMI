package com.example.MapAMI;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;


public class SeguidorRuta extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private UiSettings mUiSettings;
    private CheckBox mMyLocationButtonCheckbox;
    private CheckBox mMyLocationLayerCheckbox;
    private static final int MY_LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_LAYER_PERMISSION_REQUEST_CODE = 2;
    private static final LatLng ENTRADA_GARITA = new LatLng(-2.152126, -79.952853);
    private static final LatLng MARITIMA = new LatLng(-2.146461, -79.962302);
    private static final LatLng FIEC_NUEVA = new LatLng(-2.144895, -79.967477);
    private LatLng INICIO = new LatLng(0, 0);
    private LatLng PUNTO_INTERMEDIO = new LatLng(0, 0);
    private LatLng FIN = new LatLng(0, 0);
    private boolean mLocationPermissionDenied = false;

    private boolean grabando = false;
    private Button GrabarRuta;
    private GPSTracker gps;
    private Timer t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguidor_ruta);

        mMyLocationButtonCheckbox = (CheckBox) findViewById(R.id.item_ubicacion);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.cont_mapa_seg_ruta);
        mapFragment.getMapAsync(this);
        GrabarRuta = (Button) findViewById(R.id.grabar_ruta);
        gps = new GPSTracker(this);


    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mUiSettings = mMap.getUiSettings();

        // Keep the UI Settings state in sync with the checkboxes.
        mUiSettings.setZoomControlsEnabled(false);
        mUiSettings.setCompassEnabled(false);
        mUiSettings.setMyLocationButtonEnabled(false);
        mMap.setMyLocationEnabled(true);
        mUiSettings.setScrollGesturesEnabled(true);
        mUiSettings.setZoomGesturesEnabled(true);
        mUiSettings.setTiltGesturesEnabled(true);
        mUiSettings.setRotateGesturesEnabled(true);

        MarkerOptions options = new MarkerOptions();
        options.position(ENTRADA_GARITA);
        options.position(MARITIMA);
        options.position(FIEC_NUEVA);
        mMap.addMarker(options);
        String url = getMapsApiDirectionsUrl(ENTRADA_GARITA.latitude, ENTRADA_GARITA.longitude, FIEC_NUEVA.latitude,FIEC_NUEVA.longitude,MARITIMA.latitude,MARITIMA.longitude);
        ReadTask downloadTask = new ReadTask();
        downloadTask.execute(url);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MARITIMA, 13));
        addMarkers(ENTRADA_GARITA,FIEC_NUEVA,MARITIMA);
    }


    public void onClickGrabarButton(View v) {

        final Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(500);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        if (v == GrabarRuta && grabando == false) {
            v.setBackgroundResource(R.drawable.stop);
            INICIO = new LatLng(gps.getLatitude(), gps.getLongitude());
            grabando = true;
            v.startAnimation(animation);
            t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {
                  @Override
                  public void run() {
                      Log.d("PRUEBA","asdasdasdasdasd");
                      PUNTO_INTERMEDIO = new LatLng(gps.getLatitude(), gps.getLongitude());
                  }
              },0,15000);
        } else if (v == GrabarRuta && grabando == true) {
            v.setBackgroundResource(R.drawable.rec);
            grabando = false;
            FIN = new LatLng(gps.getLatitude(), gps.getLongitude());
            v.clearAnimation();
            t.cancel();

            MarkerOptions options = new MarkerOptions();
            options.position(INICIO);
            options.position(PUNTO_INTERMEDIO);
            options.position(FIN);
            mMap.addMarker(options);
            String url = getMapsApiDirectionsUrl(INICIO.latitude, INICIO.longitude, FIN.latitude,FIN.longitude,PUNTO_INTERMEDIO.latitude,PUNTO_INTERMEDIO.longitude);
            ReadTask downloadTask = new ReadTask();
            downloadTask.execute(url);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PUNTO_INTERMEDIO, 13));
            addMarkers(INICIO,FIN,PUNTO_INTERMEDIO);

        } else {
            v.setBackgroundResource(R.drawable.rec);
            grabando = false;
            v.clearAnimation();
            t.cancel();
        }
    }

    private boolean checkReady() {
        if (mMap == null) {
            Toast.makeText(this, R.string.mapa_no_listo, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void requestLocationPermission(int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Display a dialog with rationale.
            PermisosUbicacion.RationaleDialog
                    .newInstance(requestCode, false).show(
                    getSupportFragmentManager(), "dialog");
        } else {
            // Location permission has not been granted yet, request it.
            PermisosUbicacion.requestPermission(this, requestCode,
                    Manifest.permission.ACCESS_FINE_LOCATION, false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == MY_LOCATION_PERMISSION_REQUEST_CODE) {
            // Enable the My Location button if the permission has been granted.
            if (PermisosUbicacion.isPermissionGranted(permissions, grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                mUiSettings.setMyLocationButtonEnabled(true);
                mMyLocationButtonCheckbox.setChecked(true);
            } else {
                mLocationPermissionDenied = true;
            }

        } else if (requestCode == LOCATION_LAYER_PERMISSION_REQUEST_CODE) {
            // Enable the My Location layer if the permission has been granted.
            if (PermisosUbicacion.isPermissionGranted(permissions, grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                mMap.setMyLocationEnabled(true);
                mMyLocationLayerCheckbox.setChecked(true);
            } else {
                mLocationPermissionDenied = true;
            }
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mLocationPermissionDenied) {
            PermisosUbicacion.PermissionDeniedDialog
                    .newInstance(false).show(getSupportFragmentManager(), "dialog");
            mLocationPermissionDenied = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ui, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_compas:
                if (!checkReady()) {
                    return false;
                }
                if (item.isChecked()) {
                    item.setChecked(false);
                }
                else {
                    item.setChecked(true);
                }
                mUiSettings.setCompassEnabled(item.isChecked());
                return true;

            case R.id.item_ubicacion:
                if (!checkReady()) {
                    return false;
                }
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mUiSettings.setMyLocationButtonEnabled(item.isChecked());
                } else {
                    item.setChecked(false);
                    requestLocationPermission(MY_LOCATION_PERMISSION_REQUEST_CODE);
                }
                return true;

            case R.id.item_zoom:
                if (!checkReady()) {
                    return false;
                }
                if (item.isChecked()){
                    item.setChecked(false);
                }
                else{
                    item.setChecked(true);
                }
                mUiSettings.setZoomControlsEnabled(item.isChecked());
                return true;
            //map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            //map.invalidate();
            case R.id.item_mapa:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            case R.id.item_tierra:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    private String getMapsApiDirectionsUrl(double origin_latitude,double origin_longitude,double destination_latitude,double destination_longitude, double middle_latitude,double middle_longitude) {

        String origin = "origin=" + origin_latitude + "," + origin_longitude ;
        String destination = "destination=" + destination_latitude + "," + destination_longitude;
        String waypoints = "waypoints=optimize:true|"
                + origin_latitude + "," + origin_longitude
                + "|" + middle_latitude + "," + middle_longitude
                + "|" + destination_latitude + "," + destination_longitude;

        String sensor = "sensor=false";
        String params =  origin + "&" + destination + "&" + waypoints + "&" + sensor;

        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params;
        System.out.print(url);
        return url;
    }

    private void addMarkers(LatLng origin,LatLng destination, LatLng middle) {
        if (mMap != null) {
            mMap.addMarker(new MarkerOptions().position(origin)
                    .title("First Point"));
            mMap.addMarker(new MarkerOptions().position(middle)
                    .title("Second Point"));
            mMap.addMarker(new MarkerOptions().position(destination)
                    .title("Third Point"));
        }
    }

    private class ReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                HttpConnection http = new HttpConnection();
                data = http.readUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points = null;
            PolylineOptions polyLineOptions = null;

            // traversing through routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(4);
                polyLineOptions.color(Color.BLUE);
            }

            mMap.addPolyline(polyLineOptions);
        }
    }

}
