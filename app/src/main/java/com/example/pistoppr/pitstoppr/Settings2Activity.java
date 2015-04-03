package com.example.pistoppr.pitstoppr;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.view.View;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;


public class Settings2Activity extends ActionBarActivity {

    //SharedPreferences.Editor editor;
    //StringBuilder listOfRestaurants;
    Set<String> defaultRestaurants;
    Set<String> mySetOfRestaurants;
    SharedPreferences restaurantPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings2);
        //listOfRestaurants = new StringBuilder();
        defaultRestaurants = new HashSet<String>();
        restaurantPreferences = getPreferences(MODE_PRIVATE);
        mySetOfRestaurants = restaurantPreferences.getStringSet("restaurants", defaultRestaurants);
        StringBuilder restaurantString = new StringBuilder();
        for (String res : mySetOfRestaurants) {
            restaurantString.append(res);
            restaurantString.append("\r\n");
        }
        TextView restaurantTextView = (TextView) findViewById(R.id.textView3);
        restaurantTextView.setText(restaurantString);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void addRestaurant(View view) {
        editor = restaurantPreferences.edit();
        editor.clear();
        editor.commit();
        EditText restaurant = (EditText) findViewById(R.id.editText);
        TextView restaurantTextView = (TextView) findViewById(R.id.textView3);
        mySetOfRestaurants.add(restaurant.getText().toString());
        ((EditText) findViewById(R.id.editText)).setText("");
        restaurantPreferences = getPreferences(MODE_PRIVATE);
        editor.putStringSet("restaurants", mySetOfRestaurants);
        editor.commit();
        StringBuilder restaurantString = new StringBuilder();
        for (String res : mySetOfRestaurants){
            restaurantString.append(res);
            restaurantString.append("\r\n");
        }
        restaurantTextView.setText(restaurantString);
    }

    public void addItemsToSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.mile_range_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
    }

    public void clearRestaurants(View view) {
        editor = restaurantPreferences.edit();
        editor.clear();
        editor.commit();
        mySetOfRestaurants.clear();
        TextView restaurantTextView = (TextView) findViewById(R.id.textView3);
        restaurantTextView.setText("");
    }
}


