package conan.ci.step

import com.lesfurets.jenkins.unit.MethodCall
import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource
import static org.assertj.core.api.Assertions.assertThat

class dockerSpec extends DeclarativePipelineTest {
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
        helper.registerAllowedMethod("touch", [String.class], null)
    };

    @BeforeEach
    void jpuJunit4Setup(){
        setUp()
    }

    @Test
    void withDemoDockerContainer() throws Exception {
        String stem = this.getClass().getCanonicalName().replace(".", "/")
        File jenkinsfile = new File("test/${stem}.jenkins")
        runScript(jenkinsfile.path)

        /// Test Stages which run bat commands
        List<String> expectedBatCommands = [
                /// The windows stage
                "docker run -d -t --name JENKINS-LIBA-MASTER_10a_test_windows --user conan windows_image",
                "docker exec -w C:\\Users\\ContainerAdministrator --user conan JENKINS-LIBA-MASTER_10a_test_windows cmd -c 'cd'",
                "docker cp somesrc JENKINS-LIBA-MASTER_10a_test_windows:C:\\Users\\ContainerAdministrator/somedst",
                "docker cp JENKINS-LIBA-MASTER_10a_test_windows:C:\\Users\\ContainerAdministrator/somesrc somedst",
                "docker rm -f JENKINS-LIBA-MASTER_10a_test_windows",
                /// The cross-build stage
                "docker run -d -t --name JENKINS-LIBA-MASTER_10b_test_cross --user conan linux_image",
                "docker exec -w /home/conan --user conan JENKINS-LIBA-MASTER_10b_test_cross bash -c 'pwd'",
                "docker cp somesrc JENKINS-LIBA-MASTER_10b_test_cross:/home/conan/somedst",
                "docker cp JENKINS-LIBA-MASTER_10b_test_cross:/home/conan/somesrc somedst",
                "docker rm -f JENKINS-LIBA-MASTER_10b_test_cross",
        ]

        List<String> resultBatCommands = helper.callStack.findAll { MethodCall call ->
            call.methodName == "bat"
        }.collect { MethodCall call ->
            call.args['script'][0] as String
        }

        resultBatCommands.eachWithIndex{String resultCommand, Integer index ->
            assertThat(resultCommand).isEqualTo(expectedBatCommands[index])
        }

        /// Test Stages which run sh commands
        List<String> expectedShCommands = [
                /// The linux stage
                "docker run -d -t --name JENKINS-LIBA-MASTER_10_test_linux --user conan linux_image",
                "docker exec -w /home/conan --user conan JENKINS-LIBA-MASTER_10_test_linux bash -c 'pwd'",
                "docker cp somesrc JENKINS-LIBA-MASTER_10_test_linux:/home/conan/somedst",
                "docker cp JENKINS-LIBA-MASTER_10_test_linux:/home/conan/somesrc somedst",
                "docker rm -f JENKINS-LIBA-MASTER_10_test_linux",
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
