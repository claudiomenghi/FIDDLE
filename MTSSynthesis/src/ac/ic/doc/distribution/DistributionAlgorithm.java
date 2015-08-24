package ac.ic.doc.distribution;

import ac.ic.doc.distribution.model.AlphabetDistribution;
import ac.ic.doc.distribution.model.DistributionResult;
import ac.ic.doc.mtstools.model.MTS;

/**
 * TODO add comment
 * @author gsibay
 *
 */
public interface DistributionAlgorithm<S, A> {

	public abstract DistributionResult<S, A> tryDistribute(MTS<S, A> monolithicModel, AlphabetDistribution<A> alphabetDistribution, A tauAction);

}
