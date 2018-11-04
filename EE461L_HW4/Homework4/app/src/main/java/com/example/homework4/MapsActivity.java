package com.example.homework4;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Random;
import java.util.Stack;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String apikey = "AIzaSyChn-RpJN3LRsidRA-KeIipfW8hrUgw1Qk";
    private double lat;
    private double lng;
    private Button lucky;
    private Button back;
    private Date date = new Date();
    private Random ran = new Random(date.getTime());
    private Stack<LatLng> history = new Stack<LatLng>();

    private Marker current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        lucky = (Button) findViewById(R.id.button);
        lucky.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    goRandom(mMap);
                }
                return false;
            }
        });
        back = findViewById(R.id.button2);
        back.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    back(mMap);
                }
                return false;
            }
        });
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

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        current = mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void goRandom(GoogleMap googleMap){
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng random = new LatLng(-90 + ran.nextInt(179), -180 + ran.nextInt(360));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(random, 4));
        history.push(random);
        current.remove();
        current = mMap.addMarker(new MarkerOptions().position(random).title("Marker in Sydney"));
    }

    public void back(GoogleMap googleMap){
        mMap = googleMap;
        if(history.size() < 2){
            return;
        }
        history.pop();
        LatLng past = history.peek();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(past, 4));
        current.remove();
        current = mMap.addMarker(new MarkerOptions().position(past).title("hello"));
    }

    class BackGround extends AsyncTask<String, Void, String>{
        @Override
        protected void onPreExecute(){

        }
        @Override
        protected String doInBackground(String... url1){
            String url = url1[0].replaceAll(" ", "+");
            String fulladdress = "";
            try{
                URL obj = new URL(url);
                HttpURLConnection connec = (HttpURLConnection) obj.openConnection();
                connec.setRequestMethod("GET");
                InputStreamReader isr = new InputStreamReader(connec.getInputStream());
                BufferedReader in = new BufferedReader(isr);
                StringBuffer sb = new StringBuffer();
                String input;
                while((input = in.readLine()) != null){
                    sb.append(input);
                }
                JSONObject jsonob = new JSONObject(sb.toString());
                JSONArray array = jsonob.getJSONArray("results");
                fulladdress = array.getJSONObject(0).getString("formatted_address");

                lat = Double.parseDouble(array.getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lat"));
                lng = Double.parseDouble(array.getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lng"));
            }catch (MalformedURLException e){
                e.getStackTrace();
            }catch (IOException c) {
                c.getStackTrace();
            }catch(JSONException b){
                b.getStackTrace();
            }
            return fulladdress;
        }
        @Override
        protected void onPostExecute(final String address){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));
            current.remove();
            current = mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)));
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    Toast.makeText(getApplication().getBaseContext(), address, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        SearchView view = (SearchView) menu.findItem(R.id.action_search).getActionView();
        view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query){
                String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + query + "&key=" + apikey;
                new BackGround().execute(url);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newtext){

                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }
}
