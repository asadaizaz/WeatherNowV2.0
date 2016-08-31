package com.asad.android.weathernowv2;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Asad on 8/30/2016.
 */
public class WeatherFragment extends Fragment {
    private Weather mWeather;
    public static final String TAG = WeatherActivity.class.getSimpleName();

    private String loc;

    @BindView(R.id.timeLabel)
    TextView mTimeLabel;
    @BindView(R.id.temperatureLabel)
    TextView mTemperatureLabel;
    @BindView(R.id.humidityValue)
    TextView mHumidityValue;
    @BindView(R.id.precipValue)
    TextView mPrecipValue;
    @BindView(R.id.summaryLabel)
    TextView mSummaryLabel;
    @BindView(R.id.iconImageView)
    ImageView mImageView;
    @BindView(R.id.refreshImageView)
    ImageView mRefreshImageView;
    @BindView(R.id.locationLablel)
    TextView mLocationLabel;
    @BindView(R.id.progressBar)
    ProgressBar mProgressBar;
    EditText mEditText;
    private int MY_PERMISSION;

    private int degree = Weather.DEGREE_CELSIUS;

    private boolean isOnCreateFinished = false;

    private boolean permissionGranted = false;
    double latitude = 37.89;
    double longitude = -127.63;

    LocationService mLocationService;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "_______________________________________________________STARTED!!!!!!!!!!!!!!!!!!!!");


        getForecastInitial(latitude, longitude);


        Log.d(TAG, "Main UI code is running");

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_weather, container, false);


        ButterKnife.bind(this, v);
        Log.d(TAG, "Binded views");
        getLocation();
        isOnCreateFinished = true;
        mProgressBar.setVisibility(View.INVISIBLE);

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(latitude, longitude);
                //toggleRefresh();
            }
        });


        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        getForecast(latitude, longitude);
    }

    private boolean getPremission() {


        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION);
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions [], int[] grantResults)
    {

            if(requestCode== MY_PERMISSION)
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Permission granted");
                    permissionGranted = true;
                    getForecast(latitude,longitude);
                }
                else
                {
                    Log.d(TAG,"Permission denied");
                }
                return;
            }
        else
                return;

        }

    private void getLocation() {


        LocationService locationService = new LocationService(getActivity());


        longitude = locationService.getLongitude();
        latitude = locationService.getLatitude();


        if (longitude == 0 && latitude == 0) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

                if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION))
                {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Location Permission")
                            .setMessage("This app needs your location in order to correctly display the weather, please go to settings and enable location access.")
                            .setPositiveButton("SETTINGS", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent viewIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                                    startActivity(viewIntent);
                                }
                            })
                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                }
                else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION);

                }
            }

        } else {
            permissionGranted = true;


            Geocoder geoCoder = new Geocoder(getActivity(), Locale.getDefault());
            StringBuilder builder = new StringBuilder();
            try {
                List<Address> address = geoCoder.getFromLocation(latitude, longitude, 1);

                if (address.size() >= 0) {
                    String addressCity = address.get(0).getLocality();
                    //   String addressProvince = address.get(0).getAdminArea();
                    String addressCountry = address.get(0).getCountryCode();

                    if (addressCountry == "US")
                        degree = Weather.DEGREE_FARENHIET;

                    Log.d(TAG, addressCountry);
                    builder.append(addressCity);
                    //    builder.append(",");
                    //      builder.append(addressProvince);
                }


                loc = builder.toString();
            } catch (IOException e) {
            } catch (NullPointerException e) {
            }
        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }

        return isAvailable;

    }

    private void getForecastInitial(double latitude, double longitude) {
        String apiKey = "406c3c1c926fc6b6731c75a69641d031";


        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey +
                "/" + latitude + "," + longitude;


        if (isNetworkAvailable()) {

            getLocation();
            if (permissionGranted) {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(forecastUrl)
                        .build();
                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                            }
                        });
                        errorPopUp();

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });

                        try {
                            String jsonData = response.body().string();
                            Log.v(TAG, jsonData);
                            if (response.isSuccessful()) {
                                mWeather = getCurrentDetails(jsonData);
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateDisplay();

                                    }
                                });

                            } else {
                                errorPopUp();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Exception caught: ", e);
                        } catch (JSONException e) {
                            Log.e(TAG, "Exception caught: ");

                        }
                    }
                });
            } else {
                if(permissionGranted=true)
                Toast.makeText(getActivity(), R.string.nework_unavailable_message, Toast.LENGTH_LONG).show();
            }
        }
        else {
            AlertDialogManager alertDialogManager = new AlertDialogManager();
            alertDialogManager.showAlertDialog(getContext(), "Error", "Location not granted, pelase go to settings and enable location services", Boolean.TRUE);
        }
    }


    private void getForecast(double latitude, double longitude) {
        String apiKey = "406c3c1c926fc6b6731c75a69641d031";


        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey +
                "/" + latitude + "," + longitude;

            if (isNetworkAvailable()) {


                getLocation();
                if (permissionGranted) {

                    if (isOnCreateFinished)
                    toggleRefresh();


                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(forecastUrl)
                        .build();
                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (isOnCreateFinished)
                                    toggleRefresh();
                            }
                        });
                        errorPopUp();

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (isOnCreateFinished)
                                    toggleRefresh();

                            }
                        });

                        try {
                            String jsonData = response.body().string();
                            Log.v(TAG, jsonData);
                            if (response.isSuccessful()) {
                                mWeather = getCurrentDetails(jsonData);

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateDisplay();

                                    }
                                });

                            } else {
                                errorPopUp();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Exception caught: ", e);
                        } catch (JSONException e) {
                            Log.e(TAG, "Exception caught: ");

                        }
                    }
                });
                } else {
                    Toast.makeText(getActivity(), "Location permission denied!", Toast.LENGTH_LONG).show();

                }
            }
            else {
                if(permissionGranted=true)
                    Toast.makeText(getActivity(), R.string.nework_unavailable_message, Toast.LENGTH_LONG).show();

            }

    }

    private void errorPopUp() {


        //   AlertDialogFragment dialog = new AlertDialogFragment();

        //  dialog.show(getFragmentManager(), "error_dialog");

    }


    private Weather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timeZone = forecast.getString("timezone");
        Log.i(TAG, "Frome JSON: " + timeZone);

        JSONObject currently = forecast.getJSONObject("currently");

        Weather weather = new Weather();
        weather.setIcon(currently.getString("icon"));
        weather.setHumidity(currently.getDouble("humidity"));
        weather.setTime(currently.getLong("time"));
        weather.setPrecipChance(currently.getDouble("precipProbability"));
        weather.setSummary(currently.getString("summary"));
        weather.setTemperature( currently.getDouble("temperature"));
        weather.setTimeZone(timeZone);
        weather.setLocationLabel(loc);
        Log.d(TAG, weather.getFormattedTime());
        return weather;


    }

    private void updateDisplay() {

        mTimeLabel.setText("At " + mWeather.getFormattedTime() + " it will be");
        mHumidityValue.setText(mWeather.getHumidity() + "");
        mPrecipValue.setText(mWeather.getPrecipChance() + "%");
        mSummaryLabel.setText(mWeather.getSummary());
        mLocationLabel.setText(mWeather.getLocationLabel());
        mTemperatureLabel.setText(""+(mWeather.getTemperature(1)));
        Drawable drawable = getResources().getDrawable(mWeather.getIconId());
        mImageView.setImageDrawable(drawable);
    }
    private void toggleRefresh() {
        Log.d(TAG, "stuff");





            if (mProgressBar.getVisibility() == View.INVISIBLE) {
                mProgressBar.setVisibility(View.VISIBLE);
                mRefreshImageView.setVisibility(View.INVISIBLE);
            } else {
                mProgressBar.setVisibility(View.INVISIBLE);
                mRefreshImageView.setVisibility(View.VISIBLE);
            }





    }
}
