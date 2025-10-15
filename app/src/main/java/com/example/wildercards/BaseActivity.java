package com.example.wildercards;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class BaseActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Dialog mLoadingDialog;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        View fullView = getLayoutInflater().inflate(R.layout.activity_base, null);
        FrameLayout content = fullView.findViewById(R.id.content_frame);
        getLayoutInflater().inflate(layoutResID, content, true);
        super.setContentView(fullView);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            handleMenuItem(item);
            return true;
        });

       highlightCurrentMenuItem();
    }

    protected void showLoadingDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new Dialog(this);
            mLoadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mLoadingDialog.setContentView(R.layout.dialog_loading);
            if (mLoadingDialog.getWindow() != null) {
                mLoadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.setCanceledOnTouchOutside(false);
        }

        if (!mLoadingDialog.isShowing()) {
            mLoadingDialog.show();
        }
    }

    protected void hideLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    private void handleMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            openActivityWithAnimation(MainActivity.class, 0);
        } else if (id == R.id.nav_add) {
            openActivityWithAnimation(ConfirmCardActivity.class, 1);
        } else if (id == R.id.nav_profile) {
            openActivityWithAnimation(ProfileActivity.class, 2);
        }
    }

    protected void openActivityWithAnimation(Class<?> activityClass, int newPosition) {
        if (!this.getClass().equals(activityClass)) {
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            int currentPosition = getCurrentActivityPosition();
            if (currentPosition != -1) {
                if (newPosition > currentPosition) {
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        highlightCurrentMenuItem();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
        mLoadingDialog = null;
    }

    private int getCurrentActivityPosition() {
        if (this instanceof MainActivity) {
            return 0;
        } else if (this instanceof ConfirmCardActivity) {
            return 1;
        } else if (this instanceof ProfileActivity) {
            return 2;
        }
        return -1;
    }

    private void highlightCurrentMenuItem() {
        if (bottomNavigationView != null) {
            if (this instanceof MainActivity) {
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
            } else if (this instanceof ProfileActivity) {
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            } else if (this instanceof ConfirmCardActivity) {
                bottomNavigationView.setSelectedItemId(R.id.nav_add);
            }
        }
    }
}
