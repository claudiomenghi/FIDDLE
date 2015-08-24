package util;

import java.io.File;
import java.io.IOException;

public class TestConstants {
	public static final String TESTFILES = File.separator + "testfiles" +File.separator;

	public static File fileFrom(String fileName) throws IOException {
		return new File(new File(".").getCanonicalPath() + TESTFILES + fileName);
	}
}
