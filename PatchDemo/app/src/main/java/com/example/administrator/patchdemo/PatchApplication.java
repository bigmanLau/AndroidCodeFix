package com.example.administrator.patchdemo;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.example.administrator.patchdemo.patchlib.Patch;

public class PatchApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
       Patch.init(this);
       Patch.loadPatch(Environment.getExternalStorageDirectory().getPath().concat("/patch.jar"),!BuildConfig.DEBUG);
    }
}
