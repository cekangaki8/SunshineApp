package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Candy on 9/6/2015.
 */
    /**
     * A placeholder fragment containing a simple view.
     */
    public class ForecastFragment extends Fragment {

        private ArrayAdapter<String> forecastArrayAdapter;

        public ForecastFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.forecastfragment, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_refresh:
                    updateWeatherData();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            updateWeatherData();
        }

        private void updateWeatherData() {
            FetchWeatherTask fetchWeatherTask = new FetchWeatherTask(){
                @Override
                protected void onPostExecute(String[] strings) {
                    forecastArrayAdapter.clear();
                    for (String dayData : strings) {
                        forecastArrayAdapter.add(dayData);
                    }
                    forecastArrayAdapter.notifyDataSetChanged();
                }
            };
            //Mountain zip code 94043
            String zipCode = PreferenceManager.getDefaultSharedPreferences(getActivity()).
                    getString(getString(R.string.pref_location), getString(R.string.pref_default_location));
            String unit = PreferenceManager.getDefaultSharedPreferences(getActivity()).
                    getString(getString(R.string.pref_temperature_unit_key), getString(R.string.pref_temperature_unit_default));
            fetchWeatherTask.execute(zipCode, unit);
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

           /* List<String> forecast = new ArrayList<>();
            forecast.add("Today - Sunny - 88/63");
            forecast.add("Monday - Sunny - 88/63");
            forecast.add("Tuesday - Sunny - 88/63");
            forecast.add("Wednesday - Sunny - 88/63");
            forecast.add("Thursday - Sunny - 88/63");
            forecast.add("Friday - Sunny - 88/63");
            forecast.add("Saturday - Sunny - 88/63");
*/
            forecastArrayAdapter = new ArrayAdapter<String>(getActivity(),R.layout.list_item_forecast,R.id.list_item_forecast_textview,new ArrayList<String>());

            ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
            listView.setAdapter(forecastArrayAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String itemClicked = forecastArrayAdapter.getItem(i);
                    //Toast.makeText(getActivity(), itemClicked, Toast.LENGTH_SHORT).show();
                    Intent startDetailActivityIntent = new Intent(getActivity(), DetailActivity.class);
                    startDetailActivityIntent.putExtra(DetailActivity.WEATHERDATA, itemClicked);
                    startActivity(startDetailActivityIntent);

                }
            });


            return rootView;
        }

        private class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

            private final String LOG_TAG = FetchWeatherTask.class.getName();

            /* The date/time conversion code is going to be moved outside the asynctask later,
        * so for convenience we're breaking it out into its own method now.
        */
            private String getReadableDateString(long time){
                // Because the API returns a unix timestamp (measured in seconds),
                // it must be converted to milliseconds in order to be converted to valid date.
                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
                return shortenedDateFormat.format(time);
            }

            /**
             * Prepare the weather high/lows for presentation.
             */
            private String formatHighLows(double high, double low) {
                // For presentation, assume the user doesn't care about tenths of a degree.
                long roundedHigh = Math.round(high);
                long roundedLow = Math.round(low);

                String highLowStr = roundedHigh + "/" + roundedLow;
                return highLowStr;
            }

            /**
             * Take the String representing the complete forecast in JSON Format and
             * pull out the data we need to construct the Strings needed for the wireframes.
             *
             * Fortunately parsing is easy:  constructor takes the JSON string and converts it
             * into an Object hierarchy for us.
             */
            private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                    throws JSONException {

                // These are the names of the JSON objects that need to be extracted.
                final String OWM_LIST = "list";
                final String OWM_WEATHER = "weather";
                final String OWM_TEMPERATURE = "temp";
                final String OWM_MAX = "max";
                final String OWM_MIN = "min";
                final String OWM_DESCRIPTION = "main";

                JSONObject forecastJson = new JSONObject(forecastJsonStr);
                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

                // OWM returns daily forecasts based upon the local time of the city that is being
                // asked for, which means that we need to know the GMT offset to translate this data
                // properly.

                // Since this data is also sent in-order and the first day is always the
                // current day, we're going to take advantage of that to get a nice
                // normalized UTC date for all of our weather.

                Time dayTime = new Time();
                dayTime.setToNow();

                // we start at the day returned by local time. Otherwise this is a mess.
                int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

                // now we work exclusively in UTC
                dayTime = new Time();

                String[] resultStrs = new String[numDays];
                for(int i = 0; i < weatherArray.length(); i++) {
                    // For now, using the format "Day, description, hi/low"
                    String day;
                    String description;
                    String highAndLow;

                    // Get the JSON object representing the day
                    JSONObject dayForecast = weatherArray.getJSONObject(i);

                    // The date/time is returned as a long.  We need to convert that
                    // into something human-readable, since most people won't read "1400356800" as
                    // "this saturday".
                    long dateTime;
                    // Cheating to convert this to UTC time, which is what we want anyhow
                    dateTime = dayTime.setJulianDay(julianStartDay+i);
                    day = getReadableDateString(dateTime);

                    // description is in a child array called "weather", which is 1 element long.
                    JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    description = weatherObject.getString(OWM_DESCRIPTION);

                    // Temperatures are in a child object called "temp".  Try not to name variables
                    // "temp" when working with temperature.  It confuses everybody.
                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                    double high = temperatureObject.getDouble(OWM_MAX);
                    double low = temperatureObject.getDouble(OWM_MIN);

                    highAndLow = formatHighLows(high, low);
                    resultStrs[i] = day + " - " + description + " - " + highAndLow;
                }

                /*for (String s : resultStrs) {
                    Log.v(LOG_TAG, "Forecast entry: " + s);
                }*/
                return resultStrs;

            }


            @Override
            protected String[] doInBackground(String... strings) {
                // These two need to be declared outside the try/catch
                // so that they can be closed in the finally block.
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                // Will contain the raw JSON response as a string.
                String forecastJsonStr = null;

                //URI builder elements
                String zipCode = "30067";
                String mode = "json";
                String units = "metric";
                if (strings != null) {
                    switch (strings.length) {
                        case 2:
                            units=strings[1];
                        case 1:
                            zipCode = strings[0];
                            break;
                    }
                }

                int cnt = 7;

                try {
                    // Construct the URL for the OpenWeatherMap query
                    // Possible parameters are avaiable at OWM's forecast API page, at
                    // http://openweathermap.org/API#forecast
                    Uri.Builder opeenWeatherBuilder = new Uri.Builder();
                    opeenWeatherBuilder.scheme("http");
                    opeenWeatherBuilder.authority("api.openweathermap.org");

                    opeenWeatherBuilder.path(new StringBuilder().append("data/2.5/forecast/daily").toString())
                    .appendQueryParameter("zip", zipCode)
                    .appendQueryParameter("mode", mode)
                            .appendQueryParameter("units", units)
                            .appendQueryParameter("cnt", String.valueOf(cnt));
                    //Log.v(LOG_TAG, opeenWeatherBuilder.toString());

                    URL url = new URL(opeenWeatherBuilder.toString());

                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        return null;
                    }
                    forecastJsonStr = buffer.toString();
                   // Log.v(LOG_TAG, forecastJsonStr);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error ", e);
                    // If the code didn't successfully get the weather data, there's no point in attemping
                    // to parse it.
                    return null;
                } finally{
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e(LOG_TAG, "Error closing stream", e);
                        }
                    }
                }
                try {
                    return getWeatherDataFromJson(forecastJsonStr,cnt);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error ", e);
                    return null;
                }
            }
        }
    }

