package com.example.juanf.userslocationrealtime;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import android.util.Log;

public class SocketManager {
    public static Socket socket;
    private static String token;

    public static void Connect(final Callback loginCallback){
        try {
            if (socket != null && socket.connected()) {
                return;
            }
            socket = IO.socket("http://192.168.50.100:4000");
            socket.on("logonSuccessful", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        token = (String) args[0];
                        Log.d("UsersLocationRealTime", MessageFormat.format("Logon successful Token:{0}", token));
                        loginCallback.onSuccess(null);
                    } catch (Exception e) {
                        Log.e("UsersLocationRealTime",e.getMessage());
                        loginCallback.onError((e.getMessage()));
                    }
                }
            });
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void Disconnect(){
        if (socket != null)
            socket.disconnect();
    }

    public static void SingIn(String email, String password){
        JSONObject postData = new JSONObject();
        try {
            postData.put("email", email);
            postData.put("password", password);
            socket.emit("signIn",postData );
        }catch (JSONException e){
            Log.e("UsersLocationRealTime",e.getMessage());
        }

    }

    public static void UpdateLocation(double latitude, double longitude){
        JSONObject postData = new JSONObject();
        try {
            postData.put("token", token);
            postData.put("latitude", latitude);
            postData.put("longitude", longitude);
            socket.emit("updateLocation",postData );
        }catch (JSONException e){
            Log.e("UsersLocationRealTime",e.getMessage());
        }
    }

    public static void GetAllCurrentLocations(final Callback getAllCurrentLocationsCallback){
        JSONObject postData = new JSONObject();
        try {
            postData.put("token", token);
            socket.emit("getAllCurrentLocations",postData );
        }catch (JSONException e){
            Log.e("UsersLocationRealTime",e.getMessage());
        }

        socket.on("allCurrentLocations", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONArray currentLocations = (JSONArray)args[0];
                    getAllCurrentLocationsCallback.onSuccess(currentLocations);
                } catch (Exception e) {
                    Log.e("UsersLocationRealTime",e.getMessage());
                }
            }
        });
    }

    public static void RegisterDeleteCurrentLocation(final Callback deleteCurrentLocationCallback){
        socket.on("deleteLocation", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    String id = (String)args[0];
                    deleteCurrentLocationCallback.onSuccess(id);
                } catch (Exception e) {
                    Log.e("UsersLocationRealTime",e.getMessage());
                }
            }
        });
    }

    public static void RegisterUpdateCurrentLocation(final Callback updateCurrentLocationCallback){
        socket.on("updateCurrentLocation", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject currentLocation = (JSONObject)args[0];
                    updateCurrentLocationCallback.onSuccess(currentLocation);
                } catch (Exception e) {
                    Log.e("UsersLocationRealTime",e.getMessage());
                }
            }
        });
    }
}
