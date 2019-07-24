package com.bigman.tech

import org.gradle.api.Project

class BigmanExtension{

    HashSet<String> includePackage=[]
    HashSet<String> excludeClass=[]

    //调试模式开启补丁模式
    boolean debugOn=true

    //是否签名
    boolean sign=false
    File storeFile=null
    String storePassword=''
    String keyAlias=''
    String keyPassword=''

    BigmanExtension(Project project) {
    }
}