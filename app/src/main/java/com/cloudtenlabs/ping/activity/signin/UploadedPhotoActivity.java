package com.cloudtenlabs.ping.activity.signin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.core.ImageCompressTask;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.listeners.IImageCompressTaskListener;
import com.cloudtenlabs.ping.object.Photo;
import com.google.gson.JsonObject;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadedPhotoActivity extends AppCompatActivity {

    private Context context;
    private ArrayList<String> selectedImages;
    private ArrayList<String> compressedImages = new ArrayList<>();

    private ExecutorService mExecutorService = Executors.newFixedThreadPool(1);
    private ImageCompressTask imageCompressTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploaded_photo);

        context = this;

        selectedImages = getIntent().getStringArrayListExtra("selected_images");
        for (int index = 0; index < selectedImages.size(); index++) {
            String selectedImage = selectedImages.get(index);
            File imgFile = new File(selectedImage);
            if (imgFile.exists()) {
//                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ImageView imageView;
                if (index == 0) {
                    imageView = findViewById(R.id.firstImageView);
                } else if (index == 1) {
                    imageView = findViewById(R.id.secondImageView);
                } else if (index == 2) {
                    imageView = findViewById(R.id.thirdImageView);
                } else {
                    imageView = findViewById(R.id.fourthImageView);
                }
//                imageView.setImageURI(Uri.parse(selectedImage));
//                Picasso.get().load(imgFile).into(imageView);
                Glide.with(this).load(Uri.fromFile(imgFile)).into(imageView);
            }
        }

        TextView nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(view -> {
            compressedImages.clear();
            imageCompressTask = new ImageCompressTask(context, selectedImages.get(0), iImageCompressTaskListener);
            mExecutorService.execute(imageCompressTask);
        });

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }

    public static void copyExifRotation(File sourceFile, File destFile) {
        if (sourceFile == null || destFile == null) return;
        try {
            ExifInterface exifSource = new ExifInterface(sourceFile.getAbsolutePath());
            ExifInterface exifDest = new ExifInterface(destFile.getAbsolutePath());
            exifDest.setAttribute(ExifInterface.TAG_ORIENTATION, exifSource.getAttribute(ExifInterface.TAG_ORIENTATION));
            exifDest.saveAttributes();
        } catch (IOException ignored) {
        }
    }

    private void uploadPhotos() {
        final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Registering...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        MultipartBody.Part[] images_data = new MultipartBody.Part[compressedImages.size()];
        String[] images_name = new String[compressedImages.size()];

        for (int index = 0; index < compressedImages.size(); index++) {
            String selectedImage = compressedImages.get(index);
            File imgFile = new File(selectedImage);
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), imgFile);
            String fileName = GlobalVariable.getInstance().loggedInUser.getId() + "_" + index + "_" + System.currentTimeMillis()/1000 + ".jpeg";
            images_data[index] = MultipartBody.Part.createFormData("photos[]", fileName, fileBody);
            images_name[index] = fileName;
        }

        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));

        Call<JsonObject> call = apiInterface.update_photos(userIdBody, images_data);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();

                if (success == 1) {
                    ArrayList<Photo> photos = new ArrayList<>();
                    for (String image_name:
                            images_name) {
                        Photo photo = new Photo();
                        photo.setPhoto(image_name);
                        photos.add(photo);
                    }
                    if (photos.size() > 0) {
                        QBUser qbUser = GlobalVariable.getInstance().loggedInUser.getQb_user();
                        qbUser.setCustomData(photos.get(0).getPhoto());

                        QBUsers.updateUser(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                            @Override
                            public void onSuccess(QBUser qbUser, Bundle bundle) {
                                progressDialog.dismiss();

                                GlobalVariable.getInstance().loggedInUser.setQb_user(qbUser);
                                GlobalVariable.getInstance().loggedInUser.setPhoto(photos.get(0).getPhoto());

                                GlobalVariable.getInstance().loggedInUser.setPhotos(photos);
                                GlobalVariable.getInstance().loggedInUser.setPush_notification("1");
                                GlobalVariable.getInstance().loggedInUser.setVibration("1");

                                Intent intent = new Intent(context, CompletionActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                progressDialog.dismiss();
                                Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Failed to upload the photos.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    progressDialog.dismiss();
                    String error = result.get("error").getAsString();
                    Toast.makeText(context, error.equals("") ? "An unknown error occurred." : error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });
    }

    private IImageCompressTaskListener iImageCompressTaskListener = new IImageCompressTaskListener() {
        @Override
        public void onComplete(List<File> compressed) {
            compressedImages.add(compressed.get(0).getAbsolutePath());

//            Log.d(TAG, String.valueOf(getOrientation(selectedImages.get(0))));
//            Log.d(TAG, String.valueOf(getOrientation(compressed.get(0).getAbsolutePath())));

            copyExifRotation(new File(selectedImages.get(0)), compressed.get(0));
//            Log.d(TAG, String.valueOf(getOrientation(compressed.get(0).getAbsolutePath())));

            if (compressedImages.size() == selectedImages.size()) {
                uploadPhotos();
            } else {
                imageCompressTask = new ImageCompressTask(context, selectedImages.get(compressedImages.size()), iImageCompressTaskListener);
                mExecutorService.execute(imageCompressTask);
            }
        }

        @Override
        public void onError(Throwable error) {
            //very unlikely, but it might happen on a device with extremely low storage.
            //log it, log.WhatTheFuck?, or show a dialog asking the user to delete some files....etc, etc
            Log.wtf("ImageCompressor", "Error occurred", error);
        }
    };

}
