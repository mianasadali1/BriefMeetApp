package com.subhadip.briefmeet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.subhadip.briefmeet.databinding.ActivityEmailVerificationBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.subhadip.briefmeet.firebase_db.DatabaseManager;
import com.subhadip.briefmeet.utils.AppConstants;
import com.subhadip.briefmeet.utils.SharedObjects;

public class EmailVerificationActivity extends AppCompatActivity implements View.OnClickListener {


    ActivityEmailVerificationBinding binding;

    SharedObjects sharedObjects;

    private FirebaseAuth firebaseAuth;

    DatabaseManager databaseManager ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();

        sharedObjects = new SharedObjects(EmailVerificationActivity.this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        databaseManager = new DatabaseManager(EmailVerificationActivity.this);

        binding.btnVerify.setOnClickListener(this);

        verifyUser();
    }

    public void verifyUser(){

        final FirebaseUser user = firebaseAuth.getCurrentUser();
        binding.txtEmail.setText(user.getEmail());

        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(EmailVerificationActivity.this,
                                    "Verification email sent to " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("sendEmailVerification "," " + task.getException());
                            AppConstants.showAlertDialog(task.getException().getMessage(), EmailVerificationActivity.this);
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnVerify:
                if (firebaseAuth != null && firebaseAuth.getCurrentUser() != null){
                    firebaseAuth.getCurrentUser().reload();

                    SharedObjects.hideKeyboard(binding.btnVerify, EmailVerificationActivity.this);
                    if (SharedObjects.isNetworkConnected(EmailVerificationActivity.this)) {
                        if (firebaseAuth.getCurrentUser() != null && firebaseAuth.getCurrentUser().isEmailVerified()){

                            Intent intent = new Intent(EmailVerificationActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            overridePendingTransition(0,0);
                            finish();

                        }else{
                            AppConstants.showAlertDialog("Please verify your email address and try again.", EmailVerificationActivity.this);
                        }
                    } else {
                        AppConstants.showAlertDialog(getString(R.string.err_internet), EmailVerificationActivity.this);
                    }
                }
                break;
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


}
