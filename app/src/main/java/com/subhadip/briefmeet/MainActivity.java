package com.subhadip.briefmeet;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.subhadip.briefmeet.databinding.ActivityMainBinding;
import com.subhadip.briefmeet.schedule.ScheduleMeetingActivity;
import com.github.javiersantos.appupdater.AppUpdaterUtils;
import com.github.javiersantos.appupdater.enums.AppUpdaterError;
import com.github.javiersantos.appupdater.objects.Update;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.subhadip.briefmeet.firebase_db.DatabaseManager;
import com.subhadip.briefmeet.home.HomeFragment;
import com.subhadip.briefmeet.meeting_history.MeetingHistoryFragment;
import com.subhadip.briefmeet.schedule.ScheduleFragment;
import com.subhadip.briefmeet.settings.SettingsFragment;
import com.subhadip.briefmeet.utils.AppConstants;
import com.subhadip.briefmeet.utils.SharedObjects;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authListener;
    boolean doubleBackToExitPressedOnce = false;

    DatabaseReference databaseReferenceUser;
    DatabaseManager databaseManager;

    SharedObjects sharedObjects;

    ActivityMainBinding binding;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedObjects = new SharedObjects(MainActivity.this);

        //get firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(MainActivity.this);
        databaseManager.setOnUserListener(new DatabaseManager.OnUserListener() {
            @Override
            public void onUserFound() {
                sharedObjects.setPreference(AppConstants.USER_INFO, new Gson().toJson(databaseManager.getCurrentUser()));
                updateFragments();
            }

            @Override
            public void onUserNotFound() {
                removeAllPreferenceOnLogout();
            }
        });
        databaseReferenceUser = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS);

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    Log.e("user", "null");
                    // user auth state is changed - user is null
                    // launch login activity
                    onLogout();
                } else {
                    databaseManager.getUser(user.getUid());
                }
            }
        };
        firebaseAuth.addAuthStateListener(authListener);

        loadFragment(new HomeFragment());

        binding.scheduleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ScheduleMeetingActivity.class));
            }
        });

        binding.bottom.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if(f instanceof  HomeFragment || f instanceof  MeetingHistoryFragment) {
                    binding.scheduleBtn.show();
                } else {
                    binding.scheduleBtn.hide();
                }
            }
        }, true);
    }



    @Override
    protected void onStart() {
        super.onStart();

        if (SharedObjects.isNetworkConnected(MainActivity.this)) {
            AppUpdaterUtils appUpdaterUtils = new AppUpdaterUtils(this)
                    .withListener(new AppUpdaterUtils.UpdateListener() {
                        @Override
                        public void onSuccess(Update update, Boolean isUpdateAvailable) {
                            if (isUpdateAvailable) {
                                launchUpdateDialog(update.getLatestVersion());
                            }
                        }

                        @Override
                        public void onFailed(AppUpdaterError error) {

                        }
                    });
            appUpdaterUtils.start();
        }
    }

    private void launchUpdateDialog(String onlineVersion) {

        try {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(MainActivity.this);
            materialAlertDialogBuilder.setMessage("Update " + onlineVersion + " is available to download. Downloading the latest update you will get the latest features," +
                    "improvements and bug fixes of " + getString(R.string.app_name));
            materialAlertDialogBuilder.setCancelable(false).setPositiveButton(getResources().getString(R.string.update_now), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }
            });
            materialAlertDialogBuilder.show();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateFragments() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content);
        if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).setUserData();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            Bundle bundle;
            Fragment fragment = null;
            Class fragmentClass;
            switch (menuItem.getItemId()) {
                case R.id.nav_home:
                    fragmentClass = HomeFragment.class;
                    try {
                        fragment = (Fragment) fragmentClass.newInstance();
                        loadFragment(fragment, menuItem);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                case R.id.nav_settings:
                    fragmentClass = SettingsFragment.class;
                    try {
                        fragment = (Fragment) fragmentClass.newInstance();
                        loadFragment(fragment, menuItem);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                case R.id.nav_meeting_history:
                    fragmentClass = MeetingHistoryFragment.class;
                    try {
                        fragment = (Fragment) fragmentClass.newInstance();
                        loadFragment(fragment, menuItem);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;

                case R.id.nav_schedule:
                    fragmentClass = ScheduleFragment.class;
                    try {
                        fragment = (Fragment) fragmentClass.newInstance();
                        loadFragment(fragment, menuItem);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
            }
            return false;
        }
    };

    public void selectMenuItem(String menu) {
        if (menu.equals(getString(R.string.menu_home))) {
            binding.bottom.getMenu().findItem(R.id.nav_home).setChecked(true);
        } else if (menu.equals(getString(R.string.meeting_history))) {
            binding.bottom.getMenu().findItem(R.id.nav_meeting_history).setChecked(true);
        } else if (menu.equals(getString(R.string.schedule))) {
            binding.bottom.getMenu().findItem(R.id.nav_schedule).setChecked(true);
        } else if (menu.equals(getString(R.string.menu_settings))) {
            binding.bottom.getMenu().findItem(R.id.nav_settings).setChecked(true);
        } else {
            binding.bottom.getMenu().findItem(R.id.nav_home).setChecked(true);
            binding.bottom.getMenu().findItem(R.id.nav_meeting_history).setChecked(false);
            binding.bottom.getMenu().findItem(R.id.nav_schedule).setChecked(false);
            binding.bottom.getMenu().findItem(R.id.nav_settings).setChecked(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.doubleBackToExitPressedOnce = false;
    }

    @Override
    public void onBackPressed() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.content);
                if (f instanceof HomeFragment) {
                    selectMenuItem(getString(R.string.menu_home));
                } else if (f instanceof SettingsFragment) {
                    selectMenuItem(getString(R.string.menu_settings));
                } else if (f instanceof ScheduleFragment) {
                    selectMenuItem(getString(R.string.schedule));
                } else if (f instanceof MeetingHistoryFragment) {
                    selectMenuItem(getString(R.string.meeting_history));
                }
            }
        }, 200);

        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {

            if (doubleBackToExitPressedOnce) {
                finish();
                System.exit(0);
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, getString(R.string.exit), Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);

        } else {
            super.onBackPressed();
        }
    }

    public void loadFragment(Fragment fragment) {

        String backStateName = fragment.getClass().getName();
        String fragmentTag = backStateName;

        FragmentManager manager = getSupportFragmentManager();
        boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

        if (!fragmentPopped && manager.findFragmentByTag(fragmentTag) == null) { //fragment not in back stack, create it.
            FragmentTransaction ft = manager.beginTransaction();
            ft.replace(R.id.content, fragment, fragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(backStateName);
            ft.commit();
        }
    }

    public void loadFragment(Fragment fragment, MenuItem menuItem) {
        String backStateName = fragment.getClass().getName();
        String fragmentTag = backStateName;

        FragmentManager manager = getSupportFragmentManager();
        boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

        if (!fragmentPopped && manager.findFragmentByTag(fragmentTag) == null) { //fragment not in back stack, create it.
            FragmentTransaction ft = manager.beginTransaction();
            ft.replace(R.id.content, fragment, fragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(backStateName);
            ft.commit();
            menuItem.setChecked(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onLogout() {
        sharedObjects.removeSinglePreference(AppConstants.USER_INFO);

        Intent intent = new Intent(MainActivity.this, IntroActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    public void removeAllPreferenceOnLogout() {
        try {
            firebaseAuth.signOut();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
