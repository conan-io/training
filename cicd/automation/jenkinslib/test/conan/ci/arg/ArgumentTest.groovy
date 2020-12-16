package conan.ci.arg


import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import org.junit.jupiter.api.Test

class ArgumentTest extends DeclarativePipelineTest {

    @Test
    void basicArgumentTest() throws Exception {
        ArgumentList testArg = new ConanArgs().get(["TRAINING_CONAN_CONANFILE_DIR": "whatever"])
        println(testArg.toString())
//        assertThat("").isEqualTo("")
    }
}
