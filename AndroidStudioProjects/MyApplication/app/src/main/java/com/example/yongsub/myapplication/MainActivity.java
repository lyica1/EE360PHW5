package com.example.yongsub.myapplication;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import java.text.DateFormat;
import java.util.ArrayList;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.Response;
import com.google.android.gms.location.LocationServices;


public class MainActivity extends AppCompatActivity implements LocationListener{
    private static final long DISTANCE = 10;
    private static final long TIME = 1000 * 60 * 1;
    TextView Humidity, WindSpeed, Temperature, Precipitation, average, hour, high, low, convert;
    Button button;
    LocationManager locationManager;
    Location loc;
    boolean isGPS = false;
    boolean isNetwork = false;
    boolean canGetLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Temperature = (TextView) findViewById(R.id.Temperature);
        Humidity = (TextView) findViewById(R.id.Humidityresult);
        WindSpeed = (TextView) findViewById(R.id.WindSpeedresult);
        Precipitation = (TextView) findViewById(R.id.Precipitationresult);
        average = (TextView) findViewById(R.id.average);
        hour = (TextView) findViewById(R.id.hourtemperature);
        high = (TextView) findViewById(R.id.high);
        low = (TextView) findViewById(R.id.low);
        button = (Button) findViewById(R.id.button);
        convert = (EditText) findViewById(R.id.convert);
        locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
        isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!checkPermission(MainActivity.this)) {
            requestPermission(MainActivity.this,1);
        }
        getLocation();
        String url = "https://api.darksky.net/forecast/d1f231c034c0f2bb4a95c4bf057b224e/" + loc.getLatitude() + "," + loc.getLongitude();
        new Parsing().execute(url);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String url = "https://api.darksky.net/forecast/d1f231c034c0f2bb4a95c4bf057b224e/" + loc.getLatitude() + "," + loc.getLongitude() + ",";
                String temp = convert.getText().toString();
                url += temp;
                new TimeMachine().execute(url);
            }
        });
    }
    public void requestPermission(Activity activity, final int code){
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(activity,"We need GPS Permission",Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},code);
        }
    }
    public boolean checkPermission(Activity activity){
        int result = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        loc = location;

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {
        getLocation();
    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void getLocation() {
        try {
            if (isGPS) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        TIME,
                        DISTANCE, this);
                if (locationManager != null) {
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            } else if (isNetwork) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        TIME,
                        DISTANCE, this);
                if (locationManager != null) {
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        getLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    public void getAverage(String data){
        int sum = 0;
        try {
            JSONObject object = new JSONObject(data);
            JSONObject hourly = object.getJSONObject("hourly");
            JSONArray forty = hourly.getJSONArray("data");
            for(int i = 0; i < 48; i++){
                JSONObject temp = forty.getJSONObject(i);
                double temdata1 = temp.getDouble("temperature");
                int temdata = (int) temdata1;
                sum += temdata;
            }
            sum/=48;
            average.setText(Integer.toString(sum) + "°F");
        }catch(JSONException e){
            Log.d("JSON", "Invalid");
        }

    }
    public void getSeven(String data){
        String first, one, two, three, four, five, six, seven = "";
        String finalstring = "";
        try{
            JSONObject object = new JSONObject(data);
            JSONObject daily = object.getJSONObject("daily");
            JSONArray seven1 = daily.getJSONArray("data");
            for(int i = 0; i < 7; i++){
                JSONObject temp = seven1.getJSONObject(i);
                double tem1 = temp.getDouble("temperatureHigh");
                int tem = (int) tem1;
                first = Integer.toString(tem);
                if(i == 0){
                    one = " " + first + "°F";
                    finalstring += one + "        ";
                }else if(i == 1){
                    two = first + "°F";
                    finalstring += two + "         ";
                }else if(i == 2){
                    three = first + "°F";
                    finalstring += three + "         ";
                }else if(i == 3){
                    four = first + "°F";
                    finalstring += four + "        ";
                }else if(i == 4){
                    five = first + "°F";
                    finalstring += five + "        ";
                }else if(i == 5){
                    six = first + "°F";
                    finalstring += six + "        ";
                }else if(i == 6){
                    seven = first + "°F";
                    finalstring += seven;
                }
            }
            high.setText(finalstring);
            finalstring = "";
            for(int i = 0; i < 7; i++){
                JSONObject temp = seven1.getJSONObject(i);
                double tem1 = temp.getDouble("temperatureLow");
                int tem = (int) tem1;
                first = Integer.toString(tem);
                if(i == 0){
                    one = " " + first + "°F";
                    finalstring += one + "        ";
                }else if(i == 1){
                    two = first + "°F";
                    finalstring += two + "         ";
                }else if(i == 2){
                    three = first + "°F";
                    finalstring += three + "         ";
                }else if(i == 3){
                    four = first + "°F";
                    finalstring += four + "        ";
                }else if(i == 4){
                    five = first + "°F";
                    finalstring += five + "        ";
                }else if(i == 5){
                    six = first + "°F";
                    finalstring += six + "        ";
                }else if(i == 6){
                    seven = first + "°F";
                    finalstring += seven;
                }
                low.setText(finalstring);
            }
        }catch(JSONException e){
            Log.d("Temp", "Error");
        }
    }
    public void ButtonClick(String data){
        try{
            JSONObject object = new JSONObject(data);
            JSONObject currently = object.getJSONObject("currently");
            double temp1 = currently.getDouble("temperature");
            int temp = (int) temp1;
            String result = Integer.toString(temp);
            convert.setText(result + "°F");
        }catch (JSONException e){
            Log.d("Convert", "Error");
        }
    }
    public void display(String data) {
        String one, two, three, four, five, finalstring = "";
        String result = "";
        String hresult = "";
        String wresult = "";
        String presult = "";
        String first = "";
        try {
            JSONObject object = new JSONObject(data);
            JSONObject currently = object.getJSONObject("currently");
            JSONObject data1 = object.getJSONObject("hourly");
            JSONArray hourly = data1.getJSONArray("data");
            double temp1 = currently.getDouble("temperature");
            int temperature = (int) temp1;
            double humidity = currently.getDouble("humidity");
            double wind = currently.getDouble("windSpeed");
            double pre = currently.getDouble("precipIntensity");
            pre = Math.round(pre*100);
            pre/=100;
            for(int i = 0; i < 5; i++){
                JSONObject temp = hourly.getJSONObject(i);
                double tem1 = temp.getDouble("temperature");
                int tem = (int) tem1;
                first = Integer.toString(tem);
                if(i == 0){
                    one = " " + first + "°F";
                    finalstring += one + "               ";
                }else if(i == 1){
                    two = first + "°F";
                    finalstring += two + "                 ";
                }else if(i == 2){
                    three = first + "°F";
                    finalstring += three + "                ";
                }else if(i == 3){
                    four = first + "°F";
                    finalstring += four + "                ";
                }else if(i == 4){
                    five = first + "°F";
                    finalstring += five;
                }
            }
            getSeven(data);
            hour.setText(finalstring);
            result = Integer.toString(temperature);
            hresult = Double.toString(humidity);
            wresult = Double.toString(wind);
            presult = Double.toString(pre);
            result += "°F";
            Temperature.setText(result);
            Humidity.setText(hresult);
            WindSpeed.setText(wresult);
            Precipitation.setText(presult);
            getAverage(data);
        } catch (JSONException e) {
            Log.d("JSON", "Invalid JSON Data");
        }
    }

    public class Parsing extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... urls) {
            String returnstring = "";
            String line;
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                BufferedReader input = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while ((line = input.readLine()) != null) {
                    returnstring += line + "\n";
                }

            } catch (MalformedURLException e) {
                Log.d("URL", "Invalid URL");
            } catch (IOException a) {
                Log.d("IOException", "Error");
            }
            return returnstring;
        }

        protected void onProgressUpdate() {

        }

        protected void onPostExecute(String data) {
            display(data);
        }
    }
    public class TimeMachine extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... urls) {
            String returnstring = "";
            String line;
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                BufferedReader input = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while ((line = input.readLine()) != null) {
                    returnstring += line + "\n";
                }

            } catch (MalformedURLException e) {
                Log.d("URL", "Invalid URL");
            } catch (IOException a) {
                Log.d("IOException", "Error");
            }
            return returnstring;
        }

        protected void onProgressUpdate() {

        }

        protected void onPostExecute(String data) {
            ButtonClick(data);
        }
    }
}
