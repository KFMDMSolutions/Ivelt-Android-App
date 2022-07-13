package com.android.cts.kfmdmsolutions.ivelt;



import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;


public class SplashActivity extends AppCompatActivity {
    private static int SPLASH_TIME_OUT = 7000;
    private static CountDownTimer testTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showDialog();
    }
    public void showDialog() {

        String password = PreferenceManager.getDefaultSharedPreferences(this).getString("password", "");
        if (!password.equals("")) {

            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            final EditText input = new EditText(this);
            input.setSingleLine();
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            alert.setView(input);

            alert.setCancelable(false)
                    .setTitle("Login")
                    .setMessage("Enter Your Password");

            alert.setPositiveButton("Login", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString();
                    if (!value.equals(password)) {

                        AlertDialog.Builder alert2 = new AlertDialog.Builder(SplashActivity.this);
                        alert2.setCancelable(false)
                                .setTitle("Login")
                                .setMessage("The password you have entered is incorrect.\n Please try again!");

                        alert2.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                showDialog();
                            }
                        });
                        alert2.show();
                    }
                    else {
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });

            alert.show();

        }
        else {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        }
    }
}