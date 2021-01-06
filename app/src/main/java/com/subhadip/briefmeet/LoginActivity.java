package com.subhadip.briefmeet;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.subhadip.briefmeet.databinding.ActivityLoginBinding;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.github.javiersantos.appupdater.AppUpdaterUtils;
import com.github.javiersantos.appupdater.enums.AppUpdaterError;
import com.github.javiersantos.appupdater.objects.Update;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.subhadip.briefmeet.bean.UserBean;
import com.subhadip.briefmeet.firebase_db.DatabaseManager;
import com.subhadip.briefmeet.utils.AppConstants;
import com.subhadip.briefmeet.utils.SharedObjects;

public class LoginActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener,
                    View.OnClickListener{



    SharedObjects sharedObjects;
    private DatabaseReference dfUser;
    DatabaseManager mDatabaseManager;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private GoogleSignInClient googleSignInClient;
    private static final int SIGN_IN_REQUEST = 1;
    DatabaseManager databaseManager;

    ActivityLoginBinding binding;

    CallbackManager mCallbackManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(LoginActivity.this);

        sharedObjects = new SharedObjects(LoginActivity.this);

        setGooglePlusButtonText(binding.btnGoogleSignIn, "Continue with Google");

        setEdtListeners();

        mDatabaseManager = new DatabaseManager(LoginActivity.this);
        dfUser = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS);

        GoogleSignInOptions gso =  new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnLogin.setOnClickListener(this);
        binding.llCreateAccount.setOnClickListener(this);
        binding.txtForgotPassword.setOnClickListener(this);
        binding.btnGoogleSignIn.setOnClickListener(this);

        mCallbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = binding.buttonFacebookLogin;
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("TAG", "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d("TAG", "facebook:onCancel");
                // ...
            }
            @Override
            public void onError(FacebookException error) {
                Log.d("TAG", "facebook:onError", error);
                // ...
            }
        });
    }

    protected void setGooglePlusButtonText(SignInButton signInButton, String buttonText) {
        // Find the TextView that is inside of the SignInButton and set its text
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View v = signInButton.getChildAt(i);

            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setText(buttonText);
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (SharedObjects.isNetworkConnected(LoginActivity.this)) {
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
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(LoginActivity.this);
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

    @Override
    protected void onResume() {
        super.onResume();
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnGoogleSignIn:
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, SIGN_IN_REQUEST);
                break;
            case R.id.btnLogin:
                SharedObjects.hideKeyboard(binding.btnLogin, LoginActivity.this);
                if (SharedObjects.isNetworkConnected(LoginActivity.this)) {

                    if (!validateEmail()) {
                        return;
                    }

                    if (!validatePassword()) {
                        return;
                    }

                    binding.btnLogin.setEnabled(false);

                    checkUserLogin();
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), LoginActivity.this);
                }
                break;

            case R.id.llCreateAccount:

                Intent intentLogin;
                intentLogin = new Intent(LoginActivity.this, RegisterActivity.class);
                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentLogin);
                finish();

                break;
            case R.id.txtForgotPassword:
                if (SharedObjects.isNetworkConnected(LoginActivity.this)) {
                    showForgotPasswordDialog();
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), LoginActivity.this);
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_REQUEST)
           {
               try {
                   Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                   GoogleSignInAccount account = task.getResult(ApiException.class);
                   if (account != null){
                       checkEmailExists(account.getEmail());

                       AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                       firebaseAuthWithGoogle(credential, account);
                   }
               } catch (ApiException e) {
                   Log.w("signInResult", ":failed code=" + e.getStatusCode());
               }
           }
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.e("TAG", "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            //checkEmailExists(user.getEmail());
                            UserBean userBean = new UserBean();
                            if (userBeanData != null){
                                userBean = userBeanData;
                            }
                            userBean.setId(firebaseAuth.getCurrentUser().getUid());
                            userBean.setEmail(user.getEmail());
                            userBean.setProfile_pic(user.getPhotoUrl().toString());
                            if (!isExist){
                                userBean.setName(user.getDisplayName());
                                databaseManager.addUser(userBean);
                            }else{
                                databaseManager.updateUser(userBean);
                            }

                            Intent intentLogin;
                            intentLogin = new Intent(LoginActivity.this, MainActivity.class);
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentLogin);
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.e("TAG", "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

    boolean isExist = false;
    UserBean userBeanData = null;

    private boolean checkEmailExists(final String email) {
        isExist = false;
        Query query = dfUser.orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.e("User", "exists");
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(UserBean.class).getEmail().equals(email)) {
                            isExist = true;
                            userBeanData = new UserBean();
                            userBeanData = postSnapshot.getValue(UserBean.class);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        return isExist;
    }

    private void firebaseAuthWithGoogle(AuthCredential credential, GoogleSignInAccount account){

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("signInWith", "Credential:onComplete:" + task.isSuccessful());
                        if(task.isSuccessful()){
                            UserBean userBean = new UserBean();
                            if (userBeanData != null){
                                userBean = userBeanData;
                            }
                            userBean.setId(firebaseAuth.getCurrentUser().getUid());
                            userBean.setEmail(account.getEmail());
                            userBean.setProfile_pic(account.getPhotoUrl().toString());
                            if (!isExist){
                                userBean.setName(account.getDisplayName());
                                databaseManager.addUser(userBean);
                            }else{
                                databaseManager.updateUser(userBean);
                            }
                            googleSignInClient.signOut();

                            Intent intentLogin;
                            intentLogin = new Intent(LoginActivity.this, MainActivity.class);
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentLogin);
                            finish();

                        }else{
                            Log.w("signInWith", "Credential" + task.getException().getMessage());
                            task.getException().printStackTrace();
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    private void checkUserLogin() {
        showProgressDialog();
        //authenticate user
        firebaseAuth.signInWithEmailAndPassword(binding.edtEmail.getText().toString().trim(), binding.edtPassword.getText().toString().trim())
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        binding.btnLogin.setEnabled(true);
                        dismissProgressDialog();

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.

                        if (!task.isSuccessful()) {
                            // there was an error
                            AppConstants.showAlertDialog("Authentication failed, check your email and password or sign up", LoginActivity.this);
                        } else {

                            UserBean userBean = new UserBean();
                            userBean.setId(firebaseAuth.getCurrentUser().getUid());
                            userBean.setName("");
                            userBean.setEmail(binding.edtEmail.getText().toString());
                            userBean.setProfile_pic("");
                            if (!isExist){
                                databaseManager.addUser(userBean);
                            }

                            Intent intentLogin;
                            if (firebaseAuth.getCurrentUser() != null && firebaseAuth.getCurrentUser().isEmailVerified()) {
                                intentLogin = new Intent(LoginActivity.this, MainActivity.class);
                            } else {
                                intentLogin = new Intent(LoginActivity.this, EmailVerificationActivity.class);
                            }
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentLogin);
                            finish();
                        }
                    }
                });
    }

    TextInputLayout inputLayoutFPEmail;
    TextInputEditText edtFPEmail;

    public void showForgotPasswordDialog() {
        final Dialog dialogDate = new Dialog(LoginActivity.this);
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDate.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogDate.setContentView(R.layout.dialog_forgot_password);
        dialogDate.setCancelable(true);

        Window window = dialogDate.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        wlp.dimAmount = 0.8f;
        window.setAttributes(wlp);
        dialogDate.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        inputLayoutFPEmail = dialogDate.findViewById(R.id.inputLayoutFPEmail);

        edtFPEmail = dialogDate.findViewById(R.id.edtFPEmail);

        edtFPEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutFPEmail.setErrorEnabled(false);
                inputLayoutFPEmail.setError("");
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

                if (TextUtils.isEmpty(edtFPEmail.getText().toString().trim())) {
                    inputLayoutFPEmail.setErrorEnabled(true);
                    inputLayoutFPEmail.setError(getString(R.string.errEmailRequired));
                    return;
                }

                if (!AppConstants.isValidEmail(edtFPEmail.getText().toString().trim())) {
                    inputLayoutFPEmail.setErrorEnabled(true);
                    inputLayoutFPEmail.setError(getString(R.string.errValidEmailRequired));
                    return;
                }

                firebaseAuth.sendPasswordResetEmail(edtFPEmail.getText().toString().trim())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                    MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(LoginActivity.this);
                                    materialAlertDialogBuilder.setMessage(getString(R.string.we_have_sent_instructions));
                                    materialAlertDialogBuilder.setCancelable(false).setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            dialogDate.dismiss();
                                        }
                                    });
                                    materialAlertDialogBuilder.show();
                                } else {
                                    AppConstants.showAlertDialog(task.getException().getMessage(), LoginActivity.this);
                                }

                            }
                        });
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void setEdtListeners() {

        binding.edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.inputLayoutEmail.setErrorEnabled(false);
                binding.inputLayoutEmail.setError("");
                if (!TextUtils.isEmpty(binding.edtEmail.getText().toString().trim())){
                    checkEmailExists(binding.edtEmail.getText().toString().trim()) ;
                }else{
                    isExist = false;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        binding.edtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.inputLayoutPassword.setErrorEnabled(false);
                binding.inputLayoutPassword.setError("");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    public boolean validateEmail() {
        if (TextUtils.isEmpty(binding.edtEmail.getText().toString().trim())) {
            binding.inputLayoutEmail.setErrorEnabled(true);
            binding.inputLayoutEmail.setError(getString(R.string.errEmailRequired));
            return false;
        } else if (!AppConstants.isValidEmail(binding.edtEmail.getText().toString().trim())) {
            binding.inputLayoutEmail.setErrorEnabled(true);
            binding.inputLayoutEmail.setError(getString(R.string.errValidEmailRequired));
            return false;
        }
        return true;
    }

    public boolean validatePassword() {
        if (TextUtils.isEmpty(binding.edtPassword.getText().toString().trim())) {
            binding.inputLayoutPassword.setErrorEnabled(true);
            binding.inputLayoutPassword.setError(getString(R.string.errPasswordRequired));
            return false;
        } else if (binding.edtPassword.getText().toString().trim().length() < 6) {
            binding.inputLayoutPassword.setErrorEnabled(true);
            binding.inputLayoutPassword.setError(getString(R.string.errPasswordTooShort));
            return false;
        }
        return true;
    }

    public void showProgressDialog() {
        try {
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMax(100);
            progressDialog.setMessage(getString(R.string.authenticating));
            progressDialog.setCancelable(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            if (!LoginActivity.this.isFinishing()) {
                progressDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
