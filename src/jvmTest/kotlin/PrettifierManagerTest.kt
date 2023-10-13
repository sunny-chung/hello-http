import com.sunnychung.application.multiplatform.hellohttp.manager.PrettifierManager
import kotlin.test.Test

class PrettifierManagerTest {
    val instance = PrettifierManager()

    @Test
    fun hasJsonPrettifier() {
        assert(instance.matchPrettifiers("application/json").isNotEmpty())
        assert(instance.matchPrettifiers("application/json; charset=utf-8").isNotEmpty())
    }
}
