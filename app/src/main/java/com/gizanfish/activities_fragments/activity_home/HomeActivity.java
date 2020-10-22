package com.gizanfish.activities_fragments.activity_home;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.gizanfish.R;
import com.gizanfish.activities_fragments.activity_cart.CartActivity;
import com.gizanfish.activities_fragments.activity_checkout.CheckoutActivity;
import com.gizanfish.activities_fragments.activity_home.fragments.Fragment_Family_Box;
import com.gizanfish.activities_fragments.activity_home.fragments.Fragment_Department;
import com.gizanfish.activities_fragments.activity_home.fragments.Fragment_Main;
import com.gizanfish.activities_fragments.activity_home.fragments.Fragment_Profile;
import com.gizanfish.activities_fragments.activity_home.fragments.fragment_order.Fragment_Order;
import com.gizanfish.activities_fragments.activity_login.LoginActivity;
import com.gizanfish.activities_fragments.activity_notification.NotificationActivity;
import com.gizanfish.activities_fragments.activity_search.SearchActivity;
import com.gizanfish.databinding.ActivityHomeBinding;
import com.gizanfish.language.Language;
import com.gizanfish.models.NotFireModel;
import com.gizanfish.models.NotificationCount;
import com.gizanfish.models.UserModel;
import com.gizanfish.preferences.Preferences;
import com.gizanfish.remote.Api;
import com.gizanfish.share.Common;
import com.gizanfish.singleton.CartSingleton;
import com.gizanfish.tags.Tags;
import com.google.firebase.iid.FirebaseInstanceId;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import io.paperdb.Paper;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private Preferences preferences;
    private FragmentManager fragmentManager;
    private Fragment_Main fragment_main;
    private Fragment_Family_Box fragment_familyBox;
    private Fragment_Order fragment_order;
    private Fragment_Profile fragment_profile;
    private Fragment_Department fragment_department;
    private UserModel userModel;
    private String lang;
    private String token;
    private CartSingleton singleton;
    private int back = 0;

    protected void attachBaseContext(Context newBase) {
        Paper.init(newBase);
        super.attachBaseContext(Language.updateResources(newBase, Paper.book().read("lang", "ar")));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        initView();


    }

    private void initView() {
        fragmentManager = getSupportFragmentManager();
        preferences = Preferences.getInstance();
        userModel = preferences.getUserData(this);
        Paper.init(this);
        lang = Paper.book().read("lang", "ar");
        binding.setLang(lang);


        binding.flSearch.setOnClickListener(view -> {

            Intent intent = new Intent(this, CartActivity.class);
            startActivityForResult(intent, 100);

        });


        binding.flNotification.setOnClickListener(view -> {


            if (userModel != null) {
                readNotificationCount();
                Intent intent = new Intent(HomeActivity.this, NotificationActivity.class);
                startActivity(intent);

            } else {
                Common.CreateDialogAlert(this, getString(R.string.please_sign_in_or_sign_up));
            }

        });

        binding.flHome.setOnClickListener(v -> {
            displayFragmentMain();
        });

        binding.flDepartments.setOnClickListener(v -> {
            displayFragmentDepartment();
        });

        binding.flFamilyBox.setOnClickListener(v -> {
            displayFragmentFamilyBox();
        });

        binding.flMyOrder.setOnClickListener(v -> {

            displayFragmentMyOrder();
        });

        binding.flProfile.setOnClickListener(v -> {
            displayFragmentProfile();
        });

        displayFragmentMain();

        if (userModel != null) {
            EventBus.getDefault().register(this);
            getNotificationCount();
            updateTokenFireBase();

        }


    }

    private void getNotificationCount() {
        Api.getService(Tags.base_url)
                .getUnreadNotificationCount(userModel.getUser().getToken())
                .enqueue(new Callback<NotificationCount>() {
                    @Override
                    public void onResponse(Call<NotificationCount> call, Response<NotificationCount> response) {
                        if (response.isSuccessful()) {
                            binding.setNotCount(response.body().getCount());
                        } else {
                            try {
                                Log.e("errorNotCode", response.code() + "__" + response.errorBody().string());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if (response.code() == 500) {
                                Toast.makeText(HomeActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(HomeActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<NotificationCount> call, Throwable t) {
                        try {
                            if (t.getMessage() != null) {
                                Log.e("error_not_code", t.getMessage() + "__");

                                if (t.getMessage().toLowerCase().contains("failed to connect") || t.getMessage().toLowerCase().contains("unable to resolve host")) {
                                    Toast.makeText(HomeActivity.this, getString(R.string.something), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(HomeActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            Log.e("Error", e.getMessage() + "__");
                        }
                    }
                });
    }

    private void readNotificationCount() {
        binding.setNotCount(0);
    }


    public void displayFragmentMain() {
        try {
            Log.e("ddd","fff");
            updateHomUi();
            if (fragment_main == null) {
                fragment_main = Fragment_Main.newInstance();
            }


            if (fragment_familyBox != null && fragment_familyBox.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_familyBox).commit();
            }
            if (fragment_department != null && fragment_department.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_department).commit();
            }
            if (fragment_order != null && fragment_order.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_order).commit();
            }

            if (fragment_profile != null && fragment_profile.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_profile).commit();
            }
            if (fragment_main.isAdded()) {
                fragmentManager.beginTransaction().show(fragment_main).commit();

            } else {
                fragmentManager.beginTransaction().add(R.id.fragment_app_container, fragment_main, "fragment_main").addToBackStack("fragment_main").commit();

            }
            binding.setTitle(getString(R.string.home));
        } catch (Exception e) {
        }

    }

    public void displayFragmentDepartment() {

        try {
            updateDepartmentsUi();
            if (fragment_department == null) {
                fragment_department = Fragment_Department.newInstance();
            }


            if (fragment_main != null && fragment_main.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_main).commit();
            }
            if (fragment_profile != null && fragment_profile.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_profile).commit();
            }

            if (fragment_order != null && fragment_order.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_order).commit();
            }

            if (fragment_familyBox != null && fragment_familyBox.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_familyBox).commit();
            }

            if (fragment_department.isAdded()) {
                fragmentManager.beginTransaction().show(fragment_department).commit();

            } else {
                fragmentManager.beginTransaction().add(R.id.fragment_app_container, fragment_department, "fragment_department").addToBackStack("fragment_department").commit();

            }
            binding.setTitle(getString(R.string.department));
        } catch (Exception e) {
        }
    }

    public void displayFragmentFamilyBox() {
        try {
            updateFamilyBoxUi();
            if (fragment_familyBox == null) {
                fragment_familyBox = Fragment_Family_Box.newInstance();
            }

            if (fragment_profile != null && fragment_profile.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_profile).commit();
            }
            if (fragment_department != null && fragment_department.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_department).commit();
            }
            if (fragment_main != null && fragment_main.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_main).commit();
            }

            if (fragment_order != null && fragment_order.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_order).commit();
            }
            if (fragment_familyBox.isAdded()) {
                fragmentManager.beginTransaction().show(fragment_familyBox).commit();
            } else {
                fragmentManager.beginTransaction().add(R.id.fragment_app_container, fragment_familyBox, "fragment_family_box").addToBackStack("fragment_family_box").commit();

            }
            binding.setTitle(getString(R.string.cart));
        } catch (Exception e) {
        }

    }

    public void displayFragmentMyOrder() {

        try {
            updateOrderUi();
            if (fragment_order == null) {
                fragment_order = Fragment_Order.newInstance();
            }


            if (fragment_main != null && fragment_main.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_main).commit();
            }
            if (fragment_department != null && fragment_department.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_department).commit();
            }

            if (fragment_profile != null && fragment_profile.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_profile).commit();
            }

            if (fragment_familyBox != null && fragment_familyBox.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_familyBox).commit();
            }

            if (fragment_order.isAdded()) {
                fragmentManager.beginTransaction().show(fragment_order).commit();

            } else {
                fragmentManager.beginTransaction().add(R.id.fragment_app_container, fragment_order, "fragment_order").addToBackStack("fragment_order").commit();

            }
            binding.setTitle(getString(R.string.offers));
        } catch (Exception e) {
        }
    }

    public void displayFragmentProfile() {

        try {
            updateProfileUi();
            if (fragment_profile == null) {
                fragment_profile = Fragment_Profile.newInstance();
            }


            if (fragment_main != null && fragment_main.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_main).commit();
            }
            if (fragment_department != null && fragment_department.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_department).commit();
            }

            if (fragment_order != null && fragment_order.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_order).commit();
            }

            if (fragment_familyBox != null && fragment_familyBox.isAdded()) {
                fragmentManager.beginTransaction().hide(fragment_familyBox).commit();
            }

            if (fragment_profile.isAdded()) {
                fragmentManager.beginTransaction().show(fragment_profile).commit();

            } else {
                fragmentManager.beginTransaction().add(R.id.fragment_app_container, fragment_profile, "fragment_profile").addToBackStack("fragment_profile").commit();

            }
            binding.setTitle(getString(R.string.more));
        } catch (Exception e) {
        }
    }



    private void updateHomUi(){
        binding.flHome.setBackgroundResource(R.drawable.small_rounded_btn_primary);
        binding.iconHome.setColorFilter(ContextCompat.getColor(this,R.color.white));
        binding.tvHome.setTextColor(ContextCompat.getColor(this,R.color.white));
        binding.tvHome.setVisibility(View.VISIBLE);

        binding.flDepartments.setBackgroundResource(0);
        binding.iconDepts.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvDepts.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvDepts.setVisibility(View.GONE);

        binding.flFamilyBox.setBackgroundResource(0);
        binding.iconBox.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvBox.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvBox.setVisibility(View.GONE);

        binding.flMyOrder.setBackgroundResource(0);
        binding.iconOrder.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvOrder.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvOrder.setVisibility(View.GONE);

        binding.flProfile.setBackgroundResource(0);
        binding.iconUser.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvUser.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvUser.setVisibility(View.GONE);

    }
    private void updateDepartmentsUi(){
        binding.flHome.setBackgroundResource(0);
        binding.iconHome.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvHome.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvHome.setVisibility(View.GONE);

        binding.flDepartments.setBackgroundResource(R.drawable.small_rounded_btn_primary);
        binding.iconDepts.setColorFilter(ContextCompat.getColor(this,R.color.white));
        binding.tvDepts.setTextColor(ContextCompat.getColor(this,R.color.white));
        binding.tvDepts.setVisibility(View.VISIBLE);

        binding.flFamilyBox.setBackgroundResource(0);
        binding.iconBox.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvBox.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvBox.setVisibility(View.GONE);

        binding.flMyOrder.setBackgroundResource(0);
        binding.iconOrder.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvOrder.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvOrder.setVisibility(View.GONE);

        binding.flProfile.setBackgroundResource(0);
        binding.iconUser.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvUser.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvUser.setVisibility(View.GONE);

    }
    private void updateFamilyBoxUi(){
        binding.flHome.setBackgroundResource(0);
        binding.iconHome.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvHome.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvHome.setVisibility(View.GONE);

        binding.flDepartments.setBackgroundResource(0);
        binding.iconDepts.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvDepts.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvDepts.setVisibility(View.GONE);

        binding.flFamilyBox.setBackgroundResource(R.drawable.small_rounded_btn_primary);
        binding.iconBox.setColorFilter(ContextCompat.getColor(this,R.color.white));
        binding.tvBox.setTextColor(ContextCompat.getColor(this,R.color.white));
        binding.tvBox.setVisibility(View.VISIBLE);

        binding.flMyOrder.setBackgroundResource(0);
        binding.iconOrder.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvOrder.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvOrder.setVisibility(View.GONE);

        binding.flProfile.setBackgroundResource(0);
        binding.iconUser.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvUser.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvUser.setVisibility(View.GONE);

    }
    private void updateOrderUi(){
        binding.flHome.setBackgroundResource(0);
        binding.iconHome.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvHome.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvHome.setVisibility(View.GONE);

        binding.flDepartments.setBackgroundResource(0);
        binding.iconDepts.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvDepts.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvDepts.setVisibility(View.GONE);

        binding.flFamilyBox.setBackgroundResource(0);
        binding.iconBox.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvBox.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvBox.setVisibility(View.GONE);

        binding.flMyOrder.setBackgroundResource(R.drawable.small_rounded_btn_primary);
        binding.iconOrder.setColorFilter(ContextCompat.getColor(this,R.color.white));
        binding.tvOrder.setTextColor(ContextCompat.getColor(this,R.color.white));
        binding.tvOrder.setVisibility(View.VISIBLE);

        binding.flProfile.setBackgroundResource(0);
        binding.iconUser.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvUser.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvUser.setVisibility(View.GONE);

    }
    private void updateProfileUi(){
        binding.flHome.setBackgroundResource(0);
        binding.iconHome.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvHome.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvHome.setVisibility(View.GONE);

        binding.flDepartments.setBackgroundResource(0);
        binding.iconDepts.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvDepts.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvDepts.setVisibility(View.GONE);

        binding.flFamilyBox.setBackgroundResource(0);
        binding.iconBox.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvBox.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvBox.setVisibility(View.GONE);

        binding.flMyOrder.setBackgroundResource(0);
        binding.iconOrder.setColorFilter(ContextCompat.getColor(this,R.color.black));
        binding.tvOrder.setTextColor(ContextCompat.getColor(this,R.color.black));
        binding.tvOrder.setVisibility(View.GONE);

        binding.flProfile.setBackgroundResource(R.drawable.small_rounded_btn_primary);
        binding.iconUser.setColorFilter(ContextCompat.getColor(this,R.color.white));
        binding.tvUser.setTextColor(ContextCompat.getColor(this,R.color.white));
        binding.tvUser.setVisibility(View.VISIBLE);

    }

    private void updateTokenFireBase() {

        FirebaseInstanceId.getInstance()
                .getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                token = task.getResult().getToken();

                try {
                    Log.e("llll", userModel.getUser().getToken());
                    Api.getService(Tags.base_url)
                            .updatePhoneToken(userModel.getUser().getToken(), token, "android")
                            .enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        Log.e("token", "updated successfully");
                                    } else {
                                        try {

                                            Log.e("errorToken", response.code() + "_" + response.errorBody().string());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    try {

                                        if (t.getMessage() != null) {
                                            Log.e("errorToken2", t.getMessage());
                                            if (t.getMessage().toLowerCase().contains("failed to connect") || t.getMessage().toLowerCase().contains("unable to resolve host")) {
                                                Toast.makeText(HomeActivity.this, R.string.something, Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(HomeActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                    } catch (Exception e) {
                                    }
                                }
                            });
                } catch (Exception e) {


                }

            }
        });
    }

    public void logout() {
        if (userModel != null) {


            ProgressDialog dialog = Common.createProgressDialog(this, getString(R.string.wait));
            dialog.show();


            FirebaseInstanceId.getInstance()
                    .getInstanceId().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    token = task.getResult().getToken();

                    Api.getService(Tags.base_url)
                            .logout(userModel.getUser().getToken(), token, "android")
                            .enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    dialog.dismiss();
                                    if (response.isSuccessful()) {
                                        Log.e("dd", "ddd");
                                        preferences.clear(HomeActivity.this);
                                        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                        if (manager != null) {
                                            manager.cancel(Tags.not_tag, Tags.not_id);
                                        }
                                        navigateToSignInActivity();


                                    } else {
                                        dialog.dismiss();
                                        try {
                                            Log.e("error", response.code() + "__" + response.errorBody().string());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        if (response.code() == 500) {
                                            Toast.makeText(HomeActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(HomeActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    try {
                                        dialog.dismiss();
                                        if (t.getMessage() != null) {
                                            Log.e("error", t.getMessage() + "__");

                                            if (t.getMessage().toLowerCase().contains("failed to connect") || t.getMessage().toLowerCase().contains("unable to resolve host")) {
                                                Toast.makeText(HomeActivity.this, getString(R.string.something), Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(HomeActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e("Error", e.getMessage() + "__");
                                    }
                                }
                            });

                }
            });


        } else {
            navigateToSignInActivity();
        }

    }

    public void refreshActivity(String lang) {
        Paper.book().write("lang", lang);
        Language.setNewLocale(this, lang);
        new Handler()
                .postDelayed(() -> {

                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }, 1050);


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void listenToNotifications(NotFireModel notFireModel) {
        if (userModel != null) {
            getNotificationCount();

        }
    }

    @Override
    public void onBackPressed() {

        if (fragment_main != null && fragment_main.isAdded() && fragment_main.isVisible()) {
            if (userModel != null) {
                if (singleton.getItemCartModelList() != null && singleton.getItemCartModelList().size() > 0) {
                    if (back == 0) {
                        back = 1;
                        String sound_Path = "android.resource://" + getPackageName() + "/" + R.raw.not;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                            String CHANNEL_ID = "my_channel_02";
                            CharSequence CHANNEL_NAME = "my_channel_name";
                            int IMPORTANCE = NotificationManager.IMPORTANCE_HIGH;

                            final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE);
                            channel.setShowBadge(true);
                            channel.setSound(Uri.parse(sound_Path), new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                                    .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                                    .build()
                            );

                            builder.setChannelId(CHANNEL_ID);
                            builder.setSound(Uri.parse(sound_Path), AudioManager.STREAM_NOTIFICATION);
                            builder.setSmallIcon(R.drawable.logo);


                            builder.setContentTitle(getResources().getString(R.string.cart));


                            builder.setContentText(getResources().getString(R.string.cart_not_empty));


                            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
                            builder.setLargeIcon(bitmap);
                            manager.createNotificationChannel(channel);
                            manager.notify(new Random().nextInt(200), builder.build());
                        } else {

                            final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

                            builder.setSound(Uri.parse(sound_Path), AudioManager.STREAM_NOTIFICATION);
                            builder.setSmallIcon(R.drawable.logo);

                            builder.setContentTitle(getResources().getString(R.string.cart));


                            builder.setContentText(getResources().getString(R.string.cart_not_empty));


                            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
                            builder.setLargeIcon(bitmap);
                            manager.notify(new Random().nextInt(200), builder.build());

                        }
                    } else {
                        // back=1;
                        CreatecloseDialog();

                    }
                } else {
                    finish();
                }
            } else {
                navigateToSignInActivity();
            }
        } else {
            displayFragmentMain();
        }
    }

    private void CreatecloseDialog() {

        final androidx.appcompat.app.AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(true)
                .create();

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        TextView tvDelete = view.findViewById(R.id.tvDelete);
        TextView tvCancel = view.findViewById(R.id.tvCancel);
        TextView tvtitle = view.findViewById(R.id.tvtitle);
        TextView tvcart = view.findViewById(R.id.tvcart);

        tvDelete.setText(getResources().getString(R.string.back));
        tvcart.setText(getResources().getString(R.string.cart));
        tvtitle.setText(getResources().getString(R.string.if_you));
        tvCancel.setOnClickListener(v -> {
            back = 0;
            dialog.dismiss();

        });

        tvDelete.setOnClickListener(v -> {
            try {
                dialog.dismiss();
                finish();
            } catch (Exception e) {
            }

            dialog.dismiss();
        });

        dialog.getWindow().getAttributes().windowAnimations = R.style.dialog_congratulation_animation;
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_window_bg);
        dialog.setView(view);
        dialog.show();

    }

    private void navigateToSignInActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<Fragment> fragmentList = fragmentManager.getFragments();
        for (Fragment fragment : fragmentList) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragmentList = fragmentManager.getFragments();
        for (Fragment fragment : fragmentList) {
            fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public void updateCartCount(int count) {
binding.setCartcount(count);
    }

    @Override
    protected void onResume() {
        super.onResume();
        singleton = CartSingleton.newInstance();
        if (singleton.getItemCartModelList() != null) {
            updateCartCount(singleton.getItemCount());
        }

    }
}
