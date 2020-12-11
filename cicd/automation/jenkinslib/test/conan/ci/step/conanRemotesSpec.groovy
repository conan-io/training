package conan.ci.step

import com.lesfurets.jenkins.unit.MethodCall
import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import static org.assertj.core.api.Assertions.assertThat

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource

class conanRemotesSpec extends DeclarativePipelineTest {
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
        helper.registerAllowedMethod("isUnix", [], { -> System.properties['os.name'].contains('Windows') })
    };

    @BeforeEach
    void jpuJunit4Setup(){
        setUp()
    }

    @Test
    void conanRemotesConfigureFromEnvVars() throws Exception {
        String stem = this.getClass().getCanonicalName().replace(".", "/")
        File jenkinsfile = new File("test/${stem}.jenkins")

        runScript(jenkinsfile.path)

        List<String> expectedCommands = [
                "conan remote add conan-local-prod http://artifactory:8082/artifactory/conan/conan-local-prod --insert=0",
                "conan remote add conan-local-temp http://artifactory:8082/artifactory/conan/conan-local-temp --insert=1",
                "conan user -p psw -r conan-local-prod usr",
                "conan user -p psw -r conan-local-temp usr",
        ]

        List<String> resultCommands = helper.callStack.findAll { MethodCall call ->
            call.methodName == "sh"
        }.collect { MethodCall call ->
            call.args['script'][0] as String
        }
        resultCommands.eachWithIndex{String resultCommand, Integer index ->
            assertThat(resultCommand).isEqualTo(expectedCommands[index])
        }
    }
}
