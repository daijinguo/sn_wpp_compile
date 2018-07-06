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

    private static final String TAG = CompilePlugin.class.getSimpleName()

    private static final String SEP = File.separator
    private static final String APPLICATION = "com.android.application"
    private static final String DEP_TAG = "onlyCompile"

    private Project project
    private Logger LOG


    @Override
    void apply(Project project) {
        this.project = project
        LOG = project.logger

        LOG.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
        LOG.warn("author: patrick.dai")
        LOG.warn("class : ${CompilePlugin.class.getName()}")
        LOG.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")

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


    @SuppressWarnings("UnnecessaryQualifiedReference")
    void onlyCompileCompat() {
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

        Configuration onlyCompileConfig = project.getConfigurations().create(DEP_TAG)

        String version = getAndroidGradlePluginVersionCompat()
        LOG.warn(">>> this android gradle plugin version: ${version}")

        if (version.startsWith("1.3") ||
                version.startsWith("1.5") ||
                version.startsWith("2.") ||
                version.startsWith("3.")) {
            // 大于等于 1.3.0 版本让 provided 继承 onlyCompile
            //    低于  1.3.0 版本 手动提取 aar 中的jar添加依赖
            if (compileOnlyConfig != null) {
                compileOnlyConfig.extendsFrom(onlyCompileConfig)
            } else {
                providedConfig.extendsFrom(onlyCompileConfig)
            }
        }

        if (version.startsWith("0.") ||
                version.startsWith("1.0") ||
                version.startsWith("1.1") ||
                version.startsWith("1.2")) {
            //不支持小于1.3.0的版本(不含1.3.0)
            throw new GradleException("Not support version ${version}, android gradle plugin must >=1.3.0")
        }

        //
        // android gradle plugin version >= 1.3.0
        //
        def android = project.getExtensions().getByName("android")
        android.applicationVariants.all { def variant ->

            if (version.startsWith("1.3") ||
                    version.startsWith("1.5") ||
                    version.startsWith("2.0") ||
                    version.startsWith("2.1") ||
                    version.startsWith("2.2") ||
                    version.startsWith("2.3") ||
                    version.startsWith("2.4")) {
                // maybe i do not care about that, because most use large than 3.0
                LOG.warn(">>> current plugins version at[1.3, 1.5, 2.0, 2.1, 2.2, 2.3, 2.4]")

                //支持1.3.0+ ~ 2.4.0+，且低于2.5.0，支持传递依赖
                def prepareDependenciesTask =
                        project.tasks.findByName("prepare${variant.getName().capitalize()}Dependencies")
                if (prepareDependenciesTask) {
                    def removeSyncIssues = {
                        try {
                            Class prepareDepTaskClass =
                                    Class.forName("com.android.build.gradle.internal.tasks.PrepareDependenciesTask")
                            Field checkersField = prepareDepTaskClass.getDeclaredField('checkers')
                            checkersField.setAccessible(true)
                            def checkers = checkersField.get(prepareDependenciesTask)
                            checkers.iterator().with { checkersIterator ->
                                checkersIterator.each { dependencyChecker ->
                                    def syncIssues = dependencyChecker.syncIssues
                                    syncIssues.iterator().with { syncIssuesIterator ->
                                        syncIssuesIterator.each { syncIssue ->
                                            if (syncIssue.getType() == 7 && syncIssue.getSeverity() == 2) {
                                                project.logger.info(TAG, "[${DEP_TAG}] WARNING: ${DEP_TAG} has been enabled in 'com.android.application', ignore ${syncIssue}")
                                                syncIssuesIterator.remove()
                                            }
                                        }
                                    }

                                    // 兼容 1.3.0~2.1.3 版本
                                    // 为了将 provided 的 aar 不参与打包
                                    //    将 isOptional 设为 true
                                    if (version.startsWith("1.3") ||
                                            version.startsWith("1.5") ||
                                            version.startsWith("2.0") ||
                                            version.startsWith("2.1")) {
                                        def configurationDependencies = dependencyChecker.configurationDependencies
                                        List libraries = configurationDependencies.libraries
                                        libraries.each { library ->
                                            onlyCompileConfig.dependencies.each { providedDependency ->
                                                String libName = library.getName()
                                                if (libName.contains(providedDependency.group) &&
                                                        libName.contains(providedDependency.name) &&
                                                        libName.contains(providedDependency.version)) {
                                                    Field isOptionalField = library.getClass().getDeclaredField("isOptional")
                                                    Field modifiersField = Field.class.getDeclaredField("modifiers")
                                                    modifiersField.setAccessible(true)
                                                    modifiersField.setInt(isOptionalField,
                                                            isOptionalField.getModifiers() & ~java.lang.reflect.Modifier.FINAL)
                                                    isOptionalField.setAccessible(true)
                                                    isOptionalField.setBoolean(library, true)
                                                    // 为了递归调用可以引用，先声明再赋值
                                                    def fixDependencies = null
                                                    fixDependencies = { dependencies ->
                                                        dependencies.each { dependency ->
                                                            if (dependency.getClass() == library.getClass()) {
                                                                isOptionalField.setBoolean(dependency, true)
                                                                fixDependencies(dependency.dependencies)
                                                            }
                                                        }
                                                    }
                                                    fixDependencies(library.dependencies)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace()
                        }
                    }

                    if (version.startsWith("1.3") ||
                            version.startsWith("1.5") ||
                            version.startsWith("2.0") ||
                            version.startsWith("2.1")) {
                        // 这里不处理 as sync 的时候会出错
                        def appPlugin = project.getPlugins().findPlugin("com.android.application")
                        def taskManager = appPlugin.getMetaClass().getProperty(appPlugin, "taskManager")
                        def dependencyManager = taskManager.getClass().getSuperclass().getMetaClass().getProperty(taskManager, "dependencyManager")
                        def extraModelInfo = dependencyManager.getMetaClass().getProperty(dependencyManager, "extraModelInfo")
                        Map<?, ?> syncIssues = extraModelInfo.getSyncIssues()
                        syncIssues.iterator().with { syncIssuesIterator ->
                            syncIssuesIterator.each { syncIssuePair ->
                                if (syncIssuePair.getValue().getType() == 7 && syncIssuePair.getValue().getSeverity() == 2) {
                                    syncIssuesIterator.remove()
                                }
                            }
                        }
                        //下面同2.2.0+处理
                        prepareDependenciesTask.configure removeSyncIssues

                    } else if (version.startsWith("2.2") || version.startsWith("2.3")) {
                        prepareDependenciesTask.configure removeSyncIssues

                    } else if (version.startsWith("2.4")) {
                        prepareDependenciesTask.doFirst removeSyncIssues

                    }
                }

            }
            //
            // android gradle plugin 2.5 or 3.
            //
            else if (version.startsWith("2.5") || version.startsWith("3.")) {
                LOG.warn(">>> android gradle plugins version at[2.5, 3.]")

                //支持2.5.0+ ~ 3.2.0+，支持传递依赖
                def prepareBuildTask = project.tasks.findByName("pre${variant.getName().capitalize()}Build")
                if (prepareBuildTask) {
                    boolean needRedirectAction = false
                    prepareBuildTask.actions.iterator().with { actionsIterator ->
                        actionsIterator.each { action ->
                            if (action.getActionClassName().contains("AppPreBuildTask")) {
                                actionsIterator.remove()
                                needRedirectAction = true
                            }
                        }
                    }
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
                                try {
                                    //2.5.0+
                                    Field variantScopeField = appPreBuildTaskClass.getDeclaredField("variantScope")
                                    variantScopeField.setAccessible(true)
                                    def variantScope = variantScopeField.get(prepareBuildTask)
                                    //noinspection UnnecessaryQualifiedReference
                                    compileManifests = variantScope.getArtifactCollection(
                                            com.android.build.gradle.internal.publishing.AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH,
                                            com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactScope.ALL,
                                            com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.MANIFEST
                                    )

                                    runtimeManifests = variantScope.getArtifactCollection(
                                            com.android.build.gradle.internal.publishing.AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                                            com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactScope.ALL,
                                            com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.MANIFEST
                                    )

                                } catch (Exception e1) {
                                }
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
                                        project.getLogger().warn(
                                                "Unknown ComponentIdentifier type: " + id.getClass().getCanonicalName())
                                    }
                                }

                                runtimeArtifacts.each { def artifact ->
                                    def runtimeId = artifact.getId().getComponentIdentifier()
                                    def putMap = { def key, def value ->
                                        runtimeIds.put(key, value)
                                    }
                                    handleArtifact(runtimeId, putMap)
                                }

                                compileArtifacts.each { def artifact ->
                                    final ComponentIdentifier compileId = artifact.getId().getComponentIdentifier()
                                    def checkCompile = { def key, def value ->
                                        String runtimeVersion = runtimeIds.get(key)
                                        if (runtimeVersion == null) {
                                            String display = compileId.getDisplayName()
                                            LOG.info(
                                                    "WARNING: ${DEP_TAG} has been enabled in '${APPLICATION}' you can ignore 'Android dependency '"
                                                            + display
                                                            + "' is set to compileOnly/provided which is not supported'")
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
            //
            // android gradle plugin version large than 3
            //
            else {
                throw new GradleException("We can not support android gradle plugin version > 3")
            }

        }

        // redirect warning log to info log
        def listenerBackedLoggerContext = project.getLogger().getMetaClass().getProperty(project.getLogger(), "context")
        def originalOutputEventListener = listenerBackedLoggerContext.getOutputEventListener()
        def originalOutputEventLevel = listenerBackedLoggerContext.getLevel()
        listenerBackedLoggerContext.setOutputEventListener({ def outputEvent ->
            def logLevel = originalOutputEventLevel.name()
            if (!("QUIET".equalsIgnoreCase(logLevel) || "ERROR".equalsIgnoreCase(logLevel))) {
                if ("WARN".equalsIgnoreCase(outputEvent.getLogLevel().name())) {
                    String message = outputEvent.getMessage()
                    //Provided dependencies can only be jars.
                    //provided dependencies can only be jars.
                    if (message != null && (message.contains("Provided dependencies can only be jars.") ||
                            message.contains("provided dependencies can only be jars. "))) {
                        project.logger.info(message)
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
    File rFileDirCompat(def processAndroidResourceTask) {
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

