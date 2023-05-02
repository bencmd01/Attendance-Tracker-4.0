package com.example.attendancetracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**     Semester: Fall 2022
 * LoginActivity contains modules to create new accounts and log in.
 *
 * The register screen is also implemented here with intents that allow
 * the user to navigate back and forth between the login screen
 * and registration screen
 *
 * LoginActivity connects to the local database with DBHandler class
 * to check authentication when logging in, add new users when registering,
 * and checking the existence of emails used in the user table.
 */

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "Login"; // log tag
    private EditText emailFld, passFld;

    //Attributes on register popup
    private EditText rFirstName;
    private EditText rLastName;
    private EditText rEmail;
    private EditText rPassword;

    //Database handler, allows access to database in this class
    private DBHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize attributes
        // <var_name> = (<Type>) findViewByID(R.id.<id_given_in_XML>
        //Declare the attributes on the screen
        emailFld = (EditText) findViewById(R.id.loginUserEdit);
        passFld = (EditText) findViewById(R.id.loginPassEdit);
        Button loginBtn = (Button) findViewById(R.id.loginLoginBtn);
        Button registerBtn = (Button) findViewById(R.id.loginRegisterBtn);
        Button passwordBtn = (Button) findViewById(R.id.loginForgotPassBtn);

        // Allow attribute to react to user press
        loginBtn.setOnClickListener(view -> loginClicked());
        registerBtn.setOnClickListener(view -> registerClicked());
        passwordBtn.setOnClickListener(view -> forgotPasswordClicked());

        db = new DBHandler(this);

    }

    private void loginClicked() {
        String email = emailFld.getText().toString().trim();
        String pass = passFld.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Fields cannot be empty!", Toast.LENGTH_LONG).show();
        } else {
            Boolean auth = db.checkEmailPassword(email, pass);
            if (auth) {
                Toast.makeText(LoginActivity.this,
                        "Sign In Successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("userEmail", email);
                Log.d(TAG, "loginClicked: " + email);
                startActivity(intent);
            } else {
                Toast.makeText(LoginActivity.this,
                        "Invalid credentials, try again.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void registerClicked() {
        // Change the view to the registration screen
        setContentView(R.layout.layout_registration);
        // Register popup
        rFirstName = (EditText) findViewById(R.id.registerFirstname);
        rLastName = (EditText) findViewById(R.id.registerLastname);
        rEmail = (EditText) findViewById(R.id.registerEmail);
        rPassword = (EditText) findViewById(R.id.registerPassword);
        Button rBackBtn = (Button) findViewById(R.id.registerBackBtn);
        rBackBtn.setOnClickListener(view -> registerBackClicked());
        Button rSubmitBtn = (Button) findViewById(R.id.registerSubmitBtn);
        rSubmitBtn.setOnClickListener(view -> registerSubmitClicked());
    }

    private void forgotPasswordClicked() {
        // awaiting implementation
    }

    private void registerBackClicked() {
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
    }

    private void registerSubmitClicked() {
        String strFName = rFirstName.getText().toString().trim();
        String strLName = rLastName.getText().toString().trim();
        String strEmail = rEmail.getText().toString().trim();
        String strPass = rPassword.getText().toString().trim();
        String strRepass = rPassword.getText().toString().trim();

        if (strFName.isEmpty() || strLName.isEmpty() || strEmail.isEmpty() ||
                strPass.isEmpty() || strRepass.isEmpty()) {
            Toast.makeText(LoginActivity.this,
                    "Fields may not be empty!", Toast.LENGTH_LONG).show();
            return;
        }
        /*
        Check if password confirmation match
        And check if email already exists
        And make sure database correctly adds user
         */
        if (strPass.equals(strRepass)
            && !db.checkEmailExist(strEmail)
            && db.addUser(strEmail, strPass, strFName, strLName)) {
                Toast.makeText(LoginActivity.this,
                        String.format("%s has been added!", strEmail), Toast.LENGTH_LONG).show();
                // This will transfer the identity of the user logging in
                // The identity is given by the email used to log in. It is intended that
                // the email provided will determine the semesters that appear on the main activity screen
                Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
                startActivity(intent);
        } else {
            Toast.makeText(LoginActivity.this,
        "Failed to add new user. Try again.", Toast.LENGTH_LONG).show();
        }
    }
}