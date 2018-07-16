package dai.android.gradle.plugin.wpp.compile

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.logging.Logger
import org.gradle.util.GFileUtils

import java.lang.reflect.Field


//
// this project module almost like or is follow git code
//  https://github.com/lizhangqu/AndroidGradlePluginCompat
//

class CompilePlugin implements Plugin<Project> {
    private static final String VER = "1.0.3"
    private static final String SEP = File.separator
    private static final String APPLICATION = "com.android.application"
    private static final String DEP_TAG = "onlyCompile"

    private Project project
    private Logger LOG


    @Override
    void apply(Project project) {
        this.project = project
        LOG = project.logger

        project.ext.Aapt2Enabled = this.&canAapt2EnabledCompat
        project.ext.Aapt2JniEnabled = this.&canAapt2JniEnabledCompat
        project.ext.Aapt2DaemonModeEnabled = this.&canAapt2DaemonModeEnabledCompat
        project.ext.AndroidGradlePluginVersion = this.&getAndroidGradlePluginVersionCompat
        project.ext.WorkAtJenkins = this.&workAtJenkins
        project.ext.OnlyCompile = this.&onlyCompileCompat

        project.ext.getPublishApFileCompat = this.&getPublishApFileCompat
        project.ext.getRDirCompat = this.&getRDirCompat
        project.ext.getRFileCompat = this.&getRFileCompat
        project.ext.getPackageForRCompatCompat = this.&getPackageForRCompatCompat

        LOG.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
        LOG.warn("author : patrick.dai")
        LOG.warn("address: https://github.com/daijinguo/sn_wpp_compile")
        LOG.warn("class  : ${CompilePlugin.class.getName()}")
        LOG.warn("version: ${VER}")
        LOG.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
        LOG.warn("enable aapt2: ${canAapt2EnabledCompat()}")
        LOG.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
    }

    static <T> T resolveEnumValue(String value, Class<T> type) {
        for (T constant : type.getEnumConstants()) {
            if (constant.toString().equalsIgnoreCase(value)) {
                return constant
            }
        }
        return null
    }


    def getProjectOptions() {
        Class classProjectOptions = Class.forName("com.android.build.gradle.options.ProjectOptions")
        def constructor = classProjectOptions.getDeclaredConstructor(Project.class)
        constructor.setAccessible(true)
        def projectOptions = constructor.newInstance(project)
        return projectOptions
    }

    /**
     * 导出 aapt2 是否开启的兼容方法
     * 在 build.gradle 中 apply 后可直接使用 isAapt2EnabledCompat()
     */
    boolean canAapt2EnabledCompat() {
        boolean aapt2Enabled = false
        try {
            def projectOptions = getProjectOptions()
            Object enumValue = resolveEnumValue("ENABLE_AAPT2",
                    Class.forName("com.android.build.gradle.options.BooleanOption"))
            aapt2Enabled = projectOptions.get(enumValue)
        } catch (Exception e) {
            try {
                // function at gradle 2.3.3
                Class classAndroidGradleOptions =
                        Class.forName("com.android.build.gradle.AndroidGradleOptions")

                def isAapt2Enabled =
                        classAndroidGradleOptions.getDeclaredMethod(
                                "isAapt2Enabled",
                                Project.class)

                isAapt2Enabled.setAccessible(true)
                aapt2Enabled = isAapt2Enabled.invoke(null, project)
            } catch (Exception e1) {
                aapt2Enabled = false
            }
        }
        return aapt2Enabled
    }

    /**
     * 导出 aapt2 jni 和 aapt2 daemon mode 是否被废弃
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    boolean isAapt2JniAndAapt2DaemonModeDeprecated() {
        try {
            def aapt2JniEnumValue = resolveEnumValue(
                    "ENABLE_IN_PROCESS_AAPT2",
                    Class.forName("com.android.build.gradle.options.DeprecatedOptions")
            )

            def aapt2DaemonModeEnumValue = resolveEnumValue(
                    "ENABLE_DAEMON_MODE_AAPT2",
                    Class.forName("com.android.build.gradle.options.DeprecatedOptions")
            )

            return aapt2JniEnumValue != null && aapt2DaemonModeEnumValue != null

        } catch (Exception e) {
        }

        return false
    }

    /**
     * 导出 aapt2 Jni 是否开启的兼容方法
     * 在 build.gradle 中 apply 后可直接使用 isAapt2JniEnabledCompat()
     */
    boolean canAapt2JniEnabledCompat() {
        boolean aapt2JniEnabled = false
        if (isAapt2EnabledCompat()) {
            try {
                def projectOptions = getProjectOptions()
                def enumValue = resolveEnumValue(
                        "ENABLE_IN_PROCESS_AAPT2",
                        Class.forName("com.android.build.gradle.options.BooleanOption")
                )

                aapt2JniEnabled = projectOptions.get(enumValue)
                if (isAapt2JniAndAapt2DaemonModeDeprecated()) {
                    aapt2JniEnabled = false
                }

            } catch (Exception e) {
                aapt2JniEnabled = false
            }
        }

        return aapt2JniEnabled
    }

    /**
     * 导出 aapt2 Daemon Mode 是否开启的兼容方法
     * 在 build.gradle 中 apply 后可直接使用 isAapt2DaemonModeEnabledCompat()
     */
    boolean canAapt2DaemonModeEnabledCompat() {
        boolean aapt2DaemonEnabled = false
        if (isAapt2EnabledCompat()) {
            try {
                def projectOptions = getProjectOptions()
                def enumValue = resolveEnumValue(
                        "ENABLE_DAEMON_MODE_AAPT2",
                        Class.forName("com.android.build.gradle.options.BooleanOption")
                )

                aapt2DaemonEnabled = projectOptions.get(enumValue)
                if (isAapt2JniAndAapt2DaemonModeDeprecated()) {
                    aapt2DaemonEnabled = false
                }

            } catch (Exception e) {
                aapt2DaemonEnabled = false
            }
        }
        return aapt2DaemonEnabled
    }

    /**
     * 导出获得 android gradle plugin 插件的版本号
     * 在 build.gradle 中 apply 后可直接使用 getAndroidGradlePluginVersionCompat()
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    String getAndroidGradlePluginVersionCompat() {
        String version = null
        try {
            Class versionModel = Class.forName("com.android.builder.model.Version")
            def versionFiled = versionModel.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
            versionFiled.setAccessible(true)
            version = versionFiled.get(null)
        } catch (Exception e) {
            version = "unknown"
        }
        return version
    }

    /**
     * 导出是否在 jenkins 环境中
     * 在 build.gradle 文件 apply 后可直接使用 isJenkins()
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    boolean workAtJenkins() {
        Map<String, String> environmentMap = System.getenv()
        boolean result = false
        if (environmentMap != null &&
                environmentMap.containsKey("JOB_NAME") &&
                environmentMap.containsKey("BUILD_NUMBER")) {
            result = true
        }
        return result
    }

    // the follow used by function onlyCompileCompat
    private static final String githubSide = "https://github.com/daijinguo/sn_wpp_compile"

    @SuppressWarnings("UnnecessaryQualifiedReference")
    void onlyCompileCompat() {
        LOG.warn("")

        if (!project.getPlugins().hasPlugin(APPLICATION)) {
            LOG.warn(">>> not '${APPLICATION}'")
            return
        }

        if (project.getConfigurations().findByName(DEP_TAG) != null) {
            LOG.warn(">>> config are '${DEP_TAG}' do nothing")
            return
        }

        Configuration providedConfig = project.getConfigurations().findByName("provided")
        Configuration compileOnlyConfig = project.getConfigurations().findByName("compileOnly")
        if (providedConfig == null && compileOnlyConfig == null) {
            //System.out.println(">>> null provided and compileOnly happen")
            LOG.warn(">>> this not 'provided' and 'compileOnly'")
            return
        }

        String version = getAndroidGradlePluginVersionCompat()
        LOG.warn(">>> android gradle plugin version string: '${version}'")

        // r : 主版本号
        // x : 次版本号
        // y : 修订版本号
        String[] versions = version.split("\\.")
        int r = -1
        int x = -1
        int y = -1
        if (versions.length == 0) {
            throw new GradleException("android gradle plugin version not split with '.', please visit: ${githubSide}")
        }

        try {
            if (versions.length == 1) {
                r = Integer.parseInt(versions[0])
            } else if (versions.length >= 2) {
                r = Integer.parseInt(versions[0])
                x = Integer.parseInt(versions[1])
            }

        } catch (NumberFormatException e) {
            throw new GradleException("android gradle plugin version not a number, please visit: ${githubSide}")
        }

        if (versions.length >= 3) {
            try {
                y = Integer.parseInt(versions[2])
            } catch (NumberFormatException e) {
                y = -1
            }
        }

        LOG.warn(">>> version number: { R.X.Y } = { ${r}.${x}.${y} }")

        if (r < 3) {
            throw new GradleException("We not support gradle plugin version less 3.0")
        }

        if (r >= 4) {
            throw new GradleException("We not support gradle plugin version large 4.0")
        }

        Configuration onlyCompileConfig = project.getConfigurations().create(DEP_TAG)

        // 大于等于 1.3.0 版本让 provided 继承 onlyCompile
        //    低于  1.3.0 版本 手动提取 aar 中的jar添加依赖
        if (compileOnlyConfig != null) {
            compileOnlyConfig.extendsFrom(onlyCompileConfig)
        } else {
            providedConfig.extendsFrom(onlyCompileConfig)
        }

        //
        // android gradle plugin version >= 1.3.0
        //
        def android = project.getExtensions().getByName("android")
        android.applicationVariants.all { def variant ->

            String name = "pre${variant.getName().capitalize()}Build"
            ///LOG.warn(">>> +: task name = ${name}")

            // 支持 2.5.0+ ~ 3.2.0+，支持传递依赖
            def prepareBuildTask = project.tasks.findByName(name)
            ///LOG.warn("\n>>> +: has prepare build: ${prepareBuildTask}")

            if (prepareBuildTask) {
                boolean needRedirectAction = false
                prepareBuildTask.actions.iterator().with { actionsIterator ->
                    actionsIterator.each { action ->
                        String actionClass = action.getActionClassName()
                        ///LOG.warn(">>> +++: action class name: ${actionClass}")
                        if (actionClass.contains("AppPreBuildTask")) {
                            actionsIterator.remove()
                            needRedirectAction = true
                        }
                    }
                }
                ///LOG.warn(">>> ++: need to redirect action? ${needRedirectAction}")

                if (needRedirectAction) {
                    prepareBuildTask.doLast {
                        def compileManifests = null
                        def runtimeManifests = null
                        Class appPreBuildTaskClass = Class.forName("com.android.build.gradle.internal.tasks.AppPreBuildTask")
                        try {
                            //3.0.0+
                            Field compileManifestsField = appPreBuildTaskClass.getDeclaredField("compileManifests")
                            Field runtimeManifestsField = appPreBuildTaskClass.getDeclaredField("runtimeManifests")
                            compileManifestsField.setAccessible(true)
                            runtimeManifestsField.setAccessible(true)
                            compileManifests = compileManifestsField.get(prepareBuildTask)
                            runtimeManifests = runtimeManifestsField.get(prepareBuildTask)

                        } catch (Exception e) {
                            // some failed on this version
                            LOG.error("Some exception on current ${DEP_TAG}")
                        }

                        try {
                            Set<ResolvedArtifactResult> compileArtifacts = compileManifests.getArtifacts()
                            Set<ResolvedArtifactResult> runtimeArtifacts = runtimeManifests.getArtifacts()

                            Map<String, String> runtimeIds = new HashMap<>(runtimeArtifacts.size())

                            def handleArtifact = { id, consumer ->
                                if (id instanceof ProjectComponentIdentifier) {
                                    consumer(((ProjectComponentIdentifier) id).getProjectPath().intern(), "")
                                } else if (id instanceof ModuleComponentIdentifier) {
                                    ModuleComponentIdentifier moduleComponentId = (ModuleComponentIdentifier) id
                                    consumer(
                                            moduleComponentId.getGroup() + ":" + moduleComponentId.getModule(),
                                            moduleComponentId.getVersion())
                                } else {
                                    LOG.warn("Unknown ComponentIdentifier type: ${id.getClass().getCanonicalName()}")
                                }
                            }

                            runtimeArtifacts.each { def artifact ->
                                def runtimeId = artifact.getId().getComponentIdentifier()
                                def putMap = { def key, def value ->
                                    runtimeIds.put(key, value)
                                }
                                handleArtifact(runtimeId, putMap)
                            }

                            // this debug for show all runtimeIds
                            runtimeIds.each {
                                LOG.warn(">>> +++: { ${it.key}, ${it.value} }")
                            }

                            compileArtifacts.each { def artifact ->
                                final ComponentIdentifier compileId = artifact.getId().getComponentIdentifier()
                                def checkCompile = { def key, def value ->
                                    String runtimeVersion = runtimeIds.get(key)
                                    if (runtimeVersion == null) {
                                        String display = compileId.getDisplayName()
                                        LOG.info("WARNING: ${DEP_TAG} has been enabled in '${APPLICATION}' you can ignore 'Android dependency '"
                                                + display + "' is set to compileOnly/provided which is not supported'")
                                    } else if (!runtimeVersion.isEmpty()) {
                                        // compare versions.
                                        if (runtimeVersion != value) {
                                            throw new RuntimeException(
                                                    String.format(
                                                            "Android dependency '%s' has different version for the compile (%s) and runtime (%s) classpath. You should manually set the same version via DependencyResolution",
                                                            key,
                                                            value,
                                                            runtimeVersion
                                                    )
                                            )
                                        }
                                    }
                                }
                                handleArtifact(compileId, checkCompile)
                            }

                        } catch (Exception e) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // redirect warning log to info log
        def listenerBackedLoggerContext = LOG.getMetaClass().getProperty(LOG, "context")
        def originalOutputEventListener = listenerBackedLoggerContext.getOutputEventListener()
        def originalOutputEventLevel = listenerBackedLoggerContext.getLevel()
        listenerBackedLoggerContext.setOutputEventListener({ def outputEvent ->
            def logLevel = originalOutputEventLevel.name()
            if (!("QUIET".equalsIgnoreCase(logLevel) || "ERROR".equalsIgnoreCase(logLevel))) {
                if ("WARN".equalsIgnoreCase(outputEvent.getLogLevel().name())) {
                    String message = outputEvent.getMessage()
                    //Provided dependencies can only be jars.
                    //provided dependencies can only be jars.
                    if (message != null &&
                            (message.contains("Provided dependencies can only be jars.") ||
                                    message.contains("provided dependencies can only be jars. "))
                    ) {
                        LOG.info(message)
                        return
                    }
                }
            }
            if (originalOutputEventListener != null) {
                originalOutputEventListener.onOutput(outputEvent)
            }
        })
    }

    /**
     * R.java 包名兼容获取
     */
    String packageForRCompat(def processAndroidResourceTask) {
        if (processAndroidResourceTask == null) {
            return null
        }
        String packageForR = null
        try {
            packageForR = processAndroidResourceTask.getPackageForR()
        } catch (Exception e) {
            project.logger.info(e.getMessage())
        }
        if (packageForR == null) {
            try {
                packageForR = processAndroidResourceTask.getOriginalApplicationId()
            } catch (Exception e) {
                project.logger.info(e.getMessage())
            }
        }
        return packageForR
    }
    /**
     * R.java 输出路径兼容获取
     */
    static File rFileDirCompat(def processAndroidResourceTask) {
        if (processAndroidResourceTask == null) {
            return null
        }
        File rFileDir = processAndroidResourceTask.getSourceOutputDir()
        GFileUtils.mkdirs(rFileDir)
        return rFileDir
    }

    /**
     * resources.ap_ 输出路径兼容获取
     */
    File apFileCompat(def processAndroidResourceTask) {
        if (processAndroidResourceTask == null) {
            return null
        }
        File apFile = null
        try {
            apFile = processAndroidResourceTask.getPackageOutputFile()
        } catch (Exception e) {
            project.logger.info(e.getMessage())
        }

        String variantName = null
        if (apFile == null) {
            try {
                variantName = processAndroidResourceTask
                        .getMetaClass()
                        .getMetaProperty("variantName")
                        .getProperty(processAndroidResourceTask)
                File resPackageOutputFolder = processAndroidResourceTask.getResPackageOutputFolder()
                apFile = new File(resPackageOutputFolder, "resources" + "-" + variantName + ".ap_")

            } catch (Exception e) {
                project.logger.info(e.getMessage())
            }
        }

        if (apFile == null) {
            apFile = project.file(
                    "build${SEP}intermediates${SEP}res${SEP}resources"
                            + "-" + variantName + ".ap_")
        }

        return apFile
    }

    /**
     * 获取 publish ap 的函数
     */
    File getPublishApFileCompat(String variantName) {
        if (variantName == null || variantName.length() == 0) {
            // throw new GradleException("variantName 不能为空，且必须是驼峰形式")
            throw new GradleException("variantName must not null")
        }

        def processAndroidResourceTask =
                project.tasks.findByName("process${variantName.capitalize()}Resources")
        File originalApFile = apFileCompat(processAndroidResourceTask)
        if (originalApFile == null) {
            String androidGradlePluginVersion = getAndroidGradlePluginVersionCompat()
            if (androidGradlePluginVersion.startsWith("0.") ||
                    androidGradlePluginVersion.startsWith("1.") ||
                    androidGradlePluginVersion.startsWith("2.")) {
                // 驼峰转换为-分隔
                int length = variantName.length()
                StringBuilder sb = new StringBuilder(length)
                for (int i = 0; i < length; i++) {
                    char c = variantName.charAt(i)
                    if (Character.isUpperCase(c)) {
                        sb.append("-")
                        sb.append(Character.toLowerCase(c))
                    } else {
                        sb.append(c)
                    }
                }
                return project.file("build${SEP}intermediates${SEP}res${SEP}resources-${sb}.ap_")

            } else {
                //驼峰转换为对应的目录和文件名
                int length = variantName.length()
                StringBuilder dirName = new StringBuilder(length)
                for (int i = 0; i < length; i++) {
                    char c = variantName.charAt(i)
                    if (Character.isUpperCase(c)) {
                        dirName.append(SEP)
                        dirName.append(Character.toLowerCase(c))
                    } else {
                        dirName.append(c)
                    }
                }
                return project.file("build${SEP}intermediates${SEP}res${SEP}${dirName}${SEP}resources-${variantName}.ap_")
            }
        }

        return originalApFile
    }

    /**
     * 暴露给外界获取 R.java 除去包名路径的函数，必须在 project.afterEvaluate 中调用，否则获取到的是null
     */
    File getRDirCompat(String variantName) {
        if (variantName == null || variantName.length() == 0) {
            throw new GradleException("variantName 不能为空，且必须是驼峰形式")
        }

        def processAndroidResourceTask =
                project.tasks.findByName("process${variantName.capitalize()}Resources")
        File rDir = rFileDirCompat(processAndroidResourceTask)
        if (rDir == null) {
            int length = variantName.length()
            StringBuilder dirName = new StringBuilder(length)
            for (int i = 0; i < length; i++) {
                char c = variantName.charAt(i)
                if (Character.isUpperCase(c)) {
                    dirName.append(SEP)
                    dirName.append(Character.toLowerCase(c))
                } else {
                    dirName.append(c)
                }
            }
            return project.file("build${SEP}generated${SEP}source${SEP}r${SEP}${dirName}")
        }
        return rDir
    }

    /**
     * 暴露给外界获取 R.java 路径的函数，必须在 project.afterEvaluate 中调用，否则获取到的是null
     */
    File getRFileCompat(String variantName) {
        if (variantName == null || variantName.length() == 0) {
            throw new GradleException("variantName 不能为空，且必须是驼峰形式")
        }
        File rDir = getRDirCompat(variantName)
        String packageForR = getPackageForRCompatCompat(variantName)
        return new File(new File(rDir, packageForR.replaceAll("\\.", File.separator)), "R.java")
    }

    /**
     * 暴露给外界获取 R.java 包名的函数，必须在 project.afterEvaluate 中调用，否则获取到的是 null
     */
    String getPackageForRCompatCompat(String variantName) {
        if (variantName == null || variantName.length() == 0) {
            throw new GradleException("variantName 不能为空，且必须是驼峰形式")
        }
        def processAndroidResourceTask =
                project.tasks.findByName("process${variantName.capitalize()}Resources")
        String packageForR = packageForRCompat(processAndroidResourceTask)
        if (packageForR == null) {
            return project.android.defaultConfig.applicationId
        }
        return packageForR
    }

}

