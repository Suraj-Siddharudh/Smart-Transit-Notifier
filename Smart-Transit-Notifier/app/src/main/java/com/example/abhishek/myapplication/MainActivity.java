package com.example.abhishek.myapplication;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity  {
    public ArrayList<ArrayList<String>> eventsList = new ArrayList<>();
    Map<String,List<String>> map = new HashMap<>();


    Location location_net,location_gps,location;
    String latfield,longfield;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = this;
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://com.android.calendar/events"),
                new String[]{"calendar_id", "title", "description", "dtstart", "dtend", "eventLocation"}, null, null, "dtstart ASC");
        cursor.moveToFirst();
        // fetching calendars name
        Date currTime = Calendar.getInstance().getTime();
        Log.d("Today date", String.valueOf(currTime));
        //Long currEpoch = System.currentTimeMillis();
        Long currEpoch = System.currentTimeMillis()/1000;
        Long delta = Long.valueOf(86400);
        Long min = currEpoch + 2 * delta;
        Long max = currEpoch + 6 * delta;
        Log.d("Today date currtime", String.valueOf(currEpoch));
        String CNames[] = new String[cursor.getCount()];
        for (int i = 0; i < CNames.length; i++) {
            ArrayList<String> currEvent = new ArrayList<>();
            String eventTitle = cursor.getString(1);
            String eventStartDate = String.valueOf(Long.parseLong(cursor.getString(3))/1000);

            String eventLocation = cursor.getString(5);

            if (Long.parseLong(eventStartDate) < max && Long.parseLong(eventStartDate) > min && eventLocation.length() != 0) {
                String loc = eventLocation.replace(' ', '+');
                currEvent.add(eventTitle);
                currEvent.add(eventStartDate);
                currEvent.add(loc);
                eventsList.add(currEvent);
            }
            cursor.moveToNext();
        }
        cursor.close();

        /*********************************************/

        HttpURLConnection urlConnection = null;

        if(eventsList.size() == 0){
         Toast.makeText(this, "No events found to schedule the travel.",
                 Toast.LENGTH_LONG).show();
//            Log.d("ANALWAY","123");
         }
         else if(eventsList.get(0).get(2).length() == 0){
             Toast.makeText(this, "No location saved for this event.",
                     Toast.LENGTH_LONG).show();
         }
         else{
            Log.d("here","here");
            String curloc=null;
            try {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                String Url = "https://maps.googleapis.com/maps/api/directions/json?origin=Kings+Court+Raleigh&destination=" + eventsList.get(0).get(2) + "&key=AIzaSyBkLli9Te539Uob6HodeOgR-bD83JCrRcg&mode=transit&arrival_time=" + eventsList.get(0).get(1);


                URL url = new URL(Url);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bR = new BufferedReader(new InputStreamReader(in));
                String line = "";

                StringBuilder responseStrBuilder = new StringBuilder();
                while ((line = bR.readLine()) != null) {

                    responseStrBuilder.append(line);
                }
                in.close();

                JSONObject result = new JSONObject(responseStrBuilder.toString());

                List<JSONObject> lst = getTravelDetails(result);
                map = processingJSON(lst);

                Log.d("ASRI", String.valueOf(lst));
                TextView showTitle = findViewById(R.id.showTitle);
                String temp1 = eventsList.get(0).get(2);
                temp1 = temp1.replace("+"," ");
                showTitle.setText(eventsList.get(0).get(0));
                TextView showTime = findViewById(R.id.showTime);
                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                cal.setTimeInMillis(Long.parseLong(eventsList.get(0).get(1)) * 1000L);
                android.text.format.DateFormat df = new android.text.format.DateFormat();
                showTime.setText(df.format("dd/MM/yyyy hh:mm:ss", cal).toString());
                TextView showLoc = findViewById(R.id.showLoc);
                showLoc.setText(temp1);


                TextView showHops = findViewById(R.id.showHops);
                showHops.setText("# Hops: "+map.get("Arrival Stop").size());

                TextView startTime = findViewById(R.id.startTime);
                startTime.setText(map.get("Arrival Time").get(0));


            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }
        }


    }

    public void showDetails(View view) {
        TextView tw = findViewById(R.id.textView);
        String text = "";
        int count = map.get("Arrival Stop").size();
        int i=0;
        StringBuilder sb = new StringBuilder();
        while(i<count){
            for(String key : map.keySet()){
                sb.append(key + " : " +map.get(key).get(i) +"\n");
            }
            sb.append("\n");
            i++;
        }
        System.out.println(sb);
        tw.setText(sb);
    }

    public static Map<String,List<String>> processingJSON(List<JSONObject> jsonObjectList){
        Map<String,List<String>> map = new HashMap<>();
        String destinationName,destinationArrivalTime,sourceName,sourceDepartureTime,totalTime,headSign;
        String arrivalStop,arrivalTime, busName, busNumber, busDetails;
        for(int i=0;i<jsonObjectList.size();i++){
            JSONObject object = jsonObjectList.get(i);
            try {
                destinationName = object.getJSONObject("transit_details").getJSONObject("arrival_stop").getString("name");
                destinationArrivalTime = object.getJSONObject("transit_details").getJSONObject("arrival_time").getString("text");
                totalTime = object.getJSONObject("duration").getString("text");
                arrivalStop = object.getJSONObject("transit_details").getJSONObject("departure_stop").getString("name");
                arrivalTime = object.getJSONObject("transit_details").getJSONObject("departure_time").getString("text");
                headSign = object.getJSONObject("transit_details").getString("headsign");
                busName = object.getJSONObject("transit_details").getJSONObject("line").getJSONObject("vehicle").getString("name");
                busNumber = object.getJSONObject("transit_details").getJSONObject("line").getString("short_name");
                busDetails = busName+busNumber;

                if(!map.containsKey("Arrival Stop")){
                    map.put("Arrival Stop",new ArrayList<String>());
                }
                map.get("Arrival Stop").add(arrivalStop);

                if(!map.containsKey("Arrival Time")){
                    map.put("Arrival Time",new ArrayList<String>());
                }
                map.get("Arrival Time").add(arrivalTime);

                if(!map.containsKey("Total Time")){
                    map.put("Total Time",new ArrayList<String>());
                }
                map.get("Total Time").add(totalTime);

                if(!map.containsKey("Destination Name")){
                    map.put("Destination Name",new ArrayList<String>());
                }
                map.get("Destination Name").add(destinationName);

                if(!map.containsKey("Destination ArrivalTime")){
                    map.put("Destination ArrivalTime",new ArrayList<String>());
                }
                map.get("Destination ArrivalTime").add(destinationArrivalTime);

                if(!map.containsKey("Head Sign")){
                    map.put("Head Sign",new ArrayList<String>());
                }
                map.get("Head Sign").add(headSign);

                if(!map.containsKey("Bus Details")){
                    map.put("Bus Details",new ArrayList<String>());
                }
                map.get("Bus Details").add(busDetails);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return map;
    }
    public static List<JSONObject> getTravelDetails(JSONObject jsonObject){
        List<JSONObject> result = new ArrayList<>();
        try{
            Iterator<String> keys = (Iterator<String>) jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if(key.equals("routes")){
                    JSONArray levelOne = (JSONArray)jsonObject.get(key);
                    JSONObject jsonObject1 = levelOne.getJSONObject(0);
                    JSONArray legsArray = (JSONArray) jsonObject1.get("legs");
                    JSONObject legsObject = legsArray.getJSONObject(0);
                    JSONArray stepsArray = (JSONArray) legsObject.get("steps");
                    for(int j=0;j<stepsArray.length();j++){
                        JSONObject stepsObject2 = stepsArray.getJSONObject(j);
                        if(stepsObject2.get("travel_mode").equals("TRANSIT")){
                            result.add(stepsObject2);
                        }
                    }
                }

            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
