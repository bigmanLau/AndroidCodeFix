package com.bigman.tech.utils
class PatchSetUtils{
    /**
     * 是否在配置包含的路径里面
     * @param path
     * @param includePackage
     * @return
     */
    static boolean isIncluded(String path,Set<String> includePackage){
        if (includePackage.size()==0){
            return true
        }
        boolean  isIncluded=false
        includePackage.each {
            include->
                if (path.startsWith(include)){
                    isIncluded=true
                }
        }
        return isIncluded
    }

    /**
     * 是否在配置排除的路径里面
     * @param path
     * @param excludeClasss
     * @return
     */
    static boolean isExcluded(String path,Set<String> excludeClasss){
        boolean isExcluded=false
        excludeClasss.each {
            exclude->
                if (path.endsWith(exclude)){
                    isExcluded=true
                }
        }
        return isExcluded
    }
}