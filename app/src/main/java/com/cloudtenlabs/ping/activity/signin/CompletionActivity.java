package com.cloudtenlabs.ping.activity.signin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.cloudtenlabs.ping.BuildConfig;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

public class CompletionActivity extends AppCompatActivity {

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completion);

        context = this;

        CardView beginButton = findViewById(R.id.beginButton);
        beginButton.setOnClickListener(view -> {
            Intent intent = new Intent(context, DrawerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        if (!GlobalVariable.getInstance().mTracking) {
            AlertDialog.Builder permissionDialog = new AlertDialog.Builder(context);
            permissionDialog.setTitle("Allow ping connects to Access Your Location");
            permissionDialog.setMessage("Allow ping connects to access this device's location so that you can receive ping notifications from other users on the next page.");
            permissionDialog.setPositiveButton("NEXT", (dialog, which) -> requestLocationPermission());
//            permissionDialog.setNegativeButton("Don't Allow", null);
            permissionDialog.show();
        }
    }

    private void requestLocationPermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        GlobalVariable.getInstance().gpsService.startTracking();
                        GlobalVariable.getInstance().mTracking = true;
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            Intent intent = new Intent();
                            intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }
}