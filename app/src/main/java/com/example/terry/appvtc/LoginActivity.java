package com.example.terry.appvtc;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    EditText emailCustomer, passwordCustomer;
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Top Action Bar
        ActionBar actionBar = getSupportActionBar();

        if(actionBar != null) {
            actionBar.setTitle("Se connecter");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // EditText Login & Password
        emailCustomer = findViewById(R.id.email);
        passwordCustomer = findViewById(R.id.password);

        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(emailCustomer.getText().toString().equals("test") && passwordCustomer.getText().toString().equals("1234")){
                    Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                    startActivity(intent);

                }else {
                    Toast.makeText(getApplicationContext(), "Email ou mot de passe incorrect(s)", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}
