package conan.ci.step

import com.lesfurets.jenkins.unit.MethodCall
import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource
import static org.assertj.core.api.Assertions.assertThat

class conanGraphLockSpec extends DeclarativePipelineTest {
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
    void conanGraphLockFromEnvVars() throws Exception {
        String stem = this.getClass().getCanonicalName().replace(".", "/")
        File jenkinsfile = new File("test/${stem}.jenkins")

        runScript(jenkinsfile.path)

        List<String> expectedCommands = [
                "conan graph lock . " +
                        "--profile profile1 --profile profile2 " +
                        "--setting build_typ=Release --setting compiler=gcc " +
                        "--option somepkg:shared=True --option otherpk:shared=False " +
                        "--env ENV_VAR1=ENV_VAL1 --env ENV_VAR2=ENV_VAL2"
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
