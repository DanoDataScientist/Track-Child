package com.bmh.trackchild.Activities;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.bmh.trackchild.R;
import com.bmh.trackchild.Tools.AppPermission;
import com.bmh.trackchild.Tools.AppPermissionInterFace;
import com.bmh.trackchild.Tools.PermissionUtil;
import com.bmh.trackchild.Tools.SharedPrefs;
import com.bmh.trackchild.Tools.StaticValues;
import com.bmh.trackchild.UI.ConfirmDialogInterface;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,AppPermissionInterFace,ConfirmDialogInterface {

    Button btnParent, btnChild;
    SharedPrefs sharedPrefs;
    AppPermission appPermission;
    Intent intent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        sharedPrefs = new SharedPrefs(this);
        //check if user had registered before or not
        if (checkRegistration()) {
            identifyUser();
        } else {
            setContentView(R.layout.activity_main);
            appPermission = new AppPermission(this);
            initComponent();
        }


    }

    public void identifyUser() {
        //User is a parent and he/she had saved his/her child's DeviceName successfully.
        if (sharedPrefs.getPreferences(R.string.Key_UserType, "").equals(StaticValues.USER_IS_PARENT)) {
            startActivity(new Intent(this, TrackActivity.class));
            finish();
        }

        //User is a parent but he/she had not saved his/her child's DeviceName successfully.
        else if (sharedPrefs.getPreferences(R.string.Key_UserType, "").equals(StaticValues.USER_IS_PARENT)
                && sharedPrefs.getPreferences(R.string.Key_ChildDeviceName, "").equals("")) {
            startActivity(new Intent(this, ChildDeviceActivity.class));
            finish();
        } else if (sharedPrefs.getPreferences(R.string.Key_UserType, "").equals(StaticValues.USER_IS_CHILD)) {
            startActivity(new Intent(this, ChildActivity.class));
            finish();
        }

    }

    private boolean checkRegistration() {
        //check if the sharedPreference have saved values for User Type or not
        if (!sharedPrefs.getPreferences(R.string.Key_UserType, "").equals(""))
            return true;
        return false;
    }

    private void initComponent() {
        btnParent = (Button) findViewById(R.id.btnParent);
        btnChild = (Button) findViewById(R.id.btnChild);

        btnParent.setOnClickListener(this);
        btnChild.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View view) {
        intent = new Intent(this, RegistrationActivity.class);
        switch (view.getId()) {
            case R.id.btnParent:
                intent.putExtra(StaticValues.USER_TYPE, StaticValues.USER_PARENT);
                break;
            case R.id.btnChild:
                intent.putExtra(StaticValues.USER_TYPE, StaticValues.USER_CHILD);
                break;
        }

        if (PermissionUtil.shouldAskPermission()) {
            appPermission.checkAndRequestPermissions(new String[]{ACCESS_FINE_LOCATION, SEND_SMS, WRITE_EXTERNAL_STORAGE}, StaticValues.PERMISSIONS_REQUEST_ALL);
        } else {
            startActivity(intent);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(ACCESS_FINE_LOCATION)) {
                appPermission.handleOnRequestPermission(grantResults,grantResults[i],permissions,permissions[i],R.string.location_first_deny,R.string.location_never_ask_deny);
            } else if (permissions[i].equals(SEND_SMS)) {
                appPermission.handleOnRequestPermission(grantResults,grantResults[i],permissions,permissions[i],R.string.SMS_first_deny,R.string.SMS_never_ask_deny);

            } else if (permissions[i].equals(WRITE_EXTERNAL_STORAGE)) {
                appPermission.handleOnRequestPermission(grantResults,grantResults[i],permissions,permissions[i],R.string.SD_first_deny,R.string.SD_never_ask_deny);
            }
        }
    }

    @Override
    public void onAllPermissionGranted() {
        startActivity(intent);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if(dialog.getTag().equals(AppPermission.DENY_PERMISSION)) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, SEND_SMS, WRITE_EXTERNAL_STORAGE}, StaticValues.PERMISSIONS_REQUEST_ALL);
        }
        else {
            appPermission.navigateToAppSetting();
        }
        dialog.dismiss();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();

    }

}
