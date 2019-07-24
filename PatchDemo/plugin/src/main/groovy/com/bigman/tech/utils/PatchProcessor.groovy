package com.bigman.tech.utils

import com.android.SdkConstants
import com.bigman.tech.BigmanExtension

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.objectweb.asm.*


class PatchProcessor{
    /**
     * 处理jar包
     * @param jarFile
     * @param hashFile
     * @param hashMap
     * @param patchDir
     * @param extension
     * @return
     */
    static processJar(File jarFile,File hashFile,Map hashMap,File patchDir,BigmanExtension extension){
        if (shouldProcessJar(jarFile)){
            BLogger.i("处理Jar文件:${jarFile.absolutePath}")
            //jar解压后输出目录
            File optDirFile=new File(jarFile.absolutePath.substring(0,jarFile.absolutePath.length()-4))
            //解压jar包
            PatchFileUtils.unZipJar(jarFile,optDirFile)

            File metaInfoDir=new File(optDirFile,"META-INF")
            if(metaInfoDir.exists()){
                metaInfoDir.delete()
            }

            int counter=0
            optDirFile.eachFileRecurse {
                file->
                    if (file.isFile()){
                        boolean result=processClass(file,hashFile,hashMap,patchDir,extension)
                        if (result){
                            counter++
                        }
                    }
            }

            if(counter==0){
                //如果没发现修改了任何文件 删除该操作文件
                optDirFile.deleteDir()
                return
            }

            File optJar=new File(jarFile.parent,jarFile.name+".opt")
            //压缩为patch.jar
            PatchFileUtils.zipJar(optDirFile,optJar)
            //一下三步完成文件的替换
            jarFile.delete()
            optJar.renameTo(jarFile)
            optDirFile.deleteDir()


        }
    }

    /**
     * 判断是否需要处理jar文件
     * @param jarFile
     * @return
     */
    private static boolean shouldProcessJar(File jarFile){
        if (!jarFile.exists()||!jarFile.name.endsWith(SdkConstants.DOT_JAR)){
            return  false
        }
        String jarPath=PatchFileUtils.formatPath(jarFile.absolutePath)
        //只有在这个目录里面的jar文件才处理
        return jarPath.contains("/build/intermediates")
    }

    /**
     * 给目标class文件注入代码
     * @param classFile
     * @param hashFile
     * @param hashMap
     * @param patchDir
     * @param extension
     * @return
     */
    static boolean processClass(File classFile,File hashFile,Map hashMap,File patchDir,BigmanExtension extension){
        if (shouldProcessClass(classFile,extension)){
            //给目标class文件注入代码
           referHackWhenInit(classFile,hashFile,hashMap,patchDir)
            return true
        }
        return false
    }

    /**
     * 过滤一些不用打补丁的文件
     * @param classFile
     * @param extension
     * @return
     */
    private static boolean shouldProcessClass(File classFile,BigmanExtension extension){
        if (!classFile.exists()||!classFile.name.endsWith(SdkConstants.DOT_CLASS)){
            return false
        }

        FileInputStream inputStream=new FileInputStream(classFile)
        ClassReader cr=new ClassReader(inputStream)
        String className=cr.className
        inputStream.close()
        return !className.startsWith("com/bigman/tech/lib")&&
                !className.contains("android/support/")&&
                !className.contains("/R\$")&&
                !className.endsWith("/R")&&
                !className.endsWith("/BuildConfig")&&
                PatchSetUtils.isIncluded(className,extension.includePackage)&&
                !PatchSetUtils.isExcluded(className,extension.excludeClass)
    }

    /**
     * 注入一个Hack 类 让app里的所有类都依赖于它 解除IS_PRIVILEGE标志
     * @param classFile
     * @param hashFile
     * @param hashMap
     * @param patchDir
     */
    private static void referHackWhenInit(File classFile,File hashFile,Map hashMap,File patchDir){
        // 新建操作文件 要写入文件
        File optClass=new File(classFile.parent,classFile.name+".opt")
        FileInputStream inputStream=new FileInputStream(classFile)
        FileOutputStream outputStream=new FileOutputStream(optClass)

        ClassReader cr=new ClassReader(inputStream)
        String className=cr.className
        ClassWriter cw=new ClassWriter(cr,0)
   ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
       @Override
       MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
           MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
           mv = new MethodVisitor(Opcodes.ASM4, mv) {
               @Override
               void visitInsn(int opcode) {
                   if ("<init>".equals(name) && opcode == Opcodes.RETURN) {
                       super.visitLdcInsn(Type.getType("Lcom/example/administrator/patchdemo/Hack;"))
                   }
                   super.visitInsn(opcode)
               }
           }
           return mv
       }
   }
        cr.accept(cv, 0)

        outputStream.write(cw.toByteArray())
        inputStream.close()
        outputStream.close()

        //把原来的class文件删除了
        if (classFile.exists()){
            classFile.delete()
        }
        //把optclass文件赋值给原来的class文件
        optClass.renameTo(classFile)

        //最后把hash值保存一下
        FileInputStream is=new FileInputStream(classFile)
        String hash=DigestUtils.sha1Hex(is)
        is.close()

        //在原来的hash文件写入一行记录
        hashFile.append(PatchMapUtils.format(className,hash))

        //如果hash不同 判断为补丁文件 需要复制到补丁jar包
        if (PatchMapUtils.notSame(hashMap,className,hash)){
            //把文件复制到补丁目录下面
            FileUtils.copyFile(classFile,PatchFileUtils.touchFile(patchDir,className+SdkConstants.DOT_CLASS))
        }


    }
}