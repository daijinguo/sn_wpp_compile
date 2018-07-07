package dai.android.gradle.plugin.wpp.override

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class OverridePlugin implements Plugin<Project> {

    private static final String VER = "1.1.0"
    private static final String githubSide = "https://github.com/daijinguo/sn_wpp_compile"

    private Project mProject
    private Logger mLogger


    @Override
    void apply(Project project) {
        mProject = project
        mLogger = project.logger

        mLogger.warn("++++++++++++++++++++++++++++++++++++++++++++++++++")
        mLogger.warn("author: patrick.dai")
        mLogger.warn("github: ${githubSide}")
        mLogger.warn("class : ${OverridePlugin.class.getName()}")
        mLogger.warn("++++++++++++++++++++++++++++++++++++++++++++++++++")

        def android = project.extensions.findByType(AppExtension)
        android.registerTransform(new OverrideTransform(project))
    }
}
