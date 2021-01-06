package com.subhadip.briefmeet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.subhadip.briefmeet.utils.SharedObjects;


public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 5000;

    SharedObjects sharedObjects ;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();

        sharedObjects = new SharedObjects(SplashActivity.this);





        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intentLogin;

                if (firebaseAuth.getCurrentUser() != null) {
                    String provider = "";
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser.getProviderData().size() > 0) {
                        //Prints Out google.com for Google Sign In, prints facebook.com for Facebook
                        provider =  firebaseUser.getProviderData().get(firebaseUser.getProviderData().size() - 1).getProviderId();
                    }

                    if(provider.equals("facebook.com")) {
                        intentLogin = new Intent(SplashActivity.this, MainActivity.class);
                    } else {
                        if (checkIfEmailVerified()){
                            intentLogin = new Intent(SplashActivity.this, MainActivity.class);
                        }else{
                            intentLogin = new Intent(SplashActivity.this, IntroActivity.class);
                        }
                    }
                } else {
                    intentLogin = new Intent(SplashActivity.this, IntroActivity.class);
                }
                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentLogin);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }

    private boolean checkIfEmailVerified() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user.isEmailVerified()) {
            // user is verified
            return true;
        } else {
            // email is not verified
            // NOTE: don't forget to log out the user.
            firebaseAuth.signOut();
            return false;
        }
    }
}
