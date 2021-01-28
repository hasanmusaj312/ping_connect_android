package com.cloudtenlabs.ping.custom;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.global.GlobalFunction;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.User;
import com.mindorks.placeholderview.SwipeDirection;
import com.mindorks.placeholderview.annotations.Click;
import com.mindorks.placeholderview.annotations.Layout;
import com.mindorks.placeholderview.annotations.LongClick;
import com.mindorks.placeholderview.annotations.NonReusable;
import com.mindorks.placeholderview.annotations.Resolve;
import com.mindorks.placeholderview.annotations.View;
import com.mindorks.placeholderview.annotations.swipe.SwipeCancelState;
import com.mindorks.placeholderview.annotations.swipe.SwipeIn;
import com.mindorks.placeholderview.annotations.swipe.SwipeInDirectional;
import com.mindorks.placeholderview.annotations.swipe.SwipeInState;
import com.mindorks.placeholderview.annotations.swipe.SwipeOut;
import com.mindorks.placeholderview.annotations.swipe.SwipeOutDirectional;
import com.mindorks.placeholderview.annotations.swipe.SwipeOutState;
import com.mindorks.placeholderview.annotations.swipe.SwipingDirection;
import com.squareup.picasso.Picasso;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

@NonReusable
@Layout(R.layout.tinder_card_view)
public class TinderCard2 {

    private CardCallback callback;

    @View(R.id.profileImageView)
    private ImageView profileImageView;

    @View(R.id.nameAgeTxt)
    private TextView nameAgeTxt;

    @View(R.id.locationNameTxt)
    private TextView locationNameTxt;

    @View(R.id.favoriteButton)
    public ImageView favoriteButton;

    @View(R.id.containerLayout)
    public FrameLayout containerLayout;

    public int index;
    public User user;

    private TinderCard2 card;
    private Context context;

    @Click(R.id.profileImageView)
    private void onClick(){
        callback.onCardTapped(index, card);
//        Log.d("DEBUG", "profileImageView");
    }

    public TinderCard2(CardCallback callback, User user, int index, Context context) {
        this.callback = callback;
        this.user = user;
        this.index = index;
        this.context = context;
    }

    @Resolve
    private void onResolve(){
//        nameAgeTxt.setText("Name " + count++)
        card = this;
        nameAgeTxt.setText(user.getFirst_name());
        locationNameTxt.setText("AGE " + GlobalFunction.getInstance().convertBirthdayToAge(user.getBirthday()) + ", " + user.getCity());
//        Picasso.get().load(GlobalVariable.getInstance().SERVER_IMAGE_URL + user.getPhoto()).into(profileImageView);
        Glide.with(context).load(GlobalVariable.getInstance().SERVER_IMAGE_URL + user.getPhoto()).into(profileImageView);

        favoriteButton.setImageResource(user.getIs_favorited().equals("1") ? R.drawable.ic_favorite_selected : R.drawable.ic_favorite_normal);
        favoriteButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                callback.onFavoriteTapped(index, card);
            }
        });

        int widthInPixel = GlobalFunction.getInstance().dpTopixel(context, GlobalFunction.getInstance().getScreenWidthInDPs(context) - 30);
        int heightInPixel = GlobalFunction.getInstance().dpTopixel(context, 425);

        containerLayout.setLayoutParams(new FrameLayout.LayoutParams(widthInPixel, heightInPixel));
    }

    @SwipeOut
    private void onSwipedOut(){
//        callback.onSwipingEnd(index);
        callback.onSwipedLeft(index);
    }

    @SwipeCancelState
    private void onSwipeCancelState(){
        callback.onSwipingEnd(index);
    }

    @SwipeIn
    private void onSwipeIn(){
//        callback.onSwipingEnd(index);
        callback.onSwipedRight(index);
    }

//    @SwipeInDirectional
//    private void SwipeInDirectional(SwipeDirection direction) {
//        Log.d("", direction.name());
//    }
//
//    @SwipeOutDirectional
//    private void SwipeOutDirectional(SwipeDirection direction) {
//        Log.d("", direction.name());
//    }

    @SwipeInState
    private void onSwipeInState(){
        callback.onSwiping();
    }

    @SwipeOutState
    private void onSwipeOutState(){
        callback.onSwiping();
    }

    public interface CardCallback{
        void onSwiping();
        void onSwipingEnd(int index);
        void onSwipedRight(int index);
        void onSwipedLeft(int index);
        void onCardTapped(int index, TinderCard2 card);
        void onFavoriteTapped(int index, TinderCard2 card);
    }

}