package com.efcompany.nottitranquille;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.efcompany.nottitranquille.extratools.AppController;
import com.efcompany.nottitranquille.model.SearchData;
import com.google.gson.Gson;

import org.joda.time.DateTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    static final int LOG_IN = 1;
    static final int CONNECT = 2;
    static final int REGISTRATION = 3;

    EditText etNation;
    EditText etCity;
    DatePicker dpCheckIn;
    DatePicker dpCheckOut;
    Spinner spPriceRange;
    Button bAdvancedSearch;
    Button bSearch;

    String[] priceRanges;

    String nation;
    String city;
    int price;

    // JSON Node names
    private static final String TAG_SUCCESS = "code";
    private static final String TAG_MESSAGE = "message";
    private static final String TAG_NATION = "nation";
    private static final String TAG_CITY = "city";
    private static final String TAG_CHECKIN = "checkin";
    private static final String TAG_CHECKOUT = "checkout";
    private static final String TAG_PRICERANGE = "pricerange";
    private static final String TAG_LOCATIONS = "results";
    private static final String TAG_SOURCE = "source";

    private String site;
    SearchData query;

    // Progress Dialog
    View mProgressView;

    Gson gson;
    JSONArray locsjson = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etNation = (EditText) findViewById(R.id.etNation);
        etCity = (EditText) findViewById(R.id.etCity);
        dpCheckIn = (DatePicker) findViewById(R.id.dpCheckIn);
        dpCheckOut = (DatePicker) findViewById(R.id.dpCheckOut);
        spPriceRange = (Spinner) findViewById(R.id.spPriceRange);
        bAdvancedSearch = (Button) findViewById(R.id.bAdvSearch);
        bSearch = (Button) findViewById(R.id.bSearch);

        query = new SearchData();

        priceRanges = new String[]{getString(R.string.strAny), getString(R.string.strBelow100), getString(R.string.strBelow200),
                getString(R.string.strBelow500), getString(R.string.strOver500)};

        ArrayAdapter<String> padapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, priceRanges);

        // Show a progress spinner, and perform the conection attempt.
        mProgressView = findViewById(R.id.searchProgress);

        //Get the URL
        SharedPreferences sharedPref = this.getSharedPreferences("com.efcompany.nottitranquille", MODE_PRIVATE);
        site = sharedPref.getString("connectto", "");
        if (site.equals("")) {
            Toast.makeText(this, R.string.strNoSite, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ConnectionActivity.class);
            startActivity(intent);
        }
        site += "/api/search.jsp";

        gson = new Gson();

        spPriceRange.setAdapter(padapter);
        spPriceRange.setOnItemSelectedListener(this);

        bAdvancedSearch.setOnClickListener(this);
        bSearch.setOnClickListener(this);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                query.setPricerange("Fino+a+100+euro");
                break;
            case 1:
                query.setPricerange("Fino+a+200+euro");
                break;
            case 2:
                query.setPricerange("Fino+a+500+euro");
                break;
            case 3:
                query.setPricerange("Nessun+limite");
                break;
        }
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        showProgress(true);
        if (v.getId() == bSearch.getId()) {
            nation = etNation.getText().toString();
            city = etCity.getText().toString();
            boolean cancel = false;
            View focusView = null;

            // Check if the user filed the form.
            if (TextUtils.isEmpty(nation)) {
                etNation.setError(getString(R.string.error_field_required));
                focusView = etNation;
                cancel = true;
            }
            if (TextUtils.isEmpty(city)) {
                etCity.setError(getString(R.string.error_field_required));
                focusView = etCity;
                cancel = true;
            }


            DateTime checkin = new DateTime(dpCheckIn.getCalendarView().getDate());
            DateTime checkout = new DateTime(dpCheckOut.getCalendarView().getDate());
            if (cancel) {
                // There was an error; don't attempt login and focus the first
                // form field with an error.

                showProgress(false);
                focusView.requestFocus();
            }
            else if (checkout.isBefore(checkin)) {
                showProgress(false);
                Toast.makeText(this, getString(R.string.strerrWrongDate), Toast.LENGTH_LONG).show();
            } else {
                //Log.d("Date",new DateTime(dpCheckIn.getCalendarView().getDate()).toString() );
                //Gather the data for the query
                query.setNation(nation);
                query.setCity(city);
                query.setCheckin(checkin.toString("dd-MM-yyyy"));
                query.setCheckout(checkout.toString("dd-MM-yyyy"));
                StringRequest postRequest = new StringRequest(Request.Method.POST, site,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.i("Parser", response);
                                try {
                                    JSONObject json_response = new JSONObject(response);
                                    if (json_response.getString(TAG_SUCCESS).equals("1")) {
                                        // Locations found
                                        // Getting Array of Locations
                                        locsjson = json_response.getJSONArray(TAG_LOCATIONS);

//                                    }
                                        showProgress(false);
                                        Intent in = new Intent(SearchActivity.this, ResultsActivity.class);
                                        in.putExtra("json", locsjson.toString());
                                        in.putExtra("query", getString(R.string.strResultsQuery) + query.toString());
                                        startActivity(in);


                                    } else {
                                        showProgress(false);
                                        Toast.makeText(SearchActivity.this, R.string.strerrNoLocation, Toast.LENGTH_LONG).show();
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // error
                                Log.d("Parser", error.getMessage());
                            }
                        }
                ) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<String, String>();
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(gson.toJson(query));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        assert jsonObject != null;
                        Iterator<String> iter = jsonObject.keys();
                        HashMap<String, String> hash = new HashMap<>();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            try {
                                params.put(key, jsonObject.getString(key));
                            } catch (JSONException e) {
                                // Something went wrong!
                            }
                        }
                        return params;
                    }

                    /* (non-Javadoc)
   * @see com.android.volley.toolbox.StringRequest#parseNetworkResponse(com.android.volley.NetworkResponse)
   */
                    @Override
                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                        // since we don't know which of the two underlying network vehicles
                        // will Volley use, we have to handle and store session cookies manually
                        AppController.getInstance().checkSessionCookie(response.headers);

                        return super.parseNetworkResponse(response);
                    }

                    /* (non-Javadoc)
                     * @see com.android.volley.Request#getHeaders()
                     */
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = super.getHeaders();

                        if (headers == null || headers.equals(Collections.emptyMap())) {
                            headers = new HashMap<String, String>();
                        }

                        AppController.getInstance().addSessionCookie(headers);
                        return headers;
                    }
                };
                AppController.getInstance().addToRequestQueue(postRequest);
            }
        }
        if (v.getId() == bAdvancedSearch.getId()) {
            // Starting new intent
            Intent in = new Intent(SearchActivity.this, AdvancedSearchActivity.class);
            // Sending info to next activity
            in.putExtra(TAG_NATION, etNation.getText().toString());
            in.putExtra(TAG_CITY, etCity.getText().toString());
            in.putExtra(TAG_CHECKIN, dpCheckIn.getCalendarView().getDate());
            in.putExtra(TAG_CHECKOUT, dpCheckOut.getCalendarView().getDate());
            in.putExtra(TAG_PRICERANGE, price);
            in.putExtra(TAG_SOURCE, 1);
            // starting new activity
            startActivity(in);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);


            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == LOG_IN) {
            // Make sure the request was successful
            if (resultCode != RESULT_OK) {
                finish();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id ==R.id.connection_settings){
            Intent intent = new Intent(this, ConnectionActivity.class);
            startActivityForResult(intent, CONNECT);
        }
        if (id ==R.id.login_settings){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, LOG_IN);
        }
        if (id ==R.id.signup_settings){
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivityForResult(intent, REGISTRATION);
        }
        return super.onOptionsItemSelected(item);
    }

}
