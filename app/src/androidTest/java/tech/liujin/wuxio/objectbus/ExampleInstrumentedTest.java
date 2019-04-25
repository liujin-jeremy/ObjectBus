package tech.liujin.wuxio.objectbus;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented startTest, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under startTest.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.example.wuxio.objectbus", appContext.getPackageName());
    }
}
