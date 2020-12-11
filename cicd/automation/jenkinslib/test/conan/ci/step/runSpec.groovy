package conan.ci.step

import com.lesfurets.jenkins.unit.MethodCall
import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource
import static org.assertj.core.api.Assertions.assertThat

class runSpec extends DeclarativePipelineTest {
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
        helper.registerAllowedMethod("bat", [Map.class], { })
    };

    @BeforeEach
    void jpuJunit4Setup(){
        setUp()
    }

    //Tests both the native command runner and then the docker runner
    @Test
    void demoRun() throws Exception {
        String stem = this.getClass().getCanonicalName().replace(".", "/")
        File jenkinsfile = new File("test/${stem}.jenkins")

        runScript(jenkinsfile.path)

        /// Test Stages which run bat commands, first the native runner and docker runner
        List<String> expectedBatCommands = [
                "cd",
                "docker exec -w C:\\Users\\ContainerAdministrator --user conan JENKINS-LIBA-MASTER_10_test_docker_windows cmd -c 'cd'",
                "docker exec -w /home/conan --user conan JENKINS-LIBA-MASTER_10_test_docker_cross bash -c 'pwd'",
        ]

        List<String> resultBatCommands = helper.callStack.findAll { MethodCall call ->
            call.methodName == "bat"
        }.collect { MethodCall call ->
            call.args['script'][0] as String
        }

        resultBatCommands.eachWithIndex{String resultCommand, Integer index ->
            assertThat(resultCommand).isEqualTo(expectedBatCommands[index])
        }

        /// Test Stages which run sh commands, first the native runner then docker runner
        List<String> expectedShCommands = [
                "pwd",
                "docker exec -w /home/conan --user conan JENKINS-LIBA-MASTER_10_test_docker_linux bash -c 'pwd'",
        ]

        List<String> resultShCommands = helper.callStack.findAll { MethodCall call ->
            call.methodName == "sh"
        }.collect { MethodCall call ->
            call.args['script'][0] as String
        }

        resultShCommands.eachWithIndex{String resultCommand, Integer index ->
            assertThat(resultCommand).isEqualTo(expectedShCommands[index])
        }

    }
}
