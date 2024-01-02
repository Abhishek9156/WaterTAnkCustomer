package com.example.customer.utills;

import android.widget.RelativeLayout;

import com.example.customer.DriverRequestActivity;
import com.example.customer.model.DriverGeoModel;
import com.example.customer.remote.IFCMService;
import com.example.customer.remote.RetrofitClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Firebase;
import com.google.firebase.database.FirebaseDatabase;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class UserUtills {
    public static void sendRequestToDriver(DriverRequestActivity driverRequestActivity, RelativeLayout mainLayout, DriverGeoModel foundDriver, LatLng target) {
        CompositeDisposable compositeDisposable=new CompositeDisposable();
        IFCMService ifcmService= RetrofitClient.getInstance().create(IFCMService.class);

        //Get Token
        //FirebaseDatabase.getInstance().getReference()
    }
}
