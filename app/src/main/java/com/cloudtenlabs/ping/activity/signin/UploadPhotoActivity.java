package com.cloudtenlabs.ping.activity.signin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cloudtenlabs.ping.R;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.nguyenhoanglam.imagepicker.model.Config;
import com.nguyenhoanglam.imagepicker.model.Image;
import com.nguyenhoanglam.imagepicker.ui.imagepicker.ImagePicker;

import java.util.ArrayList;

public class UploadPhotoActivity extends AppCompatActivity {

    private Context context;

    private ArrayList<String> mResults = new ArrayList<>();
    private ArrayList<Image> images = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_photo);

        context = this;

        Fresco.initialize(this);

        TextView uploadNowButton = findViewById(R.id.uploadNowButton);
        uploadNowButton.setOnClickListener(view -> checkPermission());
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder permissionDialog = new AlertDialog.Builder(this);
            permissionDialog.setTitle("Allow ping connects to Access Your Photo");
            permissionDialog.setMessage("ping connects requests to access your photo library so that you may choose a photo to upload to your ping profile on the next page.");
            permissionDialog.setPositiveButton("NEXT", (dialog, which) -> ActivityCompat.requestPermissions(UploadPhotoActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10000));
//            permissionDialog.setNegativeButton("Don't Allow", null);
            permissionDialog.show();
        } else {
            openImagePicker();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 10000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void openImagePicker() {
        ImagePicker.with(this)
                .setFolderMode(true)
                .setCameraOnly(false)
                .setFolderTitle("Album")
                .setMultipleMode(true)
                .setSelectedImages(images)
                .setMaxSize(4)
                .setBackgroundColor("#212121")
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Config.RC_PICK_IMAGES && resultCode == RESULT_OK && data != null) {
            images = data.getParcelableArrayListExtra(Config.EXTRA_IMAGES);
            mResults.clear();
            for (Image image :
                    images) {
                mResults.add(image.getPath());
            }
            Intent intent = new Intent(context, UploadedPhotoActivity.class);
            intent.putStringArrayListExtra("selected_images", mResults);
            startActivity(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
