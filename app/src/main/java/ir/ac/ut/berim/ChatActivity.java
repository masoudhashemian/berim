package ir.ac.ut.berim;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Response;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
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

import ir.ac.ut.adapter.ChatAdapter;
import ir.ac.ut.models.Message;
import ir.ac.ut.models.User;
import ir.ac.ut.network.BerimNetworkException;
import ir.ac.ut.network.ChatNetworkListner;
import ir.ac.ut.network.MethodsName;
import ir.ac.ut.network.NetworkManager;
import ir.ac.ut.network.NetworkReceiver;

public class ChatActivity extends ActionBarActivity {

    private Context mContext;

    private EditText mMessageInput;

    private ProgressDialog mProgressDialog;

    private ListView mListView;

    private ArrayList<Message> mMessages;

    private ChatAdapter mAdapter;

    private ImageButton mSendOrAttachButton;

    private User mMe;

    private User mTalkee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mContext = this;

        mMe = ProfileUtils.getUser(mContext);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.please_wait));
        mProgressDialog.setMessage(getString(R.string.please_wait_more));

        mTalkee = (User) getIntent().getSerializableExtra("user");
        mMessageInput = (EditText) findViewById(R.id.chat_text);
        mListView = (ListView) findViewById(R.id.listview);
        mMessages = new ArrayList<>();
        mAdapter = new ChatAdapter(this, mMessages);
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

        setTitle(mTalkee.getValidUserName());

        mSendOrAttachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(mMessageInput.getText().toString())) {
                    Message message = new Message();
                    message.setText(mMessageInput.getText().toString());
                    message.setRoomId(mTalkee.getRoomId());
                    message.setSender(mMe);
                    message.setStatus(Message.MessageStatus.SENT);
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
        mMessages.add(message);
        mAdapter.notifyDataSetChanged();
        mMessageInput.setText("");
        mListView.smoothScrollToPosition((mAdapter.getCount() - 1));
    }

    public void addMessage(Message message) {
        //add message to list
        mMessages.add(message);
        mAdapter.notifyDataSetChanged();
        mMessageInput.setText("");
        mListView.smoothScrollToPosition((mAdapter.getCount() - 1));
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                        addMessage(message);
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
        final CharSequence[] items = {getString(R.string.photo_from_camera),
                getString(R.string.video_from_camera), getString(R.string.photo),
                getString(R.string.video), getString(R.string.file), getString(R.string.cancel)};

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(getString(R.string.select_file));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                Intent intent;
                if (items[item].equals(getString(R.string.photo))) {
                    intent = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Select Image"),
                            SELECT_GALARY_PHOTO);
                } else if (items[item].equals(getString(R.string.video))) {
                    intent = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("video/*");
                    startActivityForResult(Intent.createChooser(intent, "Select Video"),
                            SELECT_GALARY_PHOTO);
                } else if (items[item].equals(getString(R.string.file))) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    try {
                        startActivityForResult(
                                Intent.createChooser(intent, "Select a File to Upload"),
                                SELECT_GALARY_FILE);
                    } catch (android.content.ActivityNotFoundException ex) {
                        // Potentially direct the user to the Market with a Dialog
                        Toast.makeText(mContext, "Please install a File Manager.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else if (items[item].equals(getString(R.string.photo_from_camera))) {
                    Intent cameraIntent = new Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, REQUEST_CAMERA_IMAGE);
                } else if (items[item].equals(getString(R.string.video_from_camera))) {
                    Intent cameraIntent = new Intent(
                            android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
                    cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 15);
                    startActivityForResult(cameraIntent, REQUEST_CAMERA_VIDEO);
                } else if (items[item].equals(getString(R.string.cancel))) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private final int REQUEST_CAMERA_IMAGE = 1;

    private final int REQUEST_CAMERA_VIDEO = 2;

    private final int SELECT_GALARY_PHOTO = 3;

    private final int SELECT_GALARY_VIDEO = 4;

    private final int SELECT_GALARY_FILE = 5;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == REQUEST_CAMERA_IMAGE) {
                File f = new File(Environment.getExternalStorageDirectory()
                        .toString());
                for (File temp : f.listFiles()) {
                    if (temp.getName().equals("temp.jpg")) {
                        f = temp;
                        break;
                    }
                }
                try {
                    uploadFile(f.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == SELECT_GALARY_PHOTO) {
                Uri selectedImageUri = data.getData();
                uploadFile(getPath(selectedImageUri));
            } else if (requestCode == SELECT_GALARY_VIDEO) {
                Uri selectedVideoUri = data.getData();
                String selectedPath = getPath(selectedVideoUri);
                uploadFile(selectedPath);
            } else if (requestCode == SELECT_GALARY_FILE) {
                Uri selectedVideoUri = data.getData();
                String selectedPath = getPath(selectedVideoUri);
                uploadFile(selectedPath);
            }

        }
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void uploadFile(String filePath) {
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
                                    message.setText("");
                                    message.setRoomId(mTalkee.getRoomId());
                                    message.setFileAddress(jsonObject.getString("fileAddress"));
                                    sendMessage(message);
                                }
                            } catch (JSONException ex) {

                            }

                        }
                    }
                });
    }

}
