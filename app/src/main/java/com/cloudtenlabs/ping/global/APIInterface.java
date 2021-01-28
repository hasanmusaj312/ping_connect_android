package com.cloudtenlabs.ping.global;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface APIInterface {

    @Multipart
    @POST("index.php?command=login")
    Call<JsonObject> login(@Part("email") RequestBody email,
                           @Part("password") RequestBody password,
                           @Part("os") RequestBody os,
                           @Part("version") RequestBody version,
                           @Part("is_simulator") RequestBody is_simulator);

    @Multipart
    @POST("index.php?command=login_fb")
    Call<JsonObject> login_fb(@Part("facebook_id") RequestBody facebook_id,
                              @Part("os") RequestBody os,
                              @Part("version") RequestBody version);

    @Multipart
    @POST("index.php?command=login_fb_background")
    Call<JsonObject> login_fb_background(@Part("facebook_id") RequestBody facebook_id,
                                         @Part("os") RequestBody os,
                                         @Part("version") RequestBody version);

    @Multipart
    @POST("index.php?command=send_message_push_notification")
    Call<JsonObject> send_message_push_notification(@Part("player_id") RequestBody playerId,
                              @Part("message") RequestBody message,
                              @Part("vibration") RequestBody vibration);

    @Multipart
    @POST("index.php?command=register")
    Call<JsonObject> register(@Part("email") RequestBody email,
                              @Part("phone") RequestBody phone,
                              @Part("password") RequestBody password,
                              @Part("os") RequestBody os,
                              @Part("version") RequestBody version);

    @Multipart
    @POST("index.php?command=forgot_password")
    Call<JsonObject> forgot_password(@Part("email") RequestBody email);

    @Multipart
    @POST("index.php?command=load_users")
    Call<JsonObject> load_users(@Part("user_id") RequestBody user_id,
                                @Part("latitude") RequestBody latitude,
                                @Part("longitude") RequestBody longitude);

    @Multipart
    @POST("index.php?command=load_all_users")
    Call<JsonObject> load_all_users(@Part("user_id") RequestBody user_id,
                                    @Part("latitude") RequestBody latitude,
                                    @Part("longitude") RequestBody longitude,
                                    @Part("offset") RequestBody offset,
                                    @Part("count") RequestBody count);

    @Multipart
    @POST("index.php?command=load_user")
    Call<JsonObject> load_user(@Part("user_id_from") RequestBody user_id_from,
                               @Part("user_id_to") RequestBody user_id_to);

    @Multipart
    @POST("index.php?command=load_favorite_users")
    Call<JsonObject> load_favorite_users(@Part("user_id") RequestBody user_id);

    @Multipart
    @POST("index.php?command=favorite_user")
    Call<JsonObject> favorite_user(@Part("user_id_from") RequestBody user_id_from,
                                   @Part("user_id_to") RequestBody user_id_to,
                                   @Part("is_favorite") RequestBody is_favorite);

    @Multipart
    @POST("index.php?command=block_user")
    Call<JsonObject> block_user(@Part("user_id_from") RequestBody user_id_from,
                                @Part("user_id_to") RequestBody user_id_to,
                                @Part("is_block") RequestBody is_block);

    @Multipart
    @POST("index.php?command=swipe_user")
    Call<JsonObject> swipe_user(@Part("user_id_from") RequestBody user_id_from,
                                @Part("user_id_to") RequestBody user_id_to,
                                @Part("is_like") RequestBody is_like);

    @Multipart
    @POST("index.php?command=change_password")
    Call<JsonObject> change_password(@Part("user_id") RequestBody user_id,
                                     @Part("old_password") RequestBody old_password,
                                     @Part("new_password") RequestBody new_password);

    @Multipart
    @POST("index.php?command=contact_us")
    Call<JsonObject> contact_us(@Part("email_from") RequestBody email_from,
                                @Part("email_to") RequestBody email_to,
                                @Part("subject") RequestBody subject,
                                @Part("description") RequestBody description);

    @Multipart
    @POST("index.php?command=update_user")
    Call<JsonObject> update_user(@Part("user_id") RequestBody user_id,
                                 @Part("keys") RequestBody keys,
                                 @Part("values") RequestBody values);

    @Multipart
    @POST("index.php?command=delete_account")
    Call<JsonObject> delete_account(@Part("user_id") RequestBody user_id);

    @Multipart
    @POST("index.php?command=update_photos")
    Call<JsonObject> update_photos(@Part("user_id") RequestBody user_id,
                                   @Part MultipartBody.Part[] images_data);

    @Multipart
    @POST("index.php?command=add_photo")
    Call<JsonObject> add_photo(@Part("user_id") RequestBody user_id,
                               @Part MultipartBody.Part[] image_data);

    @Multipart
    @POST("index.php?command=delete_photo")
    Call<JsonObject> delete_photo(@Part("user_id") RequestBody user_id,
                                  @Part("photo") RequestBody photo,
                                  @Part("new_cover") RequestBody new_cover);

    @Multipart
    @POST("index.php?command=switch_photos")
    Call<JsonObject> switch_photos(@Part("user_id") RequestBody user_id,
                                   @Part("photo_first") RequestBody photo_first,
                                   @Part("photo_second") RequestBody photo_second,
                                   @Part("new_cover") RequestBody new_cover);

    @Multipart
    @POST("index.php?command=send_sms")
    Call<JsonObject> send_sms(@Part("phone_number") RequestBody phone_number,
                              @Part("message") RequestBody message);

    @Multipart
    @POST("get_converstations")
    Call<JsonObject> get_conversations(@Part("user_id") RequestBody user_id);

    @Multipart
    @POST("create_conversation")
    Call<JsonObject> create_conversation(@Part("creator_id") RequestBody creator_id,
                                         @Part("title") RequestBody title,
                                         @Part("user_ids") RequestBody user_ids);

    @Multipart
    @POST("add_message")
    Call<JsonObject> add_message(@Part("conversation_id") RequestBody conversation_id,
                                 @Part("sender_id") RequestBody sender_id,
                                 @Part("message") RequestBody message,
                                 @Part("message_type") RequestBody message_type);

    @Multipart
    @POST("get_messages")
    Call<JsonObject> get_messages(@Part("conversation_id") RequestBody conversation_id,
                                  @Part("count") RequestBody count,
                                  @Part("offset") RequestBody offset,
                                  @Part("user_id") RequestBody user_id);

    @Multipart
    @POST("delete_message")
    Call<JsonObject> delete_message(@Part("message_id") RequestBody message_id,
                                  @Part("user_id") RequestBody user_id);

    @Multipart
    @POST("clear_message_history")
    Call<JsonObject> clear_message_history(@Part("conversation_ids") RequestBody conversation_ids, @Part("user_id") RequestBody user_id);

    @Multipart
    @POST("unread_messages")
    Call<JsonObject> unread_messages(@Part("user_id") RequestBody user_id);

    @Multipart
    @POST("read_messages")
    Call<JsonObject> read_messages(@Part("user_id") RequestBody message_ids,
                                   @Part("message_ids") RequestBody user_id);

}
