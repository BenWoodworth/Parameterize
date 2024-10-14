import com.benwoodworth.parameterize.test.stackTraceLines
import kotlin.test.Test
import kotlin.test.assertEquals

class ThrowableUtilTest {
    @Test
    fun stack_trace_lines_should_be_empty_if_stack_is_null() {
        val throwable = Throwable().apply {
            asDynamic().stack = null
        }

        assertEquals(emptyList(), throwable.stackTraceLines)
    }
}
