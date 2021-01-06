package com.subhadip.briefmeet;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.subhadip.briefmeet.databinding.ActivityRegisterBinding;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {



    SharedObjects sharedObjects;

    private FirebaseAuth firebaseAuth;

    DatabaseManager databaseManager;
    private DatabaseReference dfUser;
    private String TAG = "Reg";
    private ProgressDialog progressDialog;


    private GoogleSignInClient googleSignInClient;
    private static final int SIGN_IN_REQUEST = 1;

    ActivityRegisterBinding binding;

    CallbackManager mCallbackManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(RegisterActivity.this);
        dfUser = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS);

        sharedObjects = new SharedObjects(RegisterActivity.this);

        setEdtListeners();

        GoogleSignInOptions gso =  new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnRegister.setOnClickListener(this);
        binding.llLogin.setOnClickListener(this);
        binding.btnGoogleSignIn.setOnClickListener(this);

        setGooglePlusButtonText(binding.btnGoogleSignIn, "Continue with Google");

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
    protected void onResume() {
        super.onResume();
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnGoogleSignIn:
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, SIGN_IN_REQUEST);
                break;
            case R.id.btnRegister:

                SharedObjects.hideKeyboard(binding.btnRegister, RegisterActivity.this);
                if (SharedObjects.isNetworkConnected(RegisterActivity.this)) {

                    if (!validateName()) {
                        return;
                    }
                    if (!validateEmail()) {
                        return;
                    }
                    if (isExist) {
                        binding.inputLayoutEmail.setErrorEnabled(true);
                        binding.inputLayoutEmail.setError(getString(R.string.email_exists));
                        return;
                    }
                    if (!validatePassword()) {
                        return;
                    }

                    binding.btnRegister.setEnabled(false);
                    checkUserLogin();

                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), RegisterActivity.this);
                }
                break;

            case R.id.llLogin:
                Intent intentLogin;
                intentLogin = new Intent(RegisterActivity.this, LoginActivity.class);
                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentLogin);
                finish();
                break;
        }
    }

    private void checkUserLogin() {
        showProgressDialog();
        //create user
        firebaseAuth.createUserWithEmailAndPassword(binding.edtEmail.getText().toString().trim(), binding.edtPassword.getText().toString().trim())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        dismissProgressDialog();
                        binding.btnRegister.setEnabled(true);

                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.e(TAG, "createUserWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                Log.e("UserBean", "Not null");

                                UserBean userBean = new UserBean();
                                userBean.setId(user.getUid());
                                userBean.setName(binding.edtName.getText().toString().trim());
                                userBean.setEmail(binding.edtEmail.getText().toString().trim());

                                addUser(userBean);

                                Intent intentLogin;
                                intentLogin = new Intent(RegisterActivity.this, EmailVerificationActivity.class);
                                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intentLogin);
                                finish();

                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            AppConstants.showAlertDialog("Registration failed, " + task.getException().getMessage(), RegisterActivity.this);
                        }
                    }
                });
    }

    private void addUser(UserBean userBean) {
        databaseManager.addUser(userBean);
    }

    public boolean validateName() {
        if (TextUtils.isEmpty(binding.edtName.getText().toString().trim())) {
            binding.inputLayoutName.setErrorEnabled(true);
            binding.inputLayoutName.setError(getString(R.string.err_name));
            return false;
        }
        return true;
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        finish();
    }

    boolean isExist = false;

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
                            intentLogin = new Intent(RegisterActivity.this, MainActivity.class);
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentLogin);
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.e("TAG", "signInWithCredential:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

    private void firebaseAuthWithGoogle(AuthCredential credential, GoogleSignInAccount account){

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("signInWith", "Credential:onComplete:" + task.isSuccessful());
                        if(task.isSuccessful()){

                            UserBean userBean = new UserBean();
                            userBean.setId(firebaseAuth.getCurrentUser().getUid());
                            userBean.setName(account.getDisplayName());
                            userBean.setEmail(account.getEmail());
                            userBean.setProfile_pic(account.getPhotoUrl().toString());
                            if (!isExist){
                                databaseManager.addUser(userBean);
                            }else{
                                databaseManager.updateUser(userBean);
                            }
                            googleSignInClient.signOut();

                            Intent intentLogin;
                            intentLogin = new Intent(RegisterActivity.this, MainActivity.class);
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentLogin);
                            finish();

                        }else{
                            AppConstants.showAlertDialog("Registration failed, " + task.getException().getMessage(), RegisterActivity.this);
                        }

                    }
                });
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
                if (charSequence.toString().length() > 0) {
                    checkEmailExists(charSequence.toString());
                }else if (charSequence.toString().length() == 0){
                    isExist = false ;
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
    }

    public void showProgressDialog() {
        try {
            progressDialog = new ProgressDialog(RegisterActivity.this);
            progressDialog.setMax(100);
            progressDialog.setMessage(getString(R.string.please_wait));
            progressDialog.setCancelable(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            if (!RegisterActivity.this.isFinishing()) {
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
}
