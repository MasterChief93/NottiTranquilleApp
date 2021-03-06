package com.efcompany.nottitranquille;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.efcompany.nottitranquille.sortingtools.SortInverter;
import com.efcompany.nottitranquille.sortingtools.Sorter;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ResultsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    static final int LOG_IN = 1;
    static final int CONNECT = 2;
    static final int REGISTRATION = 3;

    TextView query;
    ListView lv;

    //Ordering
    Spinner orderby;
    Spinner ascDesc;
    String[] orderWhat;
    String[] orderHow;
    Sorter sorter = new Sorter();

    ArrayList<HashMap<String, String>> locationsList;

    // JSON Node names
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_NATION = "nation";
    private static final String TAG_CITY = "city";
    private static final String TAG_CHECKIN = "checkin";
    private static final String TAG_CHECKOUT = "checkout";
    private static final String TAG_PRICERANGE = "price";
    private static final String TAG_LOCATIONS = "locations";
    private static final String TAG_NAME = "name";
    private static final String TAG_IMAGE = "image";
    private static final String TAG_TYPE = "type";
    private static final String TAG_ID = "id";


    String what = TAG_NATION;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        query = (TextView) findViewById(R.id.tvQuery);
        orderby = (Spinner) findViewById(R.id.spResOrderby);
        ascDesc = (Spinner) findViewById(R.id.spResAscDesc);
        lv = (ListView) findViewById(android.R.id.list);

        orderWhat = new String[]{getString(R.string.strNation), getString(R.string.strCity),
                getString(R.string.strName), getString(R.string.strType), getString(R.string.strPrice)};

        orderHow = new String[]{getString(R.string.strAscending), getString(R.string.strDescending)};

        // Getting result details from intent
        Intent i = getIntent();

        query.setText(i.getStringExtra("query"));


        ArrayAdapter<String> wadapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, orderWhat);

        orderby.setAdapter(wadapter);

        ArrayAdapter<String> hadapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, orderHow);

        ascDesc.setAdapter(hadapter);

        // Hashmap for ListView
        locationsList = new ArrayList<HashMap<String, String>>();


        String locjson = i.getStringExtra("json");

        JSONArray json = null;
        try {
            json = new JSONArray(locjson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        assert json != null;
        for (int j = 0; j < json.length(); j++) {
            JSONObject result = null;
            try {
                result = json.getJSONObject(j);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            assert result != null;
            Iterator<String> iter = result.keys();
            HashMap<String, String> hash = new HashMap<>();
            while (iter.hasNext()) {
                String key = iter.next();
                try {
                    hash.put(key, result.getString(key));
                } catch (JSONException e) {
                    // Something went wrong!
                }
            }
            locationsList.add(hash);
        }
        //Updating parsed JSON data into ListView
        ListAdapter adapter = new SimpleAdapter(
                this, locationsList,
                R.layout.list_results, new String[]{
                TAG_NATION, TAG_CITY, TAG_NAME, TAG_TYPE, TAG_PRICERANGE, TAG_ID, TAG_IMAGE},
                new int[]{R.id.tvResNation, R.id.tvResCity, R.id.tvResName, R.id.tvResType, R.id.tvResPrice, R.id.tvResID,
                        R.id.ivResImage1});
        // Updating listview
        lv.setAdapter(adapter);

        //Set Listeners
        orderby.setOnItemSelectedListener(ResultsActivity.this);
        ascDesc.setOnItemSelectedListener(ResultsActivity.this);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent in = new Intent(ResultsActivity.this, LocationActivity.class);
                String resID = ((TextView) view.findViewById(R.id.tvResID)).getText().toString();
                String resName = ((TextView) view.findViewById(R.id.tvResName)).getText().toString();
                String resNation = ((TextView) view.findViewById(R.id.tvResNation)).getText().toString();
                String resCity = ((TextView) view.findViewById(R.id.tvResCity)).getText().toString();
                String resType = ((TextView) view.findViewById(R.id.tvResType)).getText().toString();
                in.putExtra(TAG_ID, resID);
                in.putExtra(TAG_NAME, resName);
                in.putExtra(TAG_TYPE, resType);
                in.putExtra(TAG_NATION, resNation);
                in.putExtra(TAG_CITY, resCity);
                startActivity(in);
            }
        });


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if (locationsList.size() != 0) {

            if (parent.getId() == orderby.getId()) {
                switch (position) {
                    case 0:
                        what = TAG_NATION;
                        break;
                    case 1:
                        what = TAG_CITY;
                        break;
                    case 2:
                        what = TAG_NAME;
                        break;
                    case 3:
                        what = TAG_TYPE;
                        break;
                    case 4:
                        what = TAG_PRICERANGE;
                        break;
                }
                //Call the Sorter
                locationsList = sorter.sorter(locationsList, what);
                //If "Descending", invert the list
                if (ascDesc.getSelectedItemPosition() == 1) {
                    locationsList = SortInverter.inverter(locationsList);
                }

            } else if (parent.getId() == ascDesc.getId()) {

                switch (position) {
                    case 0:
                        locationsList = sorter.sorter(locationsList, what);
                        break;
                    case 1:
                        locationsList = SortInverter.inverter(locationsList);
                        break;
                }
            }

            //Updating parsed JSON data into ListView
            ListAdapter adapter = new SimpleAdapter(
                    this, locationsList,
                    R.layout.list_results, new String[]{
                    TAG_NATION, TAG_CITY, TAG_NAME, TAG_TYPE, TAG_PRICERANGE, TAG_IMAGE, TAG_ID},
                    new int[]{R.id.tvResNation, R.id.tvResCity, R.id.tvResName, R.id.tvResType, R.id.tvResPrice,
                            R.id.ivResImage1,R.id.tvResID});
            // Updating listview
            lv.setAdapter(adapter);

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Results Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.efcompany.nottitranquille/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Results Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.efcompany.nottitranquille/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
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
