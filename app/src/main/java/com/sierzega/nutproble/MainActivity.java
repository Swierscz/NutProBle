package com.sierzega.nutproble;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sierzega.nutproble.ble.Connection;
import com.sierzega.nutproble.ble.FragmentConnection;

public class MainActivity extends AppCompatActivity implements FragmentConnection.OnFragmentInteractionListener {

public final static String TAG = MainActivity.class.getSimpleName();
public Connection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction().add(R.id.content_main, FragmentConnection.newInstance()).commit();
        connection = new Connection(this);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
