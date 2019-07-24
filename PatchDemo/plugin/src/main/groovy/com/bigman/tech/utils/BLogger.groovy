package  com.bigman.tech.utils
import com.android.build.gradle.api.BaseVariant

class BLogger{
    private static String TAG

    static void init(BaseVariant variant){
        //构建变体的名称
        TAG=">PatchPlugin-${variant.name.capitalize()}"
    }

    static void i(String log){
        println("${TAG}:${log}")
    }
}