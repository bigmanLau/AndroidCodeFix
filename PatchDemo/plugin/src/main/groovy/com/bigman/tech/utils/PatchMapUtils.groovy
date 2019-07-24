package com.bigman.tech.utils
class PatchMapUtils{
    private static final String MAP_SEPARATOR = ":"

    /**
     * 从文件里解析出一个HashMap 键为类名 值为hash值
     * @param hashFile
     * @return
     */
    static Map parseMap(File hashFile){
        Map hashMap=[:]
        if (hashFile.exists()){
            hashFile.eachLine {
                List list=it.split(MAP_SEPARATOR)
                if (list.size()==2){
                    hashMap.put(list[0],list[1])
                }
            }
        }else{
            BLogger.i("类hash文件  $hashFile 不存在")
        }
        return hashMap
    }

    /**
     * 获取标准格式值  类名：hash值
     * @param name
     * @param hash
     * @return
     */
    static format(String name,String hash){
        return name+MAP_SEPARATOR+hash+"\n"
    }

    /**
     * 对比原来文件的hash值是否相同 如果不相同说明是补丁class需要打包
     * @param map
     * @param name
     * @param hash
     * @return
     */
    static boolean notSame(Map map,String name,String hash){
        boolean notSame=false
        if (map){
            String value=map.get(name)
            if (value){
                if (!value.equals(hash)){
                    notSame=true
                }
            }else{
                notSame=true
            }
        }
        return notSame
    }
}