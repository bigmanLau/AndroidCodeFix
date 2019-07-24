package com.example.administrator.patchdemo.patchlib;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class SignaChecker {
    private final static String TAG = "PatchFix";
    private   PublicKey mPublicKey;

    public SignaChecker(Context context) {
        try{
            PackageManager pm=context.getPackageManager();
            String packageName=context.getPackageName();

            PackageInfo packageInfo=pm.getPackageInfo(packageName,PackageManager.GET_SIGNATURES);
            CertificateFactory certificateFactory=CertificateFactory.getInstance("X.509");
            ByteArrayInputStream stream=new ByteArrayInputStream(packageInfo.signatures[0].toByteArray());
            X509Certificate certificate= (X509Certificate) certificateFactory.generateCertificate(stream);
            mPublicKey=certificate.getPublicKey();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 验证签名
     * @param dexPath
     * @return
     */
    public boolean verifySign(String dexPath) {
        JarFile jarFile=null;
        try{
            jarFile=new JarFile(dexPath);
            JarEntry jarEntry=jarFile.getJarEntry("classes.dex");
            if (jarEntry==null){
                Log.w(TAG, "patch verify failed, classes_dex is null");
                return false;
            }
            loadDigests(jarFile,jarEntry);
            Certificate[] certs=jarEntry.getCertificates();
            if (certs==null){
                Log.w(TAG,"patch verify failed ,certs is null");
            return  false;
            }
            return check(certs);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }finally {
            try{
                if (jarFile!=null){
                    jarFile.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 验证签名
     * @param certs
     * @return
     */
    private boolean check(Certificate[] certs){
      if (certs.length>0){
          for (int i = certs.length-1; i >=0; i--) {
              try{
                  certs[i].verify(mPublicKey) ;
              }catch (Exception e){
                  e.printStackTrace();
              }
          }
      }
      return  false;
    }

    private void loadDigests(JarFile jarFile,JarEntry jarEntry)throws IOException{
        InputStream is=null;
        try{
            is=jarFile.getInputStream(jarEntry);
            byte[] bytes=new byte[8192];
            while(is.read(bytes)>0){

            }
        }finally {
            if (is!=null){
                is.close();
            }
        }
    }
}
