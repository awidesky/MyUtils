package io.github.awidesky.myUtils;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FontTest {

	@Test
	@Disabled
	void test() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(FontTest::new);
	}

}
