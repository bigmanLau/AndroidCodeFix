package com.bigman.tech.utils

import jdk.internal.util.xml.impl.Input
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class PatchFileUtils {

    /**
     * 通过构建参数传入路径 然后获取该路径文件
     * @param project
     * @param property 路径名称
     * @return
     */
    static File getFileFromProperty(Project project, String property) {
        File file = null
        if (project.hasProperty(property)) {
            file = new File(project.getProperties()[property])
            if (!file.exists()) {
                throw new InvalidUserDataException("传入路径文件${project.getProperties()[project]} 不存在")
            }
            if (!file.isDirectory()) {
                throw new InvalidUserDataException("传入路径文件${project.getProperties()[project]} 不是一个目录")
            }
        }
        return file
    }

    /**
     * 获取不同变体的mapping文件
     * @param dir  目录
     * @param variant 变体
     * @param fileName 文件名称
     * @return
     */
    static File getVariantFile(File dir,def variant,String fileName){
        return  new File("${dir}/${variant.dirName}/${fileName}")
    }

    /**
     * 递归获取所有的输入文件
     * @param inputFiles
     * @return
     */
    static Set<File> getFiles(Set<File> inputFiles){
        Set<File> files=[]
        for (File file :inputFiles){
            if (file.directory) {
                file.eachFileRecurse {
                    f ->
                        if (f.file) {
                            files.add(f)
                        }
                }
            }else{
                files.add(file)
            }
        }
        return files
    }

    /**
     * 格式化路径
     */
    static String formatPath(String path){
        return path.replace("\\","/")
    }

    /**
     * 递归解压jar包
     * @param jar
     * @param dest
     */
    static void unZipJar(File jar,File dest){
        JarFile jarFile=new JarFile(jar)
        Enumeration<JarEntry> jarEntries=jarFile.entries()
        while (jarEntries.hasMoreElements()){
            JarEntry jarEntry=jarEntries.nextElement()
            if (jarEntry.directory){
                continue
            }
            String entryName=jarEntry.getName()
            String outFileName=dest.absolutePath+"/"+entryName
            File outFile=new File(outFileName)
            outFile.parentFile.mkdirs()
            InputStream inputStream=jarFile.getInputStream(jarEntry)
            FileOutputStream fileOutputStream=new FileOutputStream(outFile)
            fileOutputStream << inputStream
            fileOutputStream.close()
        }
        jarFile.close()


    }

    /**
     * 创建文件目录
     * @param dir
     * @param path
     * @return
     */
    static File touchFile(File dir,String path){
        File file=new File("${dir}/${path}")
        file.getParentFile().mkdirs()
        return file
    }

    /**
     * 压缩成jar包
     * @param jarDir
     * @param dest
     */
    static void zipJar(File jarDir,File dest){
        JarOutputStream outputStream=new JarOutputStream(new FileOutputStream(dest))
        //递归循环目录里的文件然后添加到jar包里面
        jarDir.eachFileRecurse {
            f->
                if (!f.isDirectory()){
                    //获取文件名称
                    String entryName=f.absolutePath.substring(jarDir.absolutePath.length()+1)
                    outputStream.putNextEntry(new ZipEntry(entryName))
                    InputStream inputStream=new FileInputStream(f)
                    outputStream<<inputStream
                    inputStream.close()
                }
        }
        outputStream.close()

    }
}