package com.vivint.vivintorderentry;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TokenData
{
    public static TokenStatus getTokenStatus(Context context)
    {
        String token = getToken(context);
        Date expires = getExpires(context);

        boolean hasToken = token != null && !token.isEmpty();
        boolean activeToken = expires != null && new Date().before(expires);

        if(!hasToken)
        {
            return TokenStatus.NO_TOKEN;
        }
        if(!activeToken)
        {
            return TokenStatus.EXPIRED_TOKEN;
        }

        return TokenStatus.VALID_TOKEN;
    }

    public static void storeTokens(Context context, String token, String refreshToken)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("token", token);
        editor.putString("refreshToken", refreshToken);
        editor.commit();
    }

    public static String getToken(Context context)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString("token", null);
    }

    public static String getRefreshToken(Context context)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString("refreshToken", null);
    }

    public static void storeExpires(Context context, Date date)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("expires", date.getTime());
        editor.commit();
    }

    public static Date getExpires(Context context)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        Long millis = settings.getLong("expires", -1);

        if(millis < 0)
        {
            return null;
        }
        else
        {
            return new Date(millis);
        }

    }

    public static Future<JsonObject> refreshToken(final Context context)
    {
        return Ion.with(context)
                .load("http://10.0.2.2/EcommServices/api/token")
                .setHeader("partner_override", "clearlink-dev.vivint.com")
                .setBodyParameter("grant_type", "refresh_token")
                .setBodyParameter("refresh_token", getRefreshToken(context))
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

                            storeTokens(context, token, refreshToken);
                            storeExpires(context, date);
                        }
                        catch (Exception ex)
                        {

                        }

                    }
                });
    }
}

enum TokenStatus
{
    NO_TOKEN,
    EXPIRED_TOKEN,
    VALID_TOKEN
}
