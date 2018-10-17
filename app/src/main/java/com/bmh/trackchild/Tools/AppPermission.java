package com.bmh.trackchild.Tools;

import android.annotation.TargetApi;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;


import com.bmh.trackchild.UI.ConfirmDialogs;

import java.util.ArrayList;
import java.util.List;

public class AppPermission {


    Activity activity;
    SharedPrefs Prefs;
    public static String DENY_PERMISSION="deny";
    public static String NEVER_ASK_PERMISSION="never";


    public AppPermission(Activity activity) {
        this.activity = activity;
        Prefs = new SharedPrefs(activity);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean checkAndRequestPermissions(String[] permissions, int requestCode) {

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            activity.requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), requestCode);
            return true;
        } else {
            return false;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void handleOnRequestPermission(int[] grantResults, int grantResult, String[] permissions, String permission, int firstDenyResId, int neverAskResId) {
        if (grantResults.length > 0 && grantResult == PackageManager.PERMISSION_GRANTED) {
            if (!hasPermissions(activity, permissions)) {
                ((AppPermissionInterFace) activity).onAllPermissionGranted();

            }
        }
        //Permission is denied
        else {
            //Permission denied only
            if (activity.shouldShowRequestPermissionRationale(permission)) {

                ConfirmDialogs dialogFragment = ConfirmDialogs.newInstance(activity.getResources().getString(firstDenyResId), StaticValues.DIALOG_TYPE_YES_NO);
                dialogFragment.show(activity.getFragmentManager(), DENY_PERMISSION);
            }
            //permission denied and disabled
            else {
                ConfirmDialogs dialogFragment = ConfirmDialogs.newInstance(activity.getResources().getString(neverAskResId), StaticValues.DIALOG_TYPE_YES_NO);
                dialogFragment.show(activity.getFragmentManager(), NEVER_ASK_PERMISSION);
            }

        }
    }

    public void navigateToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }


}
