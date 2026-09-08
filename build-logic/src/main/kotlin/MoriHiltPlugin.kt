import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class MoriHiltPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // NOTE: modules must also apply `ksp` and `hilt` via alias in their own
            // plugins block; convention plugins cannot resolve external plugin ids.
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            dependencies {
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-android-compiler").get())
            }
        }
    }
}
