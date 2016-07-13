package ltsa.lts.operations.composition;

import ltsa.lts.animator.StateCodec;

public class CompositionEngineFactory {

	public static CompositionEngine createCompositionEngine(
			String engineClassName, StateCodec coder) {

		CompositionEngine result;
		switch (engineClassName) {
		case "DFSCompositionEngine":
			result = new DFSCompositionEngine(coder);
			break;
		case "BFSCompositionEngine":
			result = new BFSCompositionEngine(coder);
			break;
		default:
			throw new IllegalArgumentException("The engine " + engineClassName
					+ " is not an available engine");
		}

		return result;
	}
}
