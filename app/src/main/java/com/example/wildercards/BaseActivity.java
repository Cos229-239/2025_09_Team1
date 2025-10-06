package com.example.wildercards;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Dialog mLoadingDialog;

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
            openActivity(MainActivity.class);
        } else if (id == R.id.nav_profile) {
            openActivity(ProfileActivity.class);
        } else if (id == R.id.nav_add) {
            openActivity(AddImageActivity.class);
        }
    }

    protected void openActivity(Class<?> activityClass) {
        if (!this.getClass().equals(activityClass)) {
            showLoadingDialog();
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideLoadingDialog();
        highlightCurrentMenuItem();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
        mLoadingDialog = null;
    }

    private void highlightCurrentMenuItem() {
        if (bottomNavigationView != null) {
            if (this instanceof MainActivity) {
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
            } else if (this instanceof ProfileActivity) {
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            } else if (this instanceof AddImageActivity) {
                bottomNavigationView.setSelectedItemId(R.id.nav_add);
            }
        }
    }
}
