package com.asmedia.checkin_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.loopj.android.http.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class LoginActivity extends AppCompatActivity {

    EditText UsernameEt, PasswordEt;

    //Session session;

    private Session session;

    private static final String TAG = "LoginActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        UsernameEt = (EditText)findViewById(R.id.et_username);
        PasswordEt = (EditText)findViewById(R.id.et_password);

        session = new Session(this);




        if(session.loggedin()){

            Log.d(TAG, "logged in");

            String username = String.valueOf(UsernameEt.getText());
            String password = String.valueOf(PasswordEt.getText());


            //Intent i = new Intent(this, AdminActivity.class);
            //startActivity(i);

            validateAccessToken(username, password);

        } else{

            Log.d(TAG, "not logged in");
        }

    }

    public void onLogin(final View view) throws JSONException{

        final String username = UsernameEt.getText().toString();
        final String password = PasswordEt.getText().toString();
        //String type = "login";

        String url = "http://laravel.ticketval.de/api/login";

        RequestParams params = new RequestParams();
        params.put("userName", username);
        params.put("userPassword", password);

        AsyncHttpClient client = new AsyncHttpClient();

        client.post(url, params, new JsonHttpResponseHandler(){
           @Override
            public void onSuccess (int statusCode, Header[] headers, JSONObject response){

               Log.d("Response: ", String.valueOf(response));

               try {
                   int responsecode = response.getInt("status");
                   Log.d("Status: ", String.valueOf(responsecode));

                   if (responsecode == 200){
                       Log.d(TAG, "success");
                       //alertDialog.setMessage("SUCCESS");
                       //alertDialog.show();

                       SharedPreferences sharedPref = getSharedPreferences("userCredentials", Context.MODE_PRIVATE);

                       SharedPreferences.Editor editor = sharedPref.edit();
                       editor.putString("username", username);
                       editor.putString("userpassword", password);
                       editor.apply();

                       session.setLoggedIn(true);
                       //getAccessToken(username,password);
                       validateAccessToken(username, password);
                       //redirect(view);



                   } else if(responsecode == 403){
                       Log.d(TAG, "error 403");
                       //alertDialog.setMessage("ERROR 403");
                       //alertDialog.show();
                       session.setLoggedIn(false);
                   }
               } catch (JSONException e) {
                   e.printStackTrace();
               }







           }

            @Override
            public void onFailure (int statusCode, Header[] headers, String responseString, Throwable throwable){
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.d("Failed: ", String.valueOf(statusCode));
                Log.d("Error: ", String.valueOf(throwable));
            }


        });
        /*

        BackgroundWorker backgroundWorker = new BackgroundWorker(this);

        backgroundWorker.execute(type, username, password);
        */



    }

    public void onLogout(){

        session = new Session(this);
        session.setLoggedIn(false);

    }

    public void redirect(View view){


        Log.d("Redirect: ", "redirecting....");

        Intent i = new Intent(view.getContext(), AdminActivity.class);
        startActivity(i);

    }

    public void getAccessToken(String username, String password){

        String url = "http://laravel.ticketval.de/oauth/token";

        RequestParams params = new RequestParams();
        params.put("grant_type", "password");
        params.put("client_id", "4");
        params.put("client_secret", "WGy6yOMh1730nI71mKR2V02FT6b8JrgS6A0GDKTm");
        params.put("username", username);
        params.put("password", password);

        AsyncHttpClient client = new AsyncHttpClient();

        client.post(url, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess (int statusCode, Header[] headers, JSONObject response){

                Log.d("Response: ", String.valueOf(response));
                try {
                    String accessToken = response.getString("access_token");

                    SharedPreferences sharedPref = getSharedPreferences("accessTokens", Context.MODE_PRIVATE);

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("accessToken", accessToken);
                    editor.apply();

                    Log.d("AccessToken: ", accessToken);

                    redirect(findViewById(android.R.id.content));


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onFailure (int statusCode, Header[] headers, String responseString, Throwable throwable){
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.d("Failed: ", String.valueOf(statusCode));
                Log.d("Error: ", String.valueOf(throwable));
            }


        });


    }

    public void validateAccessToken(final String username, final String password){


        String url = "http://laravel.ticketval.de/api/validateToken";

        String accesToken;



        SharedPreferences sharedPref = getSharedPreferences("accessTokens", Context.MODE_PRIVATE);

        accesToken = sharedPref.getString("accessToken", "");

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Authorization", "Bearer " + accesToken);

        client.get(url, null, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess (int statusCode, Header[] headers, JSONObject response){

                Log.d("Response: ", String.valueOf(response));

                if (response != null) {
                    try {
                        //JSONArray jsonArr = new JSONArray(response);
                        if (response != null) {

                            String debug = response.getString("status");
                            Log.d("Response: ", debug);

                            if (response.getInt("status") == 200){

                                redirect(findViewById(android.R.id.content));

                            }

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.e("JSON Data", "Didn't receive any data from server!");
                }

            }

            @Override
            public void onFailure (int statusCode, Header[] headers, String responseString, Throwable throwable){
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.d("Failed: ", String.valueOf(statusCode));
                getAccessToken(username, password);

            }
        });




    }

    public void checkPrefs(View view){

        SharedPreferences sharedPref = getSharedPreferences("userCredentials", Context.MODE_PRIVATE);

        Log.d("Name: ", sharedPref.getString("username", ""));
        Log.d("Password: ", sharedPref.getString("userpassword", ""));
    }
}
