package conan.ci.step


import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource

class withEnvBugSpec extends DeclarativePipelineTest {
    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        def library = library().name('demo_jenkins_library')
                .defaultVersion('<notNeeded>')
                .allowOverride(true)
                .implicit(true)
                .targetPath('<notNeeded>')
                .retriever(projectSource())
                .build()
        helper.registerSharedLibrary(library)
        helper.registerAllowedMethod("echo", [String.class], { String s -> println(s) })
    };

    @BeforeEach
    void jpuJunit4Setup(){
        setUp()
    }

    @Test
    void withEnvVariablesLinger() throws Exception {
        String stem = this.getClass().getCanonicalName().replace(".", "/")
        File jenkinsfile = new File("test/${stem}.jenkins")
        runScript(jenkinsfile.path)
        printCallStack()
    }
}
