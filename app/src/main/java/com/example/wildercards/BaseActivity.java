package com.example.wildercards;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;



public class BaseActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

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

    private void handleMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            openActivity(MainActivity.class);
        } else if (id == R.id.nav_profile) {
            openActivity(ProfileActivity.class);
        } else if (id == R.id.nav_add) {
            openActivity(ConfirmImageActivity.class);
        }
    }

    protected void openActivity(Class<?> activityClass) {
        if (!this.getClass().equals(activityClass)) {
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    private void highlightCurrentMenuItem() {
        if (this instanceof MainActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else if (this instanceof ProfileActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        } else if (this instanceof ConfirmImageActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_add);
        }
    }
}