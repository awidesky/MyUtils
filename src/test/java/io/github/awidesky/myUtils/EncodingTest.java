package io.github.awidesky.myUtils;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class EncodingTest {

	@Test
	@Disabled
	void test() throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(InvalidEncodingTest::new);
	}

}
