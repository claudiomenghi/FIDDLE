package ac.ic.doc.mtsa;

import java.io.File;
import java.io.IOException;

import lts.CompositeState;
import lts.LTSCompiler;
import lts.LTSInput;
import lts.LTSInputString;
import lts.LTSOutput;
import ui.FileInput;
import ui.StandardOutput;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import dispatcher.TransitionSystemDispatcher;



public class MTSCompiler {
	
	private static MTSCompiler instance = new MTSCompiler();
	public static MTSCompiler getInstance() { return instance; }
	private MTSCompiler() {}


	public MTS<Long, String> compileMTS(String modelName, File inputFile) throws Exception {
		CompositeState compositeState = compileCompositeState(modelName, inputFile);
		return AutomataToMTSConverter.getInstance().convert(compositeState.getComposition());
	}

	public MTS<Long, String> compileMTS(String modelName, String inputString) throws Exception {
		CompositeState compositeState = compileCompositeState(modelName, inputString);
		return AutomataToMTSConverter.getInstance().convert(compositeState.getComposition());
	}
	
	public CompositeState compileCompositeState(String modelName, File inputFile) throws IOException {
		LTSInput input = new FileInput(inputFile);
		return compileCompositeState(modelName, input);
	}
	
	public CompositeState compileCompositeState(String modelName, LTSInput input) throws IOException {
		StandardOutput output = new StandardOutput();
		return compileComposite(modelName, input, output);
	}

	public CompositeState compileCompositeState(String modelName, File inputFile, LTSOutput output) throws IOException {
		LTSInput input = new FileInput(inputFile);
		return compileComposite(modelName, input, output);
	}

	public CompositeState compileCompositeState(String modelName, String inputString) throws IOException {
		return compileComposite(modelName, new LTSInputString(inputString), new StandardOutput());
	}

//	private CompositeState compileComposite(String modelName, LTSInput input) throws IOException {
//		LTSOutput output = new StandardOutput(); 
//		return compileComposite(modelName, input, output);
//	}
	
	public CompositeState compileComposite(String modelName, LTSInput input, LTSOutput output) throws IOException {
		String currentDirectory = (new File(".")).getCanonicalPath();
		LTSCompiler compiler = new LTSCompiler(input, output, currentDirectory);
		compiler.compile();
		CompositeState c = compiler.continueCompilation(modelName);
		TransitionSystemDispatcher.applyComposition(c, output);
		return c;
	}

	public LTSCompiler getCompiler(LTSInput input) throws IOException {
		LTSCompiler compiler = new LTSCompiler(input, new StandardOutput(), (new File(".")).getCanonicalPath());
		return compiler;
	}
}
