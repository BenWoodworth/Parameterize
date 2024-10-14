import com.benwoodworth.parameterize.createThrowableSubclassWithoutStack
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateThrowableSubclassWithoutStackTest {
    @Test
    fun stack_should_be_left_null() {
        class ThrowableSubclass : Throwable()

        val throwable = createThrowableSubclassWithoutStack {
            ThrowableSubclass()
        }

        assertEquals(null, throwable.asDynamic().stack)
    }
}
