package com.cloudtenlabs.ping.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.adapter.AllUsersListAdapter;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.User;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllUsersFragment extends Fragment {

    private RecyclerView recyclerView;

    private ArrayList<User> users = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    public int loadCount = 100;

    public AllUsersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_all_users, container, false);

        ImageView menuButton = view.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view1 -> ((DrawerActivity)getActivity()).toggleDrawer());

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> loadUsers(0, loadCount, true));

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager recyclerViewLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(recyclerViewLayoutManager);

        return view;
    }

    public void loadUsers(int offset, int count, boolean refresh) {
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);
        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody latitudeBody = RequestBody.create(MediaType.parse("multipart/form-data"), GlobalVariable.getInstance().currentLocation != null ? String.valueOf(GlobalVariable.getInstance().currentLocation.getLatitude()) : "0");
        RequestBody longitudeBody = RequestBody.create(MediaType.parse("multipart/form-data"), GlobalVariable.getInstance().currentLocation != null ? String.valueOf(GlobalVariable.getInstance().currentLocation.getLongitude()) : "0");
        RequestBody offsetBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(offset));
        RequestBody countBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(count));

        Call<JsonObject> call = apiInterface.load_all_users(userIdBody, latitudeBody, longitudeBody, offsetBody, countBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();
                if (success == 1) {
                    if (refresh) {
                        users.clear();
                    }
                    for (JsonElement jsonObject :
                            result.getAsJsonArray("users")) {
                        Gson gson = new Gson();
                        users.add(gson.fromJson(jsonObject.getAsJsonObject(), User.class));
                    }
                    swipeRefreshLayout.setRefreshing(false);
                    refreshRecyclerView(result.getAsJsonArray("users").size() >= loadCount);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                call.cancel();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers(0, loadCount, true);
    }

    private void refreshRecyclerView(boolean showLoadMore) {
        AllUsersListAdapter adapter = new AllUsersListAdapter(users, this, showLoadMore);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
