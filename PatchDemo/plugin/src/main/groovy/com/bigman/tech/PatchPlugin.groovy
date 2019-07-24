package com.bigman.tech

import com.android.SdkConstants
import com.bigman.tech.utils.BLogger
import com.bigman.tech.utils.PatchAndroidUtils
import com.bigman.tech.utils.PatchFileUtils
import com.bigman.tech.utils.PatchMapUtils
import com.bigman.tech.utils.PatchProcessor
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class PatchPlugin implements Plugin<Project> {
    private BigmanExtension extension
    private static final String DEBUG = "debug"
    //补丁对比文件放置位置
    private static final String PATCH_DIR = "patchDir"
    private static final String PATCH_PATCHES = "bigmanPatches"
    //混淆规则文件
    private static final String MAPPING_TXT = "mapping.txt"
    //全部类的hash记录文件
    private static final String HASH_TXT = "hash.txt"

    private Task patchJarBeforeDexTask
    private List<File> patchList = []

    @Override
    void apply(Project project) {
        //第一步创建扩展
        project.extensions.create("bigman", BigmanExtension, project)

        //afterEvaluate是一般比较常见的一个配置参数的回调方式,只要project配置成功均会调用,
        project.afterEvaluate {
            //通过名字找到配置里面的bigman扩展
            extension = project.extensions.findByName("bigman") as BigmanExtension

            project.android.applicationVariants.each {
                variant ->
                    //如果是生产环境或者测试环境开启了更新模式
                    if (!variant.name.contains(DEBUG) || (variant.name.contains(DEBUG) && extension.debugOn)) {

                        File patchDir

                        //初始化日志打印器
                        BLogger.init(variant)

                        //Gradle版本为1.5-2.x
                        Task dexTaskLower = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")
                        //Gradle版本为3.0+
                        Task dexTaskHigner = project.tasks.findByName("transformClassesWithDexBuilderFor${variant.name.capitalize()}")

                        //获取生成dex的任务
                        Task dexTask
                        if (dexTaskLower != null) {
                            dexTask = dexTaskLower
                        } else if (dexTaskHigner != null) {
                            dexTask = dexTaskHigner
                        } else {
                            BLogger.i("Gradle 版本暂不支持")
                            return
                        }

                        //获取处理manifest的任务
                        Task manifestTask = project.tasks.findByName("process${variant.name.capitalize()}Manifest")
                        //获取混淆任务
                        Task proguardTask = project.tasks.findByName("transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}")

                        Set<File> manifestFiles = manifestTask.outputs.files.files

                        //获取到放置上一个安装包生成的hash文件和mapping文件目录
                        File oldPatchDir = PatchFileUtils.getFileFromProperty(project, PATCH_DIR)

                        //存放类hash值的字典
                        Map hashMap
                        //获取需要打补丁工程里面的mapping和hash文件
                        if (oldPatchDir) {
                            //获取不同变体的混淆规则文件
                            File mappingFile = PatchFileUtils.getVariantFile(oldPatchDir, variant, MAPPING_TXT)
                            //应用混淆规则
                            PatchAndroidUtils.applymapping(proguardTask, mappingFile)

                            //获取不同变体的类hash记录
                            File hashFile = PatchFileUtils.getVariantFile(oldPatchDir, variant, HASH_TXT)
                            hashMap = PatchMapUtils.parseMap(hashFile)
                        }

                        //变体构建根目录 类似debug/myflavor"
                        String dirName = variant.dirName
                        File outPatchDir = new File("${project.buildDir}/outputs/patch_plugin")
                        //输出路径
                        File outputDir = new File("${outPatchDir}/${dirName}")

                        //将要写入hash的输出hash.txt文件
                        File hashFile = new File(outputDir, HASH_TXT)

                        String patchJarBeforeDex = "patchJarBeforeDex${variant.name.capitalize()}"
                        project.task(patchJarBeforeDex) << {
                            Set<File> inputFiles = dexTask.inputs.files.files
                            inputFiles.each {
                                file ->
                                    //打印dex任务返回的输入文件
                                    BLogger.i("transformClassesTask input:${file.absolutePath}")
                            }
                            //取到所有的类文件
                            Set<File> files = PatchFileUtils.getFiles(inputFiles)

                            //---核心逻辑---循环进行代码注入
                            files.each {
                                file ->
                                    //如果是jar文件需要解压后再处理
                                    if (file.name.endsWith(SdkConstants.DOT_JAR)) {
                                        PatchProcessor.processJar(file, hashFile, hashMap, patchDir, extension)
                                    } else if (file.name.endsWith(SdkConstants.DOT_CLASS)) {
                                        PatchProcessor.processClass(file, hashFile, hashMap, patchDir, extension)
                                    }
                            }
                        }

                        //获取到hook任务
                        patchJarBeforeDexTask = project.tasks[patchJarBeforeDex]

                        //任务执行之前
                        patchJarBeforeDexTask.doFirst {
                            BLogger.init(variant)
                            //通过解析manifest文件获取Application子类全限定名
                            String applicationName = PatchAndroidUtils.getApplication(manifestFiles)
                            if (applicationName != null) {
                                //application不需要添加补丁
                                extension.excludeClass.add(applicationName)
                            }
                            extension.excludeClass.each {
                                name->
                                    BLogger.i("排除类名称:${name}")
                            }

                            //开始任务之前 先清空输出目录里的旧文件
                            //开始任务之前 先清空输出目录里的旧文件
                            outputDir.deleteDir()
                            outputDir.mkdirs()
                            //创建新的hash文件
                            hashFile.createNewFile()

                            if (oldPatchDir) {
                                //生成的补丁目录位置
                                patchDir = new File("${outputDir}/patch")
                                patchDir.mkdirs()
                                patchList.add(patchDir)
                            }
                        }

                        patchJarBeforeDexTask.doLast {
                            if (proguardTask) {
                                //生成新的混淆文件
                                File mapFile = new File("${project.buildDir}/output/mapping/${variant.dirName}/${MAPPING_TXT}")
                                File newMapFile = new File("${outputDir}/${variant.dirName}/${MAPPING_TXT}")
                                FileUtils.copyFile(mapFile, newMapFile)
                            }
                        }

                        //补丁任务将在dex任务获取完依赖之后再进行
                         patchJarBeforeDexTask.dependsOn dexTask.taskDependencies.getDependencies(dexTask)
                        //dex任务将在补丁任务执行完之后再执行
                        dexTask.dependsOn patchJarBeforeDexTask

                        //定义开启补丁任务名称
                        String patchTaskName="patch${variant.name.capitalize()}Patch"

                        //创建打补丁任务
                        project.task(patchTaskName) <<{
                            if(patchDir){
                                //通过系统sdk工具生成补丁
                              String patchPatch=PatchAndroidUtils.dex(project,patchDir)
                                //签名补丁
                                PatchAndroidUtils.signPatch(patchPatch,extension)
                            }
                        }
                        Task bigmanPatchTask=project.tasks[patchTaskName]
                        //打补丁任务会在预执行dex任务之后开始
                        bigmanPatchTask.dependsOn patchJarBeforeDexTask
                    }
            }

            //创建一个任务
            project.task(PATCH_PATCHES) <<{
                patchList.each {
                    patchDir->
                        String patchPath=PatchAndroidUtils.dex(project,patchDir)
                        PatchAndroidUtils.signPatch(patchPath,extension)
                }
            }

            //这个任务需要先执行beforedex任务执行之后才执行
            project.tasks[PATCH_PATCHES].dependsOn patchJarBeforeDexTask
        }
    }
}