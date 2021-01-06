package com.subhadip.briefmeet.meeting_history;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.subhadip.briefmeet.databinding.FragmentMeetingHistoryBinding;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.subhadip.briefmeet.R;
import com.subhadip.briefmeet.bean.MeetingHistory;
import com.subhadip.briefmeet.firebase_db.DatabaseManager;
import com.subhadip.briefmeet.meeting.MeetingActivity;
import com.subhadip.briefmeet.utils.AppConstants;
import com.subhadip.briefmeet.utils.SharedObjects;
import com.subhadip.briefmeet.utils.SimpleDividerItemDecoration;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




public class MeetingHistoryFragment extends Fragment implements DatabaseManager.OnDatabaseDataChanged {



    DatabaseManager databaseManager ;
    private ArrayList<MeetingHistory> arrMeetingHistory = new ArrayList<>();
    MeetingHistoryAdapter meetingHistoryAdapter;

    SharedObjects sharedObjects ;

    String[] appPermissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    FragmentMeetingHistoryBinding binding;

    private static final int PERMISSION_REQUEST_CODE = 10001;
    private static final int SETTINGS_REQUEST_CODE = 10002;
    private AdView adView;


    public MeetingHistoryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMeetingHistoryBinding.inflate(inflater, container, false);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        sharedObjects = new SharedObjects(getActivity());
        databaseManager = new DatabaseManager(getActivity());
        databaseManager.setDatabaseManagerListener(this);

        getData();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkAppPermissions(appPermissions)) {
                requestAppPermissions(appPermissions);
            }
        }

        adView = new AdView(getContext(), "195877082142400_195893002140808", AdSize.BANNER_HEIGHT_50);

        // Add the ad view to your activity layout
        binding.bannerContainer.addView(adView);

        // Request an ad
        adView.loadAd();

        binding.rvHistory.showShimmerAdapter();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    public void getData(){
        if (sharedObjects.getUserInfo() != null){
            databaseManager.getMeetingHistoryByUser(sharedObjects.getUserInfo().getId());
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setMeetingHistoryAdapter() {
        if (arrMeetingHistory.size() > 0) {

            Collections.sort(arrMeetingHistory, new Comparator<MeetingHistory>() {

                @Override
                public int compare(MeetingHistory arg0, MeetingHistory arg1) {
                    SimpleDateFormat format = new SimpleDateFormat(
                            AppConstants.DateFormats.DATETIME_FORMAT_24);
                    int compareResult = 0;
                    try {
                        Date arg0Date = format.parse(arg0.getStartTime());
                        Date arg1Date = format.parse(arg1.getStartTime());
                        compareResult = arg1Date.compareTo(arg0Date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return compareResult;
                }
            });

            binding.rvHistory.hideShimmerAdapter();
            meetingHistoryAdapter = new MeetingHistoryAdapter(arrMeetingHistory, getActivity());
            binding.rvHistory.setAdapter(meetingHistoryAdapter);
            binding.rvHistory.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));

            binding.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    binding.deleteGroup.setVisibility(View.GONE);
                    for(MeetingHistory item: arrMeetingHistory) {
                        if(item.isChecked()) {
                            FirebaseDatabase.getInstance().getReference()
                                    .child("MeetingHistory")
                                    .child(item.getId())
                                    .setValue(null);
                        }
                    }
                }
            });

            meetingHistoryAdapter.setHistorySelectedListener(new MeetingHistoryAdapter.HistorySelectedListener() {
                @Override
                public void updateSelectionCount() {
                    binding.deleteGroup.setVisibility(View.VISIBLE);

                    int selectedItems = 0;

                    for(MeetingHistory item: arrMeetingHistory) {
                        if(item.isChecked()) {
                            selectedItems++;
                            binding.selectionLbl.setText(String.format("(%d) selected", selectedItems));
                        }
                    }

                    if(selectedItems == 0)
                        binding.deleteGroup.setVisibility(View.GONE);
                }
            });

            meetingHistoryAdapter.setOnItemClickListener(new MeetingHistoryAdapter.OnItemClickListener() {
                @Override
                public void onItemClickListener(int position, MeetingHistory bean) {
                }

                @Override
                public void onDeleteClickListener(int position, MeetingHistory bean) {
                    databaseManager.deleteMeetingHistory(bean);
                }

                @Override
                public void onJoinClickListener(int position, MeetingHistory bean) {
                    AppConstants.MEETING_ID = bean.getMeeting_id();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkAppPermissions(appPermissions)) {
                            startActivity(new Intent(getActivity(), MeetingActivity.class));
                        } else {
                            requestAppPermissions(appPermissions);
                        }
                    } else {
                        startActivity(new Intent(getActivity(), MeetingActivity.class));
                    }

                }
            });

            binding.rvHistory.setVisibility(View.VISIBLE);
            binding.historyCard.setVisibility(View.VISIBLE);
            binding.errorGroup.setVisibility(View.GONE);
            //llError.setVisibility(View.GONE);
        } else {
            binding.rvHistory.setVisibility(View.GONE);
            binding.historyCard.setVisibility(View.GONE);
            binding.errorGroup.setVisibility(View.VISIBLE);
            //llError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDataChanged(String url, DataSnapshot dataSnapshot) {
        if (url.equalsIgnoreCase(AppConstants.Table.MEETING_HISTORY)){
            if (MeetingHistoryFragment.this.isVisible()){
                arrMeetingHistory = new ArrayList<>();
                arrMeetingHistory.addAll(databaseManager.getUserMeetingHistory());
                setMeetingHistoryAdapter();
            }
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        if (MeetingHistoryFragment.this.isVisible()) {
            arrMeetingHistory = new ArrayList<>();
            setMeetingHistoryAdapter();
        }
    }

    public boolean checkAppPermissions(String[] appPermissions) {
        //check which permissions are granted
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(getActivity(), perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        //Ask for non granted permissions
        if (!listPermissionsNeeded.isEmpty()) {
            return false;
        }
        // App has all permissions
        return true;
    }

    private void requestAppPermissions(String[] appPermissions) {
        ActivityCompat.requestPermissions(getActivity(), appPermissions, PERMISSION_REQUEST_CODE);
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
                    //invoke ur method
                } else {
                    //some permissions are denied
                    for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {
                        String permName = entry.getKey();
                        int permResult = entry.getValue();
                        //permission is denied and never asked is not checked
                        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permName)) {
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());
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
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());
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
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
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
