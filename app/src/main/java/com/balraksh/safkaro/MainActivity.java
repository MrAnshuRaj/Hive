package com.balraksh.safkaro;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.balraksh.safkaro.ui.home.HomeActivity;
import com.balraksh.safkaro.ui.permission.PermissionActivity;
import com.balraksh.safkaro.utils.PermissionHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Class<?> destination = PermissionHelper.hasRequiredPermissions(this)
                ? HomeActivity.class
                : PermissionActivity.class;
        startActivity(new Intent(this, destination));
        finish();
    }
}
