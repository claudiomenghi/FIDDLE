package controller.gr.time;

import ac.ic.doc.commons.relations.Pair;

public class TranslatorPair<S,D> implements Translator<S, Pair<S,D>> {

	@Override
	public S translate(Pair<S, D> pair) {
		return pair.getFirst();
	}

}
