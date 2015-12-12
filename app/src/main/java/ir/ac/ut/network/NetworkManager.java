package ir.ac.ut.network;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.net.URISyntaxException;

import ir.ac.ut.berim.BerimApplication;
import ir.ac.ut.berim.R;

/**
 * Created by Masood on 12/9/2015 AD.
 */
public class NetworkManager {

    public static Socket mSocket;

    public static void connect() {
        if (!isConnected()) {
            try {
                mSocket = IO.socket("http://172.31.29.76:3000");
                mSocket.connect();
//                loginUser("0936", "masood");
                connectMessageReciver();
            } catch (URISyntaxException e) {
                Log.wtf("Socket", "connection faild.");
            }
        }
    }

    public static boolean isConnected() {
        if (mSocket == null || !mSocket.connected()) {
            return false;
        } else {
            return true;
        }
    }

    public static void sendRequest(String methodName, JSONObject params,
            final NetworkReceiver callback) {
        if (!isConnected()) {
            connect();
            callback.onErrorResponse(
                    new BerimNetworkException(0, BerimApplication.getInstance().getString(
                            R.string.connection_error)));
        }
        mSocket.emit(methodName + "Request", params)
                .on(methodName + "Response", new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        JSONObject jsonObject = (JSONObject) args[0];
                        try {
                            if (jsonObject.has("error") && jsonObject.getBoolean("error") == true) {
                                callback.onErrorResponse(new BerimNetworkException(0,
                                        jsonObject.getString("errorMessage")));
                                return;
                            }else{
                                callback.onResponse(jsonObject.getJSONObject("data"));
                            }
                        } catch (JSONException e) {
                            callback.onErrorResponse(new BerimNetworkException());
                        }
                    }
                });
    }

    public static void connectMessageReciver() {
        mSocket.on("newMessage", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                ChatNetworkListner.sendNetworkResponseBroadcast(args);
            }
        });
    }

//    public void loginUser(String phoneNumber, String password) throws JSONException {
//        JSONObject json = new JSONObject();
//        json.put("phoneNumber", phoneNumber);
//        json.put("password", password);
//        mSocket.emit("signIn", json.toString()).on("signInResponse", new Emitter.Listener() {
//            @Override
//            public void call(final Object... args) {
//                ((Activity) mContext).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Log.e("notif", args[0].toString());
//                            JSONObject data = new JSONObject(args[0].toString());
////                            String username;
////                            String message;
////                            username = "user";//data.getString("username");
////                            message = data.getString("text");
//                            addMessage("login", args[0].toString());
//                        } catch (JSONException e) {
//                            return;
//                        }
//                    }
//                });
//            }
//        });
//    }
}
