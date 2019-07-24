package com.example.administrator.patchdemo.patchlib;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.administrator.patchdemo.patchlib.AssetUtils;
import com.example.administrator.patchdemo.patchlib.DexUtils;
import com.example.administrator.patchdemo.patchlib.SignaChecker;

import java.io.File;
import java.io.IOException;

public class Patch {
    private static final String TAG = "patchfix";
    private static Context sContext;
    private static SignaChecker sSignChecker;
    private static final String DEX_DIR = "patchfix";
    private static final String DEX_OPT_DEX = "patchopt";
    private static final String HACK_DEX = "hack.apk";
    public static void init(Context context){
        sContext=context;
        sSignChecker=new SignaChecker(context);
        //   /data/user/0/com.example.administrator.patchdemo/files/patchfix
        File dexDir=new File(context.getFilesDir(),DEX_DIR);
        dexDir.mkdir();

        String dexPath=null;
        try{
            dexPath=AssetUtils.copyAsset(context,HACK_DEX,dexDir);
        } catch (IOException e) {
            Log.e(TAG, "copy " + HACK_DEX + " failed");
            e.printStackTrace();
        }
        if(dexPath!=null) {
            loadPatch(dexPath, false);
        }else{
            Log.e(TAG, "注入文件不存在");
        }
    }

    /**
     * 加载补丁
     * @param dexPath
     * @param verify
     */
    public static void loadPatch(String dexPath, boolean verify) {
        if (sContext == null) {
            Log.e(TAG, "context is null");
            return;
        }

        if (!new File(dexPath).exists()) {
            Log.e(TAG, dexPath + " is null");
            return;
        }

        if (verify && !sSignChecker.verifySign(dexPath)) {
            Log.e(TAG, "patch verify failed");
            return;
        }

        File dexOptDir=new File(sContext.getFilesDir(),DEX_OPT_DEX);
        dexOptDir.mkdir();
        try{
            DexUtils.injectDexAtFirst(dexPath,dexOptDir.getAbsolutePath());
        }catch (Exception e){
            Log.e(TAG,"inject" +dexPath+"failed");
            e.printStackTrace();
        }
    }
}
