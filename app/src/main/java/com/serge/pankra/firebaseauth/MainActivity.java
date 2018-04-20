package com.serge.pankra.firebaseauth;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    EditText phone;
    Button authBtn;
    EditText confirmCode;
    Button confirmCodeBtn;
    private String verificationId;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        phone = findViewById(R.id.phone);
        authBtn = findViewById(R.id.auth);
        confirmCode = findViewById(R.id.confirmCode);
        confirmCodeBtn = findViewById(R.id.codeConfirmBtn);

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
