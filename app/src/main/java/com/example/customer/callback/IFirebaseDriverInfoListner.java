package com.example.customer.callback;

import com.example.customer.model.DriverGeoModel;

public interface IFirebaseDriverInfoListner {
    void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel);
}
