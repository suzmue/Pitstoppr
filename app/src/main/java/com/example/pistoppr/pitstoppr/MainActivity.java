package com.example.pistoppr.pitstoppr;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.flurry.android.FlurryAgent;


public class MainActivity extends ActionBarActivity {
    public String test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlurryAgent.init(this, "6629BN3RTQW9N2XGK5M5");
        setContentView(R.layout.activity_main);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Called when the user clicks the Start Trip Button */
    public void startTrip(View view){
        //Start a new view with the trip
        Intent intent = new Intent(this, TripActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Settings Button */
    public void openSettings(View view){
        //Opens the settings
        Intent intent = new Intent(this, Settings2Activity.class);
        startActivity(intent);
    }
}
