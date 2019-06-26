
package com.sierzega.nutproble.ble;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sierzega.nutproble.R;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


//W przypadku rozbudowy klasy dostosować do modelu MVP
public class FragmentConnection extends Fragment {
    public static final String TAG = FragmentConnection.class.getSimpleName();
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView devicesRecyclerView;
    private OnFragmentInteractionListener activityInteraction;


    //TEST COMPONENTS
    private Button buttonStart, buttonStop, buttonConnect, buttonDisconnect, buttonSend;
    private EditText editTextCommand;

    public FragmentConnection() {
        // Required empty public constructor
    }

    public interface OnFragmentInteractionListener {
        Connection getConnection();
    }


    public static FragmentConnection newInstance() {
        FragmentConnection fragment = new FragmentConnection();
        Log.i(TAG, "Fragment connection new instance created");
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() instanceof OnFragmentInteractionListener) {
            activityInteraction = (OnFragmentInteractionListener) getContext();
        } else {
            throw new RuntimeException(getContext().toString()
                    + " must implement OnFragmentInteractionListener");
        }
        Log.i(TAG, TAG + " creation started");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, TAG + " view created");

        //VIEW COMPONENTS INITIALIZATION
        devicesRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_connection_recycler_view);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //END OF VIEW COMPONTENTS INITLAIITON


        List<BluetootDevice> testData = new ArrayList<>();
        testData.add(new BluetootDevice("bbbbb","111:222:333"));
        testData.add(new BluetootDevice("bbbbb2","2111:222:333"));
        testData.add(new BluetootDevice("bbbbb3","3111:222:333"));
        specifyDevicesListBehaviourAndRefreshData(testData);


        swipeRefreshLayout.setOnRefreshListener(() -> {
            Toast.makeText(getContext(), "Refresh works good", Toast.LENGTH_SHORT).show();
            fetchBluetoothDevices();
        });

    }

    private void fetchBluetoothDevices(){
        Single<List<BluetootDevice>> deviceSingle = Single.fromCallable(() ->
                activityInteraction.getConnection().getBleScanner().scanForDevices());

        deviceSingle.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<BluetootDevice>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(List<BluetootDevice> bluetoothDevices) {
                Log.i(TAG, "Success");
                specifyDevicesListBehaviourAndRefreshData(bluetoothDevices);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "Error");
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }



    private void specifyDevicesListBehaviourAndRefreshData(final List<BluetootDevice> devices) {
        DevicesAdapter listAdapter = new DevicesAdapter(devices, (v, pos) ->
        {
            Toast.makeText(getContext(), "Device name: " + devices.get(pos).getName(), Toast.LENGTH_SHORT).show();
            activityInteraction.getConnection().connectToDevice(devices.get(pos).getAddress());
        }


        );
        devicesRecyclerView.setAdapter(listAdapter);
        devicesRecyclerView.setItemAnimator(new DefaultItemAnimator());
        Log.i(TAG, TAG + " devices list data changed");
    }
}
