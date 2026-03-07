package io.github.awidesky.myUtils.hash;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class HashTest {

	@Test
	@Disabled
	void testFileHash() throws IOException {
		FileHash fh = new FileHash(new PrintWriter(System.out, true));
		var re = fh.compareTwoDirectories(
				Paths.get(System.getProperty("user.home"), "Downloads"),
				Paths.get(System.getProperty("user.home"), "Downloads")
				);
		System.out.println("IsSame : " + re.isSame());
	}

	@Test
	@Disabled
	void testFileHashGUI() throws InvocationTargetException, InterruptedException  {
		SwingUtilities.invokeAndWait(SwingDirectoryCompare::new);
	}

}
