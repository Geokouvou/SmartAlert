package com.protal.smartalert;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends AppCompatActivity {

    EditText nameV,emailAd,passwordV,passwordRetypeV;
    Button buttonRegister;
    TextView alreadyReg;
    FirebaseAuth fAuth;
    ProgressBar progressBar;
    FirebaseDatabase rootNode;
    DatabaseReference myRef;
    public static String userUid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameV=findViewById(R.id.name);
        emailAd=findViewById(R.id.email);
        passwordV= findViewById(R.id.password);
        passwordRetypeV= findViewById(R.id.passwordRetype);
        buttonRegister=findViewById(R.id.buttonRegister);
        alreadyReg=findViewById(R.id.alreadyReg);
        fAuth=FirebaseAuth.getInstance();
        progressBar=findViewById(R.id.progressBar);

        if(fAuth.getCurrentUser()!=null){
            startActivity(new Intent(getApplicationContext(),MainActivity.class));
            finish();
        }

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // String name =nameV.getText().toString().trim();
                String email= emailAd.getText().toString().trim();
                String password= passwordV.getText().toString().trim();
                String passwordRetype=passwordRetypeV.getText().toString().trim();
                rootNode= FirebaseDatabase.getInstance();
                myRef = rootNode.getReference("users");






                if(TextUtils.isEmpty(email)){
                    emailAd.setError("Type your email");
                    return;
                }

                if(TextUtils.isEmpty(password)){
                    passwordV.setError("You made a mistake.Try again.");
                    return;
                }
                if(TextUtils.isEmpty(passwordRetype)){
                    passwordRetypeV.setError("You must retype the password.Try Again.");
                }


                if(password.length()< 6){
                    passwordV.setError("Password must be at least 6 characters");
                }


                if(TextUtils.equals(password,passwordRetype)){
                    progressBar.setVisibility(View.VISIBLE);


                    fAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                FirebaseUser userF = fAuth.getCurrentUser();
                                String name =nameV.getText().toString().trim();
                                addUserDetails(name,userF);
                                userUid = userF.getUid();
                                String email = userF.getEmail();
                                UserClass helperClass =new UserClass(name,email,userUid);
                                myRef.child(userUid).setValue(helperClass);
                                Toast.makeText(Register.this,"user created",Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(),MainActivity.class));

                            }else{
                                Toast.makeText(Register.this,"Error!"+task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);

                            }
                        }
                    });
                }else{
                    passwordRetypeV.setError("Passwords don't match.Try Again.");

                }



            }
        });

        alreadyReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),Login.class));

            }
        });


    }
    public void showMessage(String message){
        new AlertDialog.Builder(this)
                .setTitle("Hey! :")
                .setMessage(message)
                .setCancelable(true)
                .show();
    }
    private void addUserDetails(String displayName, FirebaseUser user){
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();
        user.updateProfile(profileChangeRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                            Toast.makeText(getApplicationContext(),"Success!",Toast.LENGTH_LONG).show();
                    }
                });
    }
}
