package ac.ic.doc.mtstools.model;


public interface Refinement {
	public <S1, S2, A> boolean isARefinement(MTS<S1,A> m, MTS<S2,A> n);
}
 