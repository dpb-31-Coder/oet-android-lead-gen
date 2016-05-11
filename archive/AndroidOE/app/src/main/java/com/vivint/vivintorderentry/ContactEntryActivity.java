package com.vivint.vivintorderentry;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.util.concurrent.Future;

public class ContactEntryActivity extends AppCompatActivity {
    //UI references
    private EditText mFirstName;
    private EditText mLastName;
    private EditText mPhoneNumber;
    private View mProgressView;
    private View mContactView;
    private EditText mDateView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_entry);

        Button mSignInButton = (Button) findViewById(R.id.submit_lead);
        mSignInButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                attemptSubmit();
            }
        });

        mFirstName = (EditText)findViewById(R.id.first_name);
        mLastName = (EditText)findViewById(R.id.last_name);
        mPhoneNumber = (EditText)findViewById(R.id.phone_number);
        mProgressView = findViewById(R.id.submit_progress);
        mContactView = findViewById(R.id.contact_view);
        mDateView = (EditText)findViewById(R.id.date);
    }


    private void attemptSubmit()
    {
        String firstName = mFirstName.getText().toString();
        String lastName = mLastName.getText().toString();
        String phoneNumber = mPhoneNumber.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(phoneNumber))
        {
            mPhoneNumber.setError(getString(R.string.error_field_required));
            focusView = mPhoneNumber;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(lastName))
        {
            mLastName.setError(getString(R.string.error_field_required));
            focusView = mLastName;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(firstName))
        {
            mFirstName.setError(getString(R.string.error_field_required));
            focusView = mFirstName;
            cancel = true;
        }

        if (cancel)
        {
            // There was an error; don't attempt submission and focus the first
            // form field with an error.
            focusView.requestFocus();
        }
        else
        {
            submitLeadCall(firstName, lastName, phoneNumber);
        }
    }


    private void submitLeadCall(String firstName, String lastName, String phoneNumber)
    {
        mProgressView.setVisibility(View.VISIBLE);
        mContactView.setVisibility(View.GONE);

        //Check if we need to refresh our token
        if(TokenData.getTokenStatus(getApplicationContext()) == TokenStatus.EXPIRED_TOKEN)
        {
            Future<JsonObject> future = TokenData.refreshToken(getApplicationContext());
            try
            {
                future.get();
            }
            catch (Exception e)
            {
                //We are unauthorized, load login activity
                Intent intent = new Intent(ContactEntryActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        }

        //Service Call
        String token = TokenData.getToken(getApplicationContext());
        Log.i(token, "Token");
        Ion.with(getApplicationContext())
            .load("http://10.0.2.2/EcommServices/Leads")
            .setHeader("authorization", "Bearer " + token)
            .setHeader("partner_override", "clearlink-dev.vivint.com")
            .setBodyParameter("firstName", firstName)
            .setBodyParameter("lastName", lastName)
            .setBodyParameter("phone", phoneNumber)
            .asJsonObject()
            .withResponse()
            .setCallback(new FutureCallback<Response<JsonObject>>()
            {
                @Override
                public void onCompleted(Exception e, Response<JsonObject> result)
                {
                    if (result.getHeaders().code() == 401)
                    {
                        //We are unauthorized, load login activity
                        Intent intent = new Intent(ContactEntryActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Lead " + result.getResult().get("id") + " Submitted Successfully", Toast.LENGTH_LONG).show();
                        mFirstName.setText("");
                        mLastName.setText("");
                        mPhoneNumber.setText("");

                        Log.i(result.getResult().get("id").getAsString(), "LeadId");
                    }

                    mProgressView.setVisibility(View.GONE);
                    mContactView.setVisibility(View.VISIBLE);
                }
            });
    }
}