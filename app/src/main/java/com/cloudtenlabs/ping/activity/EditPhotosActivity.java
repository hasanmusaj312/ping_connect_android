package com.cloudtenlabs.ping.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.signin.CompletionActivity;
import com.cloudtenlabs.ping.core.ImageCompressTask;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.listeners.IImageCompressTaskListener;
import com.cloudtenlabs.ping.object.Photo;
import com.google.gson.JsonObject;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.theophrast.ui.widget.SquareImageView;

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

public class EditPhotosActivity extends AppCompatActivity {

    ArrayList<String> photos = new ArrayList<>();
    ArrayList<SquareImageView> imageViews = new ArrayList<>();
    ArrayList<ImageView> deleteButtons = new ArrayList<>();

    Context context;
    private int GALLERY = 1, CAMERA = 2;
    int selectedIndex = -1;
    Uri imageUri;
    String realPath;

    private View.OnDragListener mOnDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View v, DragEvent event) {

            SquareImageView destinationImageView = (SquareImageView) v;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    destinationImageView.setColorFilter(Color.argb(100, 255, 0, 0)); // Red
                    break;

                case DragEvent.ACTION_DRAG_ENTERED:
//                    destinationImageView.setColorFilter(Color.argb(100, 0, 255, 0)); // Green
                    break;

                case DragEvent.ACTION_DRAG_EXITED:
//                    destinationImageView.setColorFilter(Color.argb(100, 0, 0, 255)); // Blue
//                        mPositionTextView.setText("EXIT");
                    break;

                case DragEvent.ACTION_DROP:
                    ClipData.Item item = event.getClipData().getItemAt(0);
                    String dragData = item.getText().toString();
                    int originIndex = Integer.parseInt(dragData);
                    int tempIndex = -1;
                    for (SquareImageView imageView:
                         imageViews) {
                        if (destinationImageView.getId() == imageView.getId()) {
                            tempIndex = imageViews.indexOf(imageView);
                        }
                    }
                    final int destinationIndex = tempIndex;
                    if (originIndex != destinationIndex) {
                        final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Updating...", true, false);
                        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
                        RequestBody photoFirstBody = RequestBody.create(MediaType.parse("multipart/form-data"), photos.get(originIndex));
                        RequestBody photoSecondBody = RequestBody.create(MediaType.parse("multipart/form-data"), photos.get(destinationIndex));

                        String cover;
                        if (originIndex == 0) {
                            cover = photos.get(destinationIndex);
                        } else if(destinationIndex == 0) {
                            cover = photos.get(originIndex);
                        } else {
                            cover = photos.get(0);
                        }
                        final String newCover = cover;
                        RequestBody newCoverBody = RequestBody.create(MediaType.parse("multipart/form-data"), newCover);

                        Call<JsonObject> call = apiInterface.switch_photos(userIdBody, photoFirstBody, photoSecondBody, newCoverBody);
                        call.enqueue(new Callback<JsonObject>() {
                            @Override
                            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                                progressDialog.dismiss();
                                JsonObject result = response.body();
                                assert result != null;
                                int success = result.get("success").getAsInt();

                                if (success == 1) {
                                    QBUser qbUser = GlobalVariable.getInstance().loggedInUser.getQb_user();
                                    qbUser.setCustomData(newCover);
                                    QBUsers.updateUser(qbUser);

                                    GlobalVariable.getInstance().loggedInUser.setPhoto(newCover);
                                    ArrayList<Photo> newPhotos = GlobalVariable.getInstance().loggedInUser.getPhotos();

                                    int origIndex = -1, destIndex = -1;
                                    for (Photo photoObj:
                                            newPhotos) {
                                        if (photoObj.getPhoto().equals(photos.get(originIndex))) {
                                            origIndex = newPhotos.indexOf(photoObj);
                                        } else if (photoObj.getPhoto().equals(photos.get(destinationIndex))) {
                                            destIndex = newPhotos.indexOf(photoObj);
                                        }
                                    }
                                    Photo temp = newPhotos.get(origIndex);
                                    newPhotos.set(origIndex, newPhotos.get(destIndex));
                                    newPhotos.set(destIndex, temp);

                                    GlobalVariable.getInstance().loggedInUser.setPhotos(newPhotos);
                                    arrangePhotos();
                                } else {
                                    String error = result.get("error").getAsString();
                                    if (error.equals("")) {
                                        Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                                progressDialog.dismiss();

                                Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                                call.cancel();
                            }
                        });

                        String temp = photos.get(originIndex);
                        photos.set(originIndex, photos.get(destinationIndex));
                        photos.set(destinationIndex, temp);
                        arrangePhotos();
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    destinationImageView.setColorFilter(null);
                    break;

                case DragEvent.ACTION_DRAG_LOCATION:
                    break;
            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_photos);

        context = this;

        requestMultiplePermissions();

        SquareImageView coverPhotoImageView = findViewById(R.id.coverPhotoImageView);
        SquareImageView secondPhotoImageView = findViewById(R.id.secondPhotoImageView);
        SquareImageView thirdPhotoImageView = findViewById(R.id.thirdPhotoImageView);
        SquareImageView fourthPhotoImageView = findViewById(R.id.fourthPhotoImageView);

        ImageView coverDeletButton = findViewById(R.id.coverDeleteButton);
        ImageView secondDeleteButton = findViewById(R.id.secondDeleteButton);
        ImageView thirdDeleteButton = findViewById(R.id.thirdDeleteButton);
        ImageView fourthDeleteButton = findViewById(R.id.fourthDeleteButton);

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());

        imageViews.add(coverPhotoImageView);
        imageViews.add(secondPhotoImageView);
        imageViews.add(thirdPhotoImageView);
        imageViews.add(fourthPhotoImageView);

        deleteButtons.add(coverDeletButton);
        deleteButtons.add(secondDeleteButton);
        deleteButtons.add(thirdDeleteButton);
        deleteButtons.add(fourthDeleteButton);

        arrangePhotos();

        for (SquareImageView imageView:
             imageViews) {
            imageView.setOnClickListener(view -> {
                selectedIndex = imageViews.indexOf(imageView);
                if (photos.size() <= selectedIndex) {
                    showPictureDialog();
                }
            });
            imageView.setOnLongClickListener(view -> {
                ClipData dragData = ClipData.newPlainText("itemIndex", String.valueOf(imageViews.indexOf(imageView)));
                MyDragShadowBuilder myDragShadowBuilder = new MyDragShadowBuilder(view);
                view.startDrag(dragData, myDragShadowBuilder, null, 0);
                return true;
            });
        }

        for (ImageView deleteButton:
                deleteButtons) {
            deleteButton.setOnClickListener(view -> {
                if (photos.size() == 1) {
                    Toast.makeText(context, "You need at least one photo.", Toast.LENGTH_SHORT).show();
                    return;
                }
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("Delete");
                alert.setMessage("Are you sure you want to delete this photo?");
                alert.setPositiveButton("Yes", (dialog, which) -> {
                    final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Removing...", true, false);
                    APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                    RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
                    RequestBody photoBody = RequestBody.create(MediaType.parse("multipart/form-data"), photos.get(deleteButtons.indexOf(deleteButton)));
                    RequestBody newCoverBody = RequestBody.create(MediaType.parse("multipart/form-data"), deleteButtons.indexOf(deleteButton) == 0 ? photos.get(1) : photos.get(0));

                    Call<JsonObject> call = apiInterface.delete_photo(userIdBody, photoBody, newCoverBody);
                    call.enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                            progressDialog.dismiss();

                            JsonObject result = response.body();
                            assert result != null;
                            int success = result.get("success").getAsInt();

                            if (success == 1) {
                                dialog.dismiss();

                                GlobalVariable.getInstance().loggedInUser.setPhoto(deleteButtons.indexOf(deleteButton) == 0 ? photos.get(1) : photos.get(0));
                                for (Photo photoObj:
                                     GlobalVariable.getInstance().loggedInUser.getPhotos()) {
                                    if (photoObj.getPhoto().equals(photos.get(deleteButtons.indexOf(deleteButton)))) {
                                        ArrayList<Photo> newPhotos = GlobalVariable.getInstance().loggedInUser.getPhotos();
                                        newPhotos.remove(GlobalVariable.getInstance().loggedInUser.getPhotos().indexOf(photoObj));
                                        GlobalVariable.getInstance().loggedInUser.setPhotos(newPhotos);
                                        break;
                                    }
                                }
                                arrangePhotos();
                            } else {
                                String error = result.get("error").getAsString();
                                if (error.equals("")) {
                                    Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                            progressDialog.dismiss();

                            Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                            call.cancel();
                        }
                    });
                });
                alert.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                alert.show();
            });
        }
    }

    private void arrangePhotos() {
        photos.clear();
        photos.add(GlobalVariable.getInstance().loggedInUser.getPhoto());
        for (Photo photo:
                GlobalVariable.getInstance().loggedInUser.getPhotos()) {
            if (photo.getPhoto().equals(GlobalVariable.getInstance().loggedInUser.getPhoto())) {
                continue;
            }
            photos.add(photo.getPhoto());
        }
        for (ImageView deleteButton:
                deleteButtons) {
            deleteButton.setVisibility(View.GONE);
        }
        for (SquareImageView imageView:
                imageViews) {
            imageView.setImageResource(R.drawable.img_photo_placeholder);
            imageView.setOnDragListener(null);
        }
        for (int i = 0; i < photos.size(); i++) {
//            Picasso.get().load(GlobalVariable.getInstance().SERVER_IMAGE_URL + photos.get(i)).into(imageViews.get(i));
            Glide.with(context)
                    .load(GlobalVariable.getInstance().SERVER_IMAGE_URL + photos.get(i))
                    .into(imageViews.get(i));

            deleteButtons.get(i).setVisibility(View.VISIBLE);
            imageViews.get(i).setOnDragListener(mOnDragListener);
        }
    }

    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Upload a Photo");
        String[] pictureDialogItems = {
                "From Library",
                "Take Picture" };
        pictureDialog.setItems(pictureDialogItems,
                (dialog, which) -> {
                    switch (which) {
                        case 0:
                            choosePhotoFromGallary();
                            break;
                        case 1:
                            takePhotoFromCamera();
                            break;
                    }
                });
        pictureDialog.show();
    }

    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
//        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(intent, CAMERA);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_CANCELED) {
            return;
        }
        ImageCompressTask imageCompressTask;
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    imageViews.get(selectedIndex).setImageBitmap(bitmap);
                    realPath = getRealPathFromURI(contentURI);
//                    uploadPhoto(getRealPathFromURI(contentURI));

                    imageCompressTask = new ImageCompressTask(this, getRealPathFromURI(contentURI), iImageCompressTaskListener);
                    mExecutorService.execute(imageCompressTask);
                } catch (IOException e) {
                    e.printStackTrace();
//                    Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }

        } else if (requestCode == CAMERA) {
            try {
                imageViews.get(selectedIndex).setImageURI(imageUri);
                realPath = getRealPathFromURI(imageUri);
//                uploadPhoto(getRealPathFromURI(imageUri));

                imageCompressTask = new ImageCompressTask(this, getRealPathFromURI(imageUri), iImageCompressTaskListener);
                mExecutorService.execute(imageCompressTask);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ExecutorService mExecutorService = Executors.newFixedThreadPool(1);

    private IImageCompressTaskListener iImageCompressTaskListener = new IImageCompressTaskListener() {
        @Override
        public void onComplete(List<File> compressed) {
            File file = compressed.get(0);
            copyExifRotation(new File(realPath), file);

            uploadPhoto(file.getAbsolutePath());
        }

        @Override
        public void onError(Throwable error) {
            Log.wtf("ImageCompressor", "Error occurred", error);
        }
    };

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

    private void uploadPhoto(String path) {
        final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Uploading...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        MultipartBody.Part[] images_data = new MultipartBody.Part[1];
        String[] images_name = new String[1];

        for (int index = 0; index < 1; index++) {
            File imgFile = new File(path);
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), imgFile);
            String fileName = GlobalVariable.getInstance().loggedInUser.getId() + "_" + System.currentTimeMillis()/1000 + ".jpeg";
            images_data[index] = MultipartBody.Part.createFormData("photos[]", fileName, fileBody);
            images_name[index] = fileName;
        }

        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));

        Call<JsonObject> call = apiInterface.add_photo(userIdBody, images_data);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressDialog.dismiss();

                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();

                if (success == 1) {
                    ArrayList<Photo> newPhotos = GlobalVariable.getInstance().loggedInUser.getPhotos();
                    Photo newPhoto = new Photo();
                    newPhoto.setPhoto(images_name[0]);
                    newPhotos.add(newPhoto);
                    GlobalVariable.getInstance().loggedInUser.setPhotos(newPhotos);

                    arrangePhotos();
                } else {
                    String error = result.get("error").getAsString();
                    if (error.equals("")) {
                        Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                    }
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
    private void requestMultiplePermissions(){
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        report.areAllPermissionsGranted();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }

                }).
                withErrorListener(error -> {
                })
                .onSameThread()
                .check();
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Audio.Media.DATA };
        @SuppressLint("Recycle") Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        assert cursor != null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

}
