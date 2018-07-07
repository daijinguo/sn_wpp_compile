package dai.android.gradle.plugin.wpp.override

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class OverrideTransform extends Transform {

    private Project mProject
    private Logger mLogger

    OverrideTransform(Project target) {
        mProject = target
        mLogger = target.logger
    }

    @Override
    String getName() {
        return "wppClazzTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        Collection<TransformInput> inputs = transformInvocation.inputs
        Collection<TransformInput> referencedInputs = transformInvocation.referencedInputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider
        boolean isIncremental = transformInvocation.incremental

        mLogger.warn("\n>>> transform function at class OverrideTransform")

        // has two input type: directory and jar
        inputs.each { TransformInput input ->
            mLogger.warn("\n")

            // search input directory
            input.directoryInputs.each { DirectoryInput directoryInput ->
                String dirPath = directoryInput.file.absolutePath
                String name = directoryInput.name

                mLogger.warn(">>> +: name = ${name}")
                mLogger.warn(">>> +: path = ${dirPath}")
            }

            // search input jar
            input.jarInputs.each { JarInput jarInput ->
                String jarPath = jarInput.file.getAbsolutePath()
                String name = jarInput.name
                String md5Path = DigestUtils.md5Hex(jarPath)

                mLogger.warn(">>> +:     name = ${name}")
                mLogger.warn(">>> +:     path = ${jarPath}")
                mLogger.warn(">>> +: md5 path = ${md5Path}")

            }
        }
    }
}