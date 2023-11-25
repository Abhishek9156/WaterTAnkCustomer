package com.example.customer.remote;





import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IGoogleApi {
    @GET("maps/api/directions/json")
    Observable<String> getDirections(
            @Query("mode") String mode,
            @Query("transit_routing_perference") String transit_routing,
            @Query("origin") String from,
            @Query("destination") String to,
            @Query("key") String key
            );
}
