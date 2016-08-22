package ltsa.compiler.test.compositional;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.LTSInputString;
import ltsa.lts.parser.ltsinput.LTSInput;
import ltsa.ui.EmptyLTSOuput;

import org.junit.Test;

public class CompilerCompositionalOperatorsTest {

	@Test
	public void test() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		String fileURL=
				classLoader.getResource("ltsa.compiler.test.compositional.PowerPlant.lts").getFile();
		
		byte[] encoded = Files.readAllBytes(Paths.get(fileURL));
		 
		String ltsInput=new String(encoded, Charset.defaultCharset());
		System.out.println(ltsInput);
		LTSInput input=new LTSInputString(ltsInput);
		LTSOutput output=new EmptyLTSOuput();
		LTSCompiler ltsCompiler=new LTSCompiler(input, output, null);
		ltsCompiler.compile();
		
	}

}
