package cc.ewell.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class PluginImpl implements Plugin<Project> {
    void apply(Project project) {
        project.task('proguard',type: proguard.gradle.ProGuardTask) {
            def warName = project.bootWar.getArchiveFileName().get()
            def buildDir = project.getBuildDir()
            def classesRootdir = "${buildDir}/libs/WEB-INF/classes"
            def archivePath = "${classesRootdir}/proguard.jar"
            def warDir = "$buildDir/libs/${warName}"

            //不做up-to-date校验，强制在每次运行当前任务时都执行
            outputs.upToDateWhen { false }

            //跳过测试和spotbugs执行更快
            project.test.enabled = false
            project.spotbugsMain.enabled = false

            doFirst {
                //1 解压war
                File warFile = new File(warDir);
                if(warFile.exists()){
                    project.exec {
                        workingDir "$buildDir/libs"
                        commandLine 'jar',"-xf","${warName}"
                    }
                }else{
                    println "Can't find war"
                    return
                }

                //2 打jar包
                File file = new File(classesRootdir)

                if(file.isDirectory()){
                    project.exec {
                        workingDir classesRootdir
                        def command = 'bash'
                        def argss = '-c'
                        if (System.getProperty('os.name').toLowerCase(Locale.ROOT).contains('windows')) {
                            command = 'cmd'
                            argss = '/c'
                        }
                        executable command
                        //jar打包classes目录下所有文件作为injar
                        args argss, "jar -cfM $archivePath *"
                    }
                }else{
                    throw new RuntimeException("目录不存在：${classesRootdir}" )
                }
            }

            //混淆配置，在执行阶段执行
            configuration 'configuration.pro'

            def injarsDir = archivePath
            def outjarsDir = archivePath.replace(".jar", "_obf.jar")
            //3 执行任务最后，解压缩混淆好的到classes下
            doLast {

                def classesFile = new File("${buildDir}/libs/WEB-INF/classes")
                classesFile.listFiles().each {f ->
                    if(!f.name.endsWith(".jar")){
                        if(f.isDirectory()){
                            f.deleteDir()
                        }else{
                            f.delete()
                        }
                    }
                }

                //将混淆后的jar解压到原来的目录
                project.exec {
                    workingDir classesRootdir
                    commandLine 'jar',"-xf", outjarsDir
                }
                project.delete injarsDir
                project.delete outjarsDir

                project.delete "${buildDir}/libs/org"
                project.delete "${buildDir}/libs/${warName}"

                //重新打war包
                project.exec {
                    workingDir "$buildDir/libs"
                    def command = 'bash'
                    def argss = '-c'
                    if (System.getProperty('os.name').toLowerCase(Locale.ROOT).contains('windows')) {
                        command = 'cmd'
                        argss = '/c'
                    }
                    executable command
                    args argss, "jar -cfM ${buildDir}/libs/${warName} WEB-INF/* META-INF/*"
                }

                project.delete "${buildDir}/libs/WEB-INF"
                project.delete "${buildDir}/libs/META-INF"
            }
        }
        project.afterEvaluate {
            def proguard = project.tasks.proguard
            project.assemble.dependsOn proguard
        }
    }
}

