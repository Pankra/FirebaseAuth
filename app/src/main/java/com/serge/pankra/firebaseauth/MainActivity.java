package com.serge.pankra.firebaseauth;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String BACKGROUND_COLOR = "background_color";
    RelativeLayout root;
    EditText phone;
    Button authBtn;
    EditText confirmCode;
    Button confirmCodeBtn;
    private String verificationId;
    private FirebaseAuth auth;
    private FirebaseRemoteConfig remoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.root);
        phone = findViewById(R.id.phone);
        authBtn = findViewById(R.id.auth);
        confirmCode = findViewById(R.id.confirmCode);
        confirmCodeBtn = findViewById(R.id.codeConfirmBtn);

        remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setDefaults(R.xml.remote_config_defaults);
        // Create a Remote Config Setting to enable developer mode, which you can use to increase
        // the number of fetches available per hour during development. See Best Practices in the
        // README for more information.
        // [START enable_dev_mode]
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        remoteConfig.setConfigSettings(configSettings);
        // [END enable_dev_mode]

        fetchRemoteConfig();

        auth = FirebaseAuth.getInstance();
        auth.useAppLanguage();

        final PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationStateChangedCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                System.out.println("!!! phoneAuthCredential " + phoneAuthCredential.getProvider());
                System.out.println("!!! " + phoneAuthCredential.getSignInMethod());
                System.out.println("!!! " + phoneAuthCredential.getSmsCode());
                singInUser(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                System.out.println("!!! onVerificationFailed " + e.getMessage());
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                System.out.println("!!! verificationId: " + verificationId);
                System.out.println("!!! " + forceResendingToken.toString());
                super.onCodeSent(verificationId, forceResendingToken);
                MainActivity.this.verificationId = verificationId;
            }
        };

        authBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneAuthProvider authProvider = PhoneAuthProvider.getInstance();
                authProvider.verifyPhoneNumber(
                        phone.getText().toString(),
                        10,
                        TimeUnit.SECONDS,
                        MainActivity.this,
                        verificationStateChangedCallbacks
                );
            }
        });

        confirmCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, confirmCode.getText().toString());
                singInUser(credential);
            }
        });
    }

    private void fetchRemoteConfig() {
        String bgColor = remoteConfig.getString(BACKGROUND_COLOR);
        root.setBackgroundColor(Color.parseColor(bgColor));

        long cacheExpiration = 3600; // 1 hour in seconds.
        // If your app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (remoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        // [START fetch_config_with_callback]
        // cacheExpirationSeconds is set to cacheExpiration here, indicating the next fetch request
        // will use fetch data from the Remote Config service, rather than cached parameter values,
        // if cached parameter values are more than cacheExpiration seconds old.
        // See Best Practices in the README for more information.
        remoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Fetch Succeeded",
                                    Toast.LENGTH_SHORT).show();

                            // After config data is successfully fetched, it must be activated before newly fetched
                            // values are returned.
                            remoteConfig.activateFetched();
                        } else {
                            Toast.makeText(MainActivity.this, "Fetch Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                        root.setBackgroundColor(Color.parseColor(remoteConfig.getString(BACKGROUND_COLOR)));
                    }
                });
        // [END fetch_config_with_callback]
    }

    private void singInUser(PhoneAuthCredential phoneAuthCredential) {
        auth.signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            System.out.println("!!! signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            System.out.println("!!! user phone : " + user.getPhoneNumber());
                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            System.out.println("!!! signInWithCredential:failure " + task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                System.out.println("!!! The verification code entered was invalid");
                            }
                        }
                    }
                });
    }
}
