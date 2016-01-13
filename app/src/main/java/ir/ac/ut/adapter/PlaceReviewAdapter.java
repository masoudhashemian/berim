package ir.ac.ut.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ir.ac.ut.berim.R;
import ir.ac.ut.models.Place;
import ir.ac.ut.models.Review;

/**
 * Created by masood on 10/1/15.
 */
public class PlaceReviewAdapter extends BaseAdapter {
    public static final int PLACE_DESCRIPTION=0;
    public static final int PLACE_REVIEW=1;
    private Context mContext;

    private Review[] reviews;

    public PlaceReviewAdapter(Context context, ArrayList<Review> data) {
        mContext = context;
        reviews = new Review[data.size()];
        reviews = data.toArray(reviews);
    }


    @Override
    public int getCount() {
        return reviews.length;
    }

    @Override
    public Object getItem(int position) {
        return reviews[position];
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
        PlaceReviewViewHolder reviewViewHolder;
        PlaceDescriptionViewHolder descriptionViewHolder;
        if (convertView == null) {
            if(listViewItemType==PLACE_DESCRIPTION){
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_place_description, parent, false);
                reviewViewHolder = new PlaceReviewViewHolder(convertView);
                convertView.setTag(reviewViewHolder);
                reviewViewHolder.name.setText(reviews[position].getUser().getNickName());
                reviewViewHolder.description.setText(reviews[position].getDescription());
            }
            else
            {//review
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_place_review, parent, false);
                reviewViewHolder = new PlaceReviewViewHolder(convertView);
                convertView.setTag(reviewViewHolder);
                reviewViewHolder.name.setText(reviews[position].getUser().getNickName());
                reviewViewHolder.description.setText(reviews[position].getDescription());
                reviewViewHolder.icon.setImageResource(R.drawable.ic_launcher);
            }
        } else {
            reviewViewHolder = (PlaceReviewViewHolder) convertView.getTag();
        }
        return convertView;
    }
    @Override
    public int getViewTypeCount(){
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position==0)
            return PLACE_DESCRIPTION;
        return PLACE_REVIEW;
    }

    class PlaceReviewViewHolder {

        final TextView name;
        final TextView description;
        final ImageView icon;


        PlaceReviewViewHolder(View view) {
            name = (TextView) view.findViewById(R.id.reviewer_name_text_view);
            description = (TextView) view.findViewById(R.id.review_description_text_view);
            icon = (ImageView) view.findViewById(R.id.review_icon_image);
        }
    }
    class PlaceDescriptionViewHolder {

        final TextView name;
        final TextView description;


        PlaceDescriptionViewHolder(View view) {
            name = (TextView) view.findViewById(R.id.reviewer_name_text_view);
            description = (TextView) view.findViewById(R.id.review_description_text_view);
        }
    }
}



