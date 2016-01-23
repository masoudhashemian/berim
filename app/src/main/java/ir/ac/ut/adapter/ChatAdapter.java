package ir.ac.ut.adapter;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.nkzawa.socketio.client.On;

import java.util.ArrayList;

import ir.ac.ut.berim.ProfileUtils;
import ir.ac.ut.berim.R;
import ir.ac.ut.models.Message;
import ir.ac.ut.utils.ImageLoader;

/**
 * Created by saeed on 11/24/2015.
 */
public class ChatAdapter extends BaseAdapter {


    private static final int FROM_ME = 0;
    private static final int FROM_HER = 1;
    private Context mContext;

    private ArrayList<Message> mMessages;

    public ChatAdapter(Context context, ArrayList<Message> data) {
        mContext = context;
        mMessages = data;
    }


    @Override
    public int getCount() {
        return mMessages.size();
    }

    @Override
    public Message getItem(int position) {
        return mMessages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

//    @Override
//    public int getViewTypeCount() {
//        return super.getViewTypeCount();
//    }
//
//    @Override
//    public int getItemViewType(int position) {
//        return super.getItemViewType(position);
//    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int listViewItemType = getItemViewType(position);
        final Message chatMessage = mMessages.get(position);
        MessageViewHolder viewHolder = null;
        if (convertView == null) {
            if (listViewItemType == FROM_ME) {
                convertView = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_chat_message, parent, false);
                viewHolder = new MessageViewHolder(convertView);
                convertView.setTag(viewHolder);

            } else {//IT'S A MESSEGE FROM OTHERS
                convertView = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_chat_message_her, parent, false);
                viewHolder = new MessageViewHolder(convertView);
                convertView.setTag(viewHolder);
            }
        } else {
            viewHolder = (MessageViewHolder) convertView.getTag();
        }

        if (chatMessage.getFileAddress() != null && !chatMessage.getFileAddress().equals("")) {
            String[] fileName = chatMessage.getFileAddress().split("/");
            Log.wtf("has file", chatMessage.getFileAddress());
            viewHolder.inAppImage.setVisibility(View.VISIBLE);
            ImageLoader.getInstance().display(chatMessage.getFileAddress(), viewHolder.inAppImage, R.drawable.ic_action_attach);
            viewHolder.inAppImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent download = new Intent(Intent.ACTION_VIEW, Uri.parse(chatMessage.getFileAddress()));
                    mContext.startActivity(download);
                }
            });
        } else {
            viewHolder.name.setText(chatMessage.getText());
            viewHolder.inAppImage.setVisibility(View.GONE);
        }
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (ProfileUtils.getUser(mContext).getId()
                .equals(mMessages.get(position).getSender().getId())) {
            return FROM_ME;
        }
        return FROM_HER;
    }

    class MessageViewHolder {

        final TextView name;

        final TextView description;
        final TextView sender;
        final ImageView inAppImage;
        final ImageView icon;

        MessageViewHolder(View view) {
            name = (TextView) view.findViewById(R.id.message_name_text_view);
            description = (TextView) view.findViewById(R.id.message_description_text_view);
            sender = (TextView) view.findViewById(R.id.chat_sender);
            icon = (ImageView) view.findViewById(R.id.chat_sender_avatar);
            inAppImage = (ImageView) view.findViewById(R.id.in_chat_image);
        }
    }
}
