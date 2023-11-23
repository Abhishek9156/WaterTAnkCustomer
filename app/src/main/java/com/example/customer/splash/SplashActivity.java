package com.example.customer.splash;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.customer.MainActivity;
import com.example.customer.R;
import com.example.customer.common.Common;
import com.example.customer.model.RiderModel;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;

public class SplashActivity extends AppCompatActivity {
    private final static int LOGIN_REQUEST_CODE = 7171;
    private List<AuthUI.IdpConfig> provider;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;

    ProgressBar progressBar;

    FirebaseDatabase database;
    DatabaseReference driverInfoRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        progressBar = findViewById(R.id.progress_bar);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        delaySplashScreen();
    }

    private void delaySplashScreen() {
        progressBar.setVisibility(View.VISIBLE);
        Completable.timer(3, TimeUnit.SECONDS,
                        AndroidSchedulers.mainThread())
                .subscribe(() ->
                        firebaseAuth.addAuthStateListener(listener)


                );
    }

    @Override
    protected void onStop() {
        if (firebaseAuth != null && listener != null) {
            firebaseAuth.removeAuthStateListener(listener);
        }
        super.onStop();
    }
    public  void init(){
        database = FirebaseDatabase.getInstance();
        driverInfoRef = database.getReference(Common.RIDER_INFO_REFENCE);
        provider = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth -> {
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user != null) {
                checkUserFromFireBase();
            } else {
                showLoginLayout();
            }
        };
    }

    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_phone_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(provider).build(), LOGIN_REQUEST_CODE);
    }

    private void checkUserFromFireBase() {

        driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Toast.makeText(SplashScreenActivity.this, "User Already register", Toast.LENGTH_SHORT).show();
                            RiderModel driverInfoModel = snapshot.getValue(RiderModel.class);
                            GotoHomeActivity(driverInfoModel);
                        } else {
                            showRegisterLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SplashActivity.this, "Check user fire" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        TextInputEditText ed_first_name = view.findViewById(R.id.edit_first_name);
        TextInputEditText ed_last_name = view.findViewById(R.id.edit_last_name);
        TextInputEditText ed_phone_number = view.findViewById(R.id.edit_phone_number);
        Button btn_continue = view.findViewById(R.id.btn_register);

        if (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
            ed_phone_number.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

        builder.setView(view);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        btn_continue.setOnClickListener(view1 -> {
            if (TextUtils.isEmpty(ed_first_name.getText().toString())) {
                Toast.makeText(this, "Please enter first name", Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(ed_last_name.getText().toString())) {
                Toast.makeText(this, "Please enter last name", Toast.LENGTH_SHORT).show();
                return;

            } else if (TextUtils.isEmpty(ed_phone_number.getText().toString())) {
                Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
                return;
            } else {
                RiderModel model = new RiderModel();
                model.setFirstName(ed_first_name.getText().toString());
                model.setLastName(ed_last_name.getText().toString());
                model.setPhoneNumber(ed_phone_number.getText().toString());


                driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(model)
                        .addOnFailureListener(error -> {
                            Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        })
                        .addOnSuccessListener(save -> {
                            Toast.makeText(this, "Register successful", Toast.LENGTH_SHORT).show();
                            alertDialog.dismiss();
                            GotoHomeActivity(model);
                        });

            }
        });
    }

    private void GotoHomeActivity(RiderModel driverInfoModel) {
        Common.currentRider = driverInfoModel;
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            } else {
                Toast.makeText(this, "[ERROR]: " + response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}