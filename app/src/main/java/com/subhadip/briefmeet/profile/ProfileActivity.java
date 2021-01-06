package com.subhadip.briefmeet.profile;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.subhadip.briefmeet.databinding.ActivityProfileBinding;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.subhadip.briefmeet.R;
import com.subhadip.briefmeet.bean.UserBean;
import com.subhadip.briefmeet.firebase_db.DatabaseManager;
import com.subhadip.briefmeet.utils.AppConstants;
import com.subhadip.briefmeet.utils.SharedObjects;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class ProfileActivity extends AppCompatActivity
        implements DatabaseManager.OnUserAddedListener, View.OnClickListener {


    ActivityProfileBinding binding;

    SharedObjects sharedObjects ;

    private DatabaseReference dfUsers;

    DatabaseManager mDatabaseManager;
    String[] appPermissions = {Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final int PERMISSION_REQUEST_CODE = 10001;
    private static final int SETTINGS_REQUEST_CODE = 10002;
    ProgressDialog progressDialog;
    private StorageReference storageReference;

    private FirebaseAuth firebaseAuth;
    
    UserBean userBean ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());



        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });
        }

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();

        sharedObjects = new SharedObjects(ProfileActivity.this);
        userBean = sharedObjects.getUserInfo();
        dfUsers = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS);
        mDatabaseManager = new DatabaseManager(ProfileActivity.this);
        mDatabaseManager.setOnUserAddedListener(this);
        storageReference = FirebaseStorage.getInstance().getReference();

        progressDialog = new ProgressDialog(ProfileActivity.this);
        setEdtListeners();
        binding.rlPickImage.setOnClickListener(this);
        binding.imgBack.setOnClickListener(this);
        binding.txtSave.setOnClickListener(this);
        setData();
    }

    private void setData() {
        if (userBean != null){

            if (!TextUtils.isEmpty(userBean.getProfile_pic())){
                Log.e("Pic", userBean.getProfile_pic());
                 Picasso.get().load(userBean.getProfile_pic()).error(R.drawable.avatar).into(binding.imgUser);
            }

            if (!TextUtils.isEmpty(userBean.getName())){
                binding.edtName.setText(userBean.getName());
                binding.edtName.setSelection(userBean.getName().length());
            }

            binding.inputLayoutEmail.setEnabled(false);

            if (!TextUtils.isEmpty(userBean.getEmail())){
                binding.txtEmail.setText(userBean.getEmail());
            }else{
                binding.txtEmail.setText("");
            }

        }
    }

    private void updateUser() {
        userBean.setName(binding.edtName.getText().toString());
        mDatabaseManager.updateUser(userBean);
    }

    private void setEdtListeners() {

        binding.edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.inputLayoutName.setErrorEnabled(false);
                binding.inputLayoutName.setError("");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        binding.edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.inputLayoutEmail.setErrorEnabled(false);
                binding.inputLayoutEmail.setError("");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.imgBack:
                onBackPressed();
                break;
            case R.id.rlPickImage:
                if (SharedObjects.isNetworkConnected(ProfileActivity.this)){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkAppPermissions(appPermissions)) {
                            showImagePickerDialog();
                        } else {
                            requestAppPermissions(appPermissions);
                        }
                    } else {
                        showImagePickerDialog();
                    }
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), ProfileActivity.this);
                }

                break;
            case R.id.txtSave:
                SharedObjects.hideKeyboard(binding.txtSave,ProfileActivity.this);
                if (TextUtils.isEmpty(binding.edtName.getText().toString().trim())){
                    binding.inputLayoutName.setErrorEnabled(true);
                    binding.inputLayoutName.setError(getString(R.string.err_name));
                }else{
                    updateUser();
                }
                break;
        }
    }

    public boolean checkAppPermissions(String[] appPermissions) {
        //check which permissions are granted
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        //Ask for non granted permissions
        if (!listPermissionsNeeded.isEmpty()) {
//            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PERMISSION_REQUEST_CODE);
            return false;
        }
        // App has all permissions
        return true;
    }

    private void requestAppPermissions(String[] appPermissions) {
        ActivityCompat.requestPermissions(this, appPermissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                HashMap<String, Integer> permissionResults = new HashMap<>();
                int deniedCount = 0;

                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        permissionResults.put(permissions[i], grantResults[i]);
                        deniedCount++;
                    }
                }
                if (deniedCount == 0) {
                    Log.e("Permissions", "All permissions are granted!");
                    showImagePickerDialog();
                } else {
                    //some permissions are denied
                    for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {
                        String permName = entry.getKey();
                        int permResult = entry.getValue();
                        //permission is denied and never asked is not checked
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(ProfileActivity.this);
                            materialAlertDialogBuilder.setMessage(getString(R.string.permission_msg));
                            materialAlertDialogBuilder.setCancelable(false)
                                    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .setPositiveButton(getString(R.string.yes_grant_permission), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                            if (!checkAppPermissions(appPermissions)) {
                                                requestAppPermissions(appPermissions);
                                            }
                                        }
                                    });
                            materialAlertDialogBuilder.show();

                            break;
                        } else {//permission is denied and never asked is checked
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(ProfileActivity.this);
                            materialAlertDialogBuilder.setMessage(getString(R.string.permission_msg_never_checked));
                            materialAlertDialogBuilder.setCancelable(false)
                                    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .setPositiveButton(getString(R.string.go_to_settings), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                            openSettings();
                                        }
                                    });
                            materialAlertDialogBuilder.show();

                            break;
                        }

                    }
                }

        }
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", ProfileActivity.this.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, SETTINGS_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case SETTINGS_REQUEST_CODE:
                Log.e("Settings", "onActivityResult!");
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        if (checkAppPermissions(appPermissions)) {
                            showImagePickerDialog();
                        } else {
                            requestAppPermissions(appPermissions);
                        }
                    }
                }
                break;
            case REQUEST_CODE_TAKE_PICTURE:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        showCropImageDialog(imageUri);
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        Log.e("TAKE_PICTURE", "RESULT_CANCELED");
                        break;
                    }
                    default: {
                        break;
                    }
                }
                break;
            case SELECT_FILE_GALLERY:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        if (data != null) {
                            Uri uri = data.getData();
                            showCropImageDialog(uri);
                        }
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        Log.e("FILE_GALLERY", "RESULT_CANCELED");
                        break;
                    }
                    default: {
                        break;
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void showCropImageDialog(Uri uri) {
        final Dialog dialogDate = new Dialog(ProfileActivity.this);
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDate.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogDate.setContentView(R.layout.dialog_crop_image);
        dialogDate.setCancelable(true);

        Window window = dialogDate.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        wlp.dimAmount = 0.8f;
        window.setAttributes(wlp);
        dialogDate.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Button btnCrop = dialogDate.findViewById(R.id.btnCrop);
        CropImageView cropImageView = dialogDate.findViewById(R.id.cropImageView);
        cropImageView.setImageUriAsync(uri);
        cropImageView.setGuidelines(CropImageView.Guidelines.ON);
        cropImageView.setAspectRatio(1,1);

        btnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogDate.dismiss();

                Bitmap bitmapNew = null;
                Bitmap croppedBitmap = cropImageView.getCroppedImage();

                final int maxSize = 600;
                int outWidth;
                int outHeight;
                int inWidth = croppedBitmap.getWidth();
                int inHeight = croppedBitmap.getHeight();
                if (inWidth > inHeight) {
                    outWidth = maxSize;
                    outHeight = (inHeight * maxSize) / inWidth;
                } else {
                    outHeight = maxSize;
                    outWidth = (inWidth * maxSize) / inHeight;
                }

                bitmapNew = Bitmap.createScaledBitmap(croppedBitmap, outWidth, outHeight, true);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                File fileImageSend = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                try {
                    FileOutputStream fo = new FileOutputStream(fileImageSend);
                    bitmapNew.compress(Bitmap.CompressFormat.JPEG, 100, fo);
                    fo.flush();
                    fo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Uri uri = Uri.fromFile(fileImageSend);
                UploadImageFileToFirebaseStorage(uri);
                Log.e("Uri", uri.getPath());

            }
        });

        dialogDate.show();
    }

    public void UploadImageFileToFirebaseStorage(Uri uri) {

        try {
            if (uri != null) {
                progressDialog.setTitle(getString(R.string.uploading_image));
                progressDialog.show();

                // Creating second StorageReference.
                //        StorageReference storageReference2nd = storageReference.child(SharedObjects.Storage_Path + System.currentTimeMillis() + "." + GetFileExtension(imageUri));
                StorageReference storageReference2nd = storageReference.child(AppConstants.Storage_Path + System.currentTimeMillis() + ".jpg");
                UploadTask uploadTask = storageReference2nd.putFile(uri);

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            // Hiding the progressDialog.
                            progressDialog.dismiss();
                            // Showing exception error message.
                            AppConstants.showSnackBar(task.getException().getMessage(),binding.edtName);

                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return storageReference2nd.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            Picasso.get().load(new File(uri.getPath())).into(binding.imgUser);

                            Log.e("taskSnapshot", downloadUri.toString());
//                                userBean.setProfile_pic(taskSnapshot.getDownloadUrl().toString());
                                userBean.setProfile_pic(downloadUri.toString());
                                sharedObjects.setPreference(AppConstants.USER_INFO,new Gson().toJson(userBean));
                              mDatabaseManager.updateUser(userBean);
                        } else {
                            // Handle failures
                            // ...
                            // Hiding the progressDialog.
                            // Showing exception error message.
//                            AppConstants.showSnackBar(exception.getMessage(),edtName);
                        }
                    }
                });

                /*// Adding addOnSuccessListener to second StorageReference.
                storageReference2nd.putFile(uri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                Picasso.get().load(new File(uri.getPath())).into(imgUser);

                                // Hiding the progressDialog after done uploading.
                                progressDialog.dismiss();

                                *//*Task<Uri> downloadUri = taskSnapshot.getStorage().getDownloadUrl();

                                if(downloadUri.isSuccessful()) {
                                }else{
                                    Log.e("taskSnapshot", " not success");
                                }*//*

                                String generatedFilePath = taskSnapshot.getMetadata().getPath();
                                Log.e("taskSnapshot", generatedFilePath);
//                                userBean.setProfile_pic(taskSnapshot.getDownloadUrl().toString());
                                mDatabaseManager.updateUser(userBean);

                            }
                        })
                        // If something goes wrong.
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Hiding the progressDialog.
                                progressDialog.dismiss();
                                // Showing exception error message.
                                AppConstants.showSnackBar(exception.getMessage(),edtName);
                            }
                        })
                        // On progress change upload time.
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                progressDialog.setTitle(getString(R.string.uploading_image));
                            }
                        });*/

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showImagePickerDialog() {
        final Dialog dialog = new Dialog(ProfileActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.dialog_image_picker);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        wlp.dimAmount = 0.8f;
        window.setAttributes(wlp);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        final LinearLayout llGallery = (LinearLayout) dialog.findViewById(R.id.llGallery);
        final LinearLayout llCamera = (LinearLayout) dialog.findViewById(R.id.llCamera);

        ImageView imgClose = (ImageView) dialog.findViewById(R.id.imgClose);

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), AppConstants.IMAGE_DIRECTORY_NAME);
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
            Log.e("Dir", "not exist");
        } else {
            Log.e("Dir", "exist");
        }

        llGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                galleryIntent();
            }
        });

        llCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                takePicture();
            }
        });

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    Uri imageUri;
    public static final int REQUEST_CODE_TAKE_PICTURE = 2222, SELECT_FILE_GALLERY = 1111;

    private void galleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_FILE_GALLERY);
    }

    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
    }

    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), AppConstants.IMAGE_DIRECTORY_NAME);

        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
            Log.e("Dir", "not exist");
        } else {
            Log.e("Dir", "exist");
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }
        return mediaFile;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onSuccess() {
        AppConstants.showSnackBar("Profile updated successfully", binding.edtName);
    }

    @Override
    public void onFail() {
        AppConstants.showSnackBar("Couldn't updated profile", binding.edtName);
    }
}
