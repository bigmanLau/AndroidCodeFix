package com.bigman.tech.utils

import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ProGuardTransform
import com.bigman.tech.BigmanExtension
import groovy.xml.Namespace
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.util.JavaEnvUtils
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project


class PatchAndroidUtils {
    private static final String PATCH_NAME = "patch.jar"
    /**
     * 应用混淆规则
     * @param proguardTask
     * @param mappingFile
     */
    static void applymapping(TransformTask proguardTask, File mappingFile) {
        if (proguardTask) {
            ProGuardTransform transform = (ProGuardTransform) proguardTask.getTransform()
            if (mappingFile.exists()) {
                transform.applyTestedMapping(mappingFile)
            } else {
                BLogger.i("混淆规则文件${mappingFile} 不存在")
            }
        }
    }

    /**
     * 获取app 应用名称
     * @param manifestFiles
     * @return
     */
    static String getApplication(Set<File> manifestFiles) {
        //递归获取所有的输入文件
        manifestFiles = PatchFileUtils.getFiles(manifestFiles)
        File manifestFile = null
        for (File file : manifestFiles) {
            //找到AndroidManifest.xml文件
            if (file.exists() && file.absolutePath.endsWith("AndroidManifest.xml")) {
                manifestFile = file
                break
            }
        }
        //开始解析xml文件
        Node manifest = new XmlParser().parse(manifestFile)
        Namespace androidTag = new groovy.xml.Namespace("http://schemas.android.com/apk/res/android", 'android')
        String applicationName = manifest.application[0].attribute(androidTag.name)
        if (applicationName != null) {
            return applicationName.replace(".", "/")
        }
        return null

    }

    /**
     * 执行dex命令 生成补丁patch.jar文件
     * @param project
     * @param classDir
     * @return
     */
    static String dex(Project project, File classDir) {
        //patch.jar
        String patchPatch = classDir.parent + "/" + PATCH_NAME
        if (classDir.listFiles().size()) {
            String sdkDir = getSdkDir(project)
            if (sdkDir == null) {
                throw new InvalidUserDataException('$ANDROID_HOME 安卓安装目录找不到')
            }

            String buildToolsVersion = project.android.buildToolsVersion
            //判断命令文件 后缀格式
            String cmdExt = Os.isFamily(Os.FAMILY_WINDOWS) ? ".bat" : ""
            ByteArrayOutputStream stdout = new ByteArrayOutputStream()
            //todo 执行dex命令输出一个jar文件实际是一个dex
            project.exec {
                commandLine "${sdkDir}/build-tools/${buildToolsVersion}/dx${cmdExt}",
                        '--dex',
                        "--output=${patchPatch}",
                        "${classDir.absolutePath}"
                standardOutput = stdout
            }
            String error = stdout.toString().trim()
            if (error) {
                BLogger.i("dex error 转dex报错:${error}")
            }
        }
        return patchPatch
    }

    /**
     * 获取安卓sdk目录
     * @param project
     * @return
     */
    static String getSdkDir(Project project) {
        Properties properties = new Properties()
        File localProps = project.rootProject.file("local.properties")
        if (localProps.exists()) {
            properties.load(localProps.newDataInputStream())
            return properties.getProperty("sdk.dir")
        } else {
            return System.getenv("ANDROID_HOME")
        }
    }

    static signPatch(String patchPath, BigmanExtension extension) {
        File patchFile = new File(patchPath)
        if (!patchFile.exists() || !extension.sign) {
            //如果没有补丁文件或者没有签名文件直接返回
            return
        }

        if (extension.storeFile == null || !extension.storeFile.exists()) {
            throw new IllegalArgumentException("签名文件不存在：签名失败")
        }
        BLogger.i("签名补丁开始")
        //签名命令
        List<String> command = [JavaEnvUtils.getJdkExecutable("jarsigner"),
                                '-verbose',
                                '-sigalg', 'MD5withRSA',
                                '-digestalg', 'SHA1',
                                '-keystore', extension.storeFile.absolutePath,
                                '-keypass', extension.keyPassword,
                                '-storepass', extension.storePassword,
                                patchFile.absolutePath,
                                extension.keyAlias
        ]
        Process proc=command.execute()
        Thread outThread=new Thread(new Runnable() {
            @Override
            void run() {
                  int b
                 while ((b=proc.inputStream.read())!=-1){
                     System.out.write(b)
                 }
            }
        })

        Thread errThread=new Thread(new Runnable() {
            @Override
            void run() {
                int b
                while ((b=proc.errorStream.read())!=-1){
                    System.out.write(b)
                }
            }
        })

        outThread.start()
        errThread.start()
        //子线程比较耗时Process.waitFor()去等待子线程运行结束
        int result=proc.waitFor()
        outThread.join()
        errThread.join()
        if (result!=0){
            throw new GradleException("签名失败")
        }

    }
}