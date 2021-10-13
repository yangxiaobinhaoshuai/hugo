package hugo.weaving.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class HugoPlugin implements Plugin<Project> {
  @Override void apply(Project project) {
    def hasApp = project.plugins.withType(AppPlugin)
    def hasLib = project.plugins.withType(LibraryPlugin)
    if (!hasApp && !hasLib) {
      throw new IllegalStateException("'android' or 'android-library' plugin required.")
    }

    final def log = project.logger
    final def variants
    if (hasApp) {
      variants = project.android.applicationVariants
    } else {
      variants = project.android.libraryVariants
    }

    project.dependencies {
      debugCompile 'com.jakewharton.hugo:hugo-runtime:1.2.1'
      // TODO this should come transitively
      debugCompile 'org.aspectj:aspectjrt:1.8.6'
      compile 'com.jakewharton.hugo:hugo-annotations:1.2.1'
    }

    project.extensions.create('hugo', HugoExtension)

    variants.all { variant ->
      if (!variant.buildType.isDebuggable()) {
        log.debug("Skipping non-debuggable build type '${variant.buildType.name}'.")
        printLog("Skipping non-debuggable build type '${variant.buildType.name}'.")
        return;
      } else if (!project.hugo.enabled) {
        log.debug("Hugo is not disabled.")
        return;
      }

      JavaCompile javaCompile = variant.javaCompile

      //ajc args: [
      // -showWeaveInfo,
      // -1.5,
      // -inpath,
      //   /Users/yangxiaobin/DevelopSpace/IDEA/hugo/hugo-example/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes,

      // -aspectpath,
      //    /Users/yangxiaobin/.gradle/caches/transforms-1/files-1.1/hugo-runtime-1.2.1.aar/a5ace53027ccfdad59bde16452f2b386/jars/classes.jar
      //   :/Users/yangxiaobin/.gradle/caches/modules-2/files-2.1/com.jakewharton.hugo/hugo-annotations/1.2.1/b01150795c5cdca1eb7e501bf00f105ff0e31501/hugo-annotations-1.2.1.jar
      //   :/Users/yangxiaobin/.gradle/caches/modules-2/files-2.1/org.aspectj/aspectjrt/1.8.6/a7db7ea5f7bb18a1cbd9f24edd0e666504800be/aspectjrt-1.8.6.jar,
      //
      // -d,
      //    /Users/yangxiaobin/DevelopSpace/IDEA/hugo/hugo-example/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes,
      //
      // -classpath,
      //    /Users/yangxiaobin/.gradle/caches/transforms-1/files-1.1/hugo-runtime-1.2.1.aar/a5ace53027ccfdad59bde16452f2b386/jars/classes.jar
      //    :/Users/yangxiaobin/.gradle/caches/modules-2/files-2.1/com.jakewharton.hugo/hugo-annotations/1.2.1/b01150795c5cdca1eb7e501bf00f105ff0e31501/hugo-annotations-1.2.1.jar
      //    :/Users/yangxiaobin/.gradle/caches/modules-2/files-2.1/org.aspectj/aspectjrt/1.8.6/a7db7ea5f7bb18a1cbd9f24edd0e666504800be/aspectjrt-1.8.6.jar,
      //
      // -bootclasspath, /Users/yangxiaobin/Library/Android/sdk/platforms/android-29/android.jar]
      javaCompile.doLast {
        String[] args = [
            "-showWeaveInfo",
            "-1.5",
            "-progress",
            "-inpath", javaCompile.destinationDir.toString(),
            "-aspectpath", javaCompile.classpath.asPath,
            "-d", javaCompile.destinationDir.toString(),
            "-classpath", javaCompile.classpath.asPath,
            "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)
        ]
        log.debug "ajc args: " + Arrays.toString(args)
        printLog("ajc args: " + Arrays.toString(args))

        MessageHandler handler = new MessageHandler(true);
        new Main().run(args, handler);
        for (IMessage message : handler.getMessages(null, true)) {
          switch (message.getKind()) {
            case IMessage.ABORT:
            case IMessage.ERROR:
            case IMessage.FAIL:
              log.error message.message, message.thrown
              printLog(message.message)
              break;
            case IMessage.WARNING:
              log.warn message.message, message.thrown
              printLog(message.message)
              break;
            case IMessage.INFO:
              log.info message.message, message.thrown
              printLog(message.message)
              break;
            case IMessage.DEBUG:
              log.debug message.message, message.thrown
              printLog(message.message)
              break;
          }
        }
      }
    }
  }


  public void printLog(String msg){
    println "===> $msg"
  }
}
