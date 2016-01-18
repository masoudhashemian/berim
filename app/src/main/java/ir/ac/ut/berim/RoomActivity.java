package ir.ac.ut.berim;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Response;
import com.nononsenseapps.filepicker.FilePickerActivity;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import ir.ac.ut.adapter.RoomAdapter;
import ir.ac.ut.database.DatabaseHelper;
import ir.ac.ut.models.Message;
import ir.ac.ut.models.Room;
import ir.ac.ut.models.User;
import ir.ac.ut.network.BerimNetworkException;
import ir.ac.ut.network.ChatNetworkListner;
import ir.ac.ut.network.MethodsName;
import ir.ac.ut.network.NetworkManager;
import ir.ac.ut.network.NetworkReceiver;

public class RoomActivity extends BerimActivity {

    private Context mContext;

    private EditText mMessageInput;

    private ProgressDialog mProgressDialog;

    private ListView mListView;

    private ArrayList<Message> mMessages;

    private RoomAdapter mAdapter;

    private ImageButton mSendOrAttachButton;

    private User mMe;

    private Room mRoom;

    private final int FILE_CODE = 1;

    private final int SELECT_USER = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        mContext = this;
        mMe = ProfileUtils.getUser(mContext);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.please_wait));
        mProgressDialog.setMessage(getString(R.string.please_wait_more));
        mRoom = (Room) getIntent().getSerializableExtra("room");
        mMessageInput = (EditText) findViewById(R.id.chat_text);
        mListView = (ListView) findViewById(R.id.listview);
        mMessages = new ArrayList<>();
        mAdapter = new RoomAdapter(this, mMessages);
        mListView.setAdapter(mAdapter);
        mListView.setDivider(null);
        mSendOrAttachButton = (ImageButton) findViewById(R.id.send_button);

        mMessageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString())) {
                    mSendOrAttachButton.setImageResource(R.drawable.ic_action_attach);
                } else {
                    mSendOrAttachButton.setImageResource(R.drawable.send_icon);
                }
            }
        });

        mSendOrAttachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(mMessageInput.getText().toString())) {
                    Message message = new Message();
                    message.setText(mMessageInput.getText().toString());
                    message.setRoomId(mRoom.getId());
                    message.setSender(mMe);
                    message.setStatus(Message.MessageStatus.SENT);
                    message.setDate(String.valueOf(System.currentTimeMillis()));
                    message.setId("not-set");
                    try {
                        sendMessage(message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    attachFile();
                }
            }
        });
        loadMessages();
    }

    public void loadMessages() {
        ArrayList<Message> messages = DatabaseHelper.getInstance(mContext).getMessage(
                DatabaseHelper.ROOM_ID + "='" + mRoom.getId() + "'");
        mMessages.addAll(messages);
        mAdapter.notifyDataSetChanged();
        mMessageInput.setText("");
    }

    public void sendMessage(Message message) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("text", message.getText());
        json.put("roomId", message.getRoomId());
        if (message.getFileAddress() != null && message.getFileAddress().length() > 0) {
            json.put("file", message.getFileAddress());
        } else {
            json.put("file", "");
        }
        NetworkManager.sendRequest(MethodsName.SEND_MESSAGE, json, new NetworkReceiver() {
            @Override
            public void onResponse(Object response) {
                //todo change message status to sent
                Log.wtf("SEND_MESSAGE", response.toString());
            }

            @Override
            public void onErrorResponse(BerimNetworkException error) {
                //todo show error for message.
                Log.wtf("SEND_MESSAGE", error.getMessage());
            }
        });
        addMessage(message);
    }

    public void addMessage(Message message) {
        //add message to list
        mMessages.add(message);
        mAdapter.notifyDataSetChanged();
        mMessageInput.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBerimHeader.setTitle(mRoom.getName());
        mBerimHeader.showRightAction(R.drawable.ic_action_new_user, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(mContext, SelectUserActivity.class), SELECT_USER);
            }
        });
        mChatNetworkListner.register();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mChatNetworkListner.unregister();
    }

    protected ChatNetworkListner mChatNetworkListner = new ChatNetworkListner() {
        @Override
        public void onMessageReceived(final JSONObject response) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.e("notif", response.getString("text"));
                        Message message = Message.createFromJson(response);
                        if (message.getRoomId().equals(mRoom.getId())) {
                            addMessage(message);
                        }
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }

        @Override
        public void onMessageErrorReceived(BerimNetworkException error) {

        }
    };


    public void attachFile() {
        Intent i = new Intent(mContext, FilePickerActivity.class);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                Environment.getExternalStorageDirectory().getPath());
        startActivityForResult(i, FILE_CODE);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();

                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            uploadFile(uri.getPath());
                        }
                    }
                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra
                            (FilePickerActivity.EXTRA_PATHS);

                    if (paths != null) {
                        for (String path : paths) {
                            Uri uri = Uri.parse(path);
                            uploadFile(uri.getPath());
                        }
                    }
                }

            } else {
                Uri uri = data.getData();
                uploadFile(uri.getPath());
            }
        } else if (requestCode == SELECT_USER && resultCode == Activity.RESULT_OK) {
            final User user = (User) data.getSerializableExtra("user");
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("userId", user.getId());
                jsonObject.put("roomId", mRoom.getId());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            NetworkManager.sendRequest(MethodsName.ADD_USER_TO_ROOM, jsonObject,
                    new NetworkReceiver<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Message message = new Message();
                                    message.setDate(String.valueOf(System.currentTimeMillis()));
                                    message.setStatus(Message.MessageStatus.SENT);
                                    message.setText(String.format(getString(R.string.i_add_user),
                                            user.getValidUserName()));
                                    message.setRoomId(mRoom.getId());
                                    message.setSender(mMe);
                                    try {
                                        sendMessage(message);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onErrorResponse(final BerimNetworkException error) {
                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, "خطا: " + error.toString(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
        }
    }

    public void uploadFile(String filePath) {
        if (filePath == null) {
            Toast.makeText(mContext,
                    getString(R.string.an_error_occurred_try_again),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog.show();
        NetworkManager
                .uploadFile(mContext, new File(filePath), new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        mProgressDialog.dismiss();
                        if (result == null) {
                            Toast.makeText(mContext,
                                    getString(R.string.an_error_occurred_try_again),
                                    Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        } else {
                            Log.wtf("FILE UPLOADED", result.getResult().toString());
                            try {
                                JSONObject jsonObject = new JSONObject(result.getResult());
                                if (jsonObject.getBoolean("error")) {
                                    Toast.makeText(mContext,
                                            getString(R.string.an_error_occurred_try_again) + ": "
                                                    + jsonObject.getString("errorMessage"),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Message message = new Message();
                                    message.setText("file");
                                    message.setRoomId(mRoom.getId());
                                    message.setFileAddress(
                                            jsonObject.getJSONObject("data").getString(
                                                    "fileAddress"));
                                    message.setSender(mMe);
                                    message.setStatus(Message.MessageStatus.SENT);
                                    message.setDate(String.valueOf(System.currentTimeMillis()));
                                    message.setId("not-set");
                                    sendMessage(message);
                                }
                            } catch (JSONException ex) {
                            }

                        }
                    }
                });
    }

}