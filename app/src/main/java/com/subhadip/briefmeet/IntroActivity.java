package com.subhadip.briefmeet;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.subhadip.briefmeet.databinding.ActivityIntroBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.subhadip.briefmeet.bean.Intro;
import com.subhadip.briefmeet.bean.MeetingHistory;
import com.subhadip.briefmeet.firebase_db.DatabaseManager;
import com.subhadip.briefmeet.meeting.MeetingActivity;
import com.subhadip.briefmeet.utils.AppConstants;
import com.subhadip.briefmeet.utils.IntroPagerAdapter;
import com.subhadip.briefmeet.utils.SharedObjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class IntroActivity extends AppCompatActivity implements View.OnClickListener {

    SharedObjects sharedObjects;

    ActivityIntroBinding binding;
    IntroPagerAdapter introPagerAdapter;

    private static int currentPage = 0;
    private static int NUM_PAGES = 4;

    final long DELAY_MS = 2000;
    final long PERIOD_MS = 3500;

    ArrayList<Intro> arrSlider = new ArrayList() ;

    String[] appPermissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private static final int PERMISSION_REQUEST_CODE = 10001;
    private static final int SETTINGS_REQUEST_CODE = 10002;

    DatabaseManager databaseManager ;
    private DatabaseReference databaseReferenceMeetingHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);



        sharedObjects = new SharedObjects(IntroActivity.this);

        databaseManager = new DatabaseManager(IntroActivity.this);
//        databaseManager.setDatabaseManagerListener(this);
        databaseReferenceMeetingHistory = FirebaseDatabase.getInstance().getReference(AppConstants.Table.MEETING_HISTORY);

        arrSlider.add(new Intro(R.raw.online_meeting, getString(R.string.title_1), getString(R.string.message_1)));
        arrSlider.add(new Intro(R.raw.calendar, getString(R.string.title_2), getString(R.string.message_2)));
        arrSlider.add(new Intro(R.raw.chat, getString(R.string.title_3), getString(R.string.message_3)));
        arrSlider.add(new Intro(R.raw.history, getString(R.string.title_4), getString(R.string.message_4)));
        arrSlider.add(new Intro(R.raw.video_call, getResources().getString(R.string.app_name), getString(R.string.message_5)));


        introPagerAdapter = new IntroPagerAdapter(IntroActivity.this, arrSlider);
        binding.viewPager.setAdapter(introPagerAdapter);
        binding.circleIndicator.setViewPager(binding.viewPager);

        binding.btnLogin.setOnClickListener(this);
        binding.btnSignUp.setOnClickListener(this);
        binding.btnJoin.setOnClickListener(this);

        //enable if need auto slider
        /*final Handler handler = new Handler();
        final Runnable update = new Runnable() {
            @Override
            public void run() {
                if (currentPage == NUM_PAGES) {
                    currentPage = 0;
                }
                viewPager.setCurrentItem(currentPage++, true);
            }
        };

        Timer swipeTimer = new Timer();
        swipeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(update);
            }
        }, DELAY_MS, PERIOD_MS);*/

    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnLogin:
                if (SharedObjects.isNetworkConnected(IntroActivity.this)) {
                    startActivity(new Intent(IntroActivity.this, LoginActivity.class));
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), IntroActivity.this);
                }
                break;
            case R.id.btnSignUp:
                if (SharedObjects.isNetworkConnected(IntroActivity.this)) {
                    startActivity(new Intent(IntroActivity.this, RegisterActivity.class));
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), IntroActivity.this);
                }
                break;
            case R.id.btnJoin:
                if (SharedObjects.isNetworkConnected(IntroActivity.this)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkAppPermissions(appPermissions)) {
                            showMeetingCodeDialog();
                        } else {
                            requestAppPermissions(appPermissions);
                        }
                    } else {
                        showMeetingCodeDialog();
                    }
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), IntroActivity.this);
                }
                break;
        }
    }

    boolean isMeetingExist = false;

    private boolean checkMeetingExists(final String meeting_id) {
        isMeetingExist = false;
        Query query = databaseReferenceMeetingHistory.orderByChild("meeting_id").equalTo(meeting_id);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.e("Meeting", "exists");
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(MeetingHistory.class).getMeeting_id().equals(meeting_id)) {
                            isMeetingExist = true;
                        }
                    }
                }else{
                    isMeetingExist = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                isMeetingExist = false;
            }
        });
        return isMeetingExist;
    }

    TextInputLayout inputLayoutCode, inputLayoutName;
    TextInputEditText edtCode, edtName;

    public void showMeetingCodeDialog() {
        final Dialog dialogDate = new Dialog(IntroActivity.this);
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDate.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogDate.setContentView(R.layout.dialog_meeting_code);
        dialogDate.setCancelable(true);

        Window window = dialogDate.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        wlp.dimAmount = 0.8f;
        window.setAttributes(wlp);
        dialogDate.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        inputLayoutCode = dialogDate.findViewById(R.id.inputLayoutCode);
        inputLayoutName = dialogDate.findViewById(R.id.inputLayoutName);
        edtCode = dialogDate.findViewById(R.id.edtCode);
        edtName = dialogDate.findViewById(R.id.edtName);

        edtCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutCode.setErrorEnabled(false);
                inputLayoutCode.setError("");
                if (charSequence.length() == 11){
                    checkMeetingExists(charSequence.toString());
                }else{
                    isMeetingExist = false;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutName.setErrorEnabled(false);
                inputLayoutName.setError("");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        Button btnAdd = dialogDate.findViewById(R.id.btnAdd);
        Button btnCancel = dialogDate.findViewById(R.id.btnCancel);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(edtCode.getText().toString().trim())) {
                    inputLayoutCode.setErrorEnabled(true);
                    inputLayoutCode.setError(getString(R.string.errMeetingCode));
                    return;
                }
                if (edtCode.getText().toString().length() < 11) {
                    inputLayoutCode.setErrorEnabled(true);
                    inputLayoutCode.setError(getString(R.string.errMeetingCodeInValid));
                    return;
                }
                if (!isMeetingExist){
                    AppConstants.showAlertDialog(getResources().getString(R.string.meeting_not_exist),IntroActivity.this);
                    return;
                }
                if (TextUtils.isEmpty(edtName.getText().toString().trim())) {
                    inputLayoutName.setErrorEnabled(true);
                    inputLayoutName.setError(getString(R.string.err_name));
                    return;
                }

                AppConstants.MEETING_ID = edtCode.getText().toString().trim();
                AppConstants.NAME = edtName.getText().toString().trim();

                dialogDate.dismiss();

                startActivity(new Intent(IntroActivity.this, MeetingActivity.class));
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDate.dismiss();
            }
        });

        if (!dialogDate.isShowing()) {
            dialogDate.show();
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
                    showMeetingCodeDialog();
                } else {
                    //some permissions are denied
                    for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {
                        String permName = entry.getKey();
                        int permResult = entry.getValue();
                        //permission is denied and never asked is not checked
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(IntroActivity.this);
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
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(IntroActivity.this);
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
        Uri uri = Uri.fromParts("package", IntroActivity.this.getPackageName(), null);
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
                            showMeetingCodeDialog();
                        } else {
                            requestAppPermissions(appPermissions);
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
