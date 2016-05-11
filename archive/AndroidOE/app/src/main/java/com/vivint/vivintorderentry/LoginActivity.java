package com.vivint.vivintorderentry;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {
    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //If we have a non expired token, just redirect to contact activity
        if(TokenData.getTokenStatus(getApplicationContext()) == TokenStatus.VALID_TOKEN)
        {
            Intent intent = new Intent(LoginActivity.this, ContactEntryActivity.class);
            startActivity(intent);
        }

        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }



    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin()
    {
        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            loginCall(username, password);
        }
    }



    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor)
    {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }


    public void loginCall(String username, String password)
    {
        mProgressView.setVisibility(View.VISIBLE);
        mLoginFormView.setVisibility(View.GONE);

        Ion.with(getApplicationContext())
            .load("http://10.0.2.2/EcommServices/api/token")
            .setHeader("partner_override", "clearlink-dev.vivint.com")
            .setBodyParameter("grant_type", "password")
            .setBodyParameter("username", username)
            .setBodyParameter("password", password)
            .asJsonObject()
            .setCallback(new FutureCallback<JsonObject>()
            {
                @Override
                public void onCompleted(Exception e, JsonObject result)
                {
                    try
                    {
                        //Extract token and expiration date
                        String token = result.get("access_token").getAsString(); // Get the string "result" inside the Json-object
                        String expires = result.get(".expires").getAsString();
                        String refreshToken = result.get("refresh_token").getAsString();

                        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
                        Date date = format.parse(expires);

                        //Store token info
                        TokenData.storeTokens(getApplicationContext(), token, refreshToken);
                        TokenData.storeExpires(getApplicationContext(), date);


                        //Go to next activity
                        Intent intent = new Intent(LoginActivity.this, ContactEntryActivity.class);
                        startActivity(intent);

                        finish();
                    }
                    catch (Exception ex)
                    {
                        // This method will run if something goes wrong with the json, like a typo to the json-key or a broken JSON.
                        mPasswordView.setError(getString(R.string.error_incorrect_password));
                        mPasswordView.requestFocus();

                        mProgressView.setVisibility(View.GONE);
                        mLoginFormView.setVisibility(View.VISIBLE);
                    }

                }
            });
    }
}

