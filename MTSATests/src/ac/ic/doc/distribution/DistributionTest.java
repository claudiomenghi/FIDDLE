package ac.ic.doc.distribution;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.PredicateUtils;
import org.junit.Test;

import ac.ic.doc.distribution.model.AlphabetDistribution;
import ac.ic.doc.distribution.model.DistributionFeedbackOnFullAlphabet;
import ac.ic.doc.distribution.model.DistributionResult;
import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.MTSConstants;
import ac.ic.doc.mtstools.model.impl.MTSImpl;

public class DistributionTest{

	public void testAlphabetModelBigger() {
		DistributionFacade<Long, String> distributionFacade = new DistributionFacade<Long, String>();
		
		MTS<Long, String> monolithicModel = new MTSImpl<Long, String>(new Long(0));
		Set<String> actions = new HashSet<String>();
		actions.add("a");
		actions.add("b");
		actions.add("c");
		actions.add("d");
		
		monolithicModel.addActions(actions);
		
		AlphabetDistribution<String> alphabetDistribution = this.createDefaultAD();
		
		DistributionResult<Long, String> result = distributionFacade.tryDistribute(monolithicModel, alphabetDistribution, MTSConstants.TAU);
		Predicate isDistributionFeedbackOnFullAlphabet = PredicateUtils.instanceofPredicate(DistributionFeedbackOnFullAlphabet.class);
		
		Assert.assertTrue(!CollectionUtils.exists(result.getFeedback(), isDistributionFeedbackOnFullAlphabet));
	}
	
	public void testAlphabetModelSmaller() {
		DistributionFacade<Long, String> distributionFacade = new DistributionFacade<Long, String>();
		
		MTS<Long, String> monolithicModel = new MTSImpl<Long, String>(new Long(0));
		Set<String> actions = new HashSet<String>();
		actions.add("a");
		actions.add("b");
		
		monolithicModel.addActions(actions);
		
		AlphabetDistribution<String> alphabetDistribution = this.createDefaultAD();
		
		DistributionResult<Long, String> result = distributionFacade.tryDistribute(monolithicModel, alphabetDistribution, MTSConstants.TAU);
		Predicate isDistributionFeedbackOnFullAlphabet = PredicateUtils.instanceofPredicate(DistributionFeedbackOnFullAlphabet.class);
		
		Assert.assertTrue(!CollectionUtils.exists(result.getFeedback(), isDistributionFeedbackOnFullAlphabet));
	}

	@Test
	public void testAlphabetModelEqualsAlphabetDistribution() {
		DistributionFacade<Long, String> distributionFacade = new DistributionFacade<Long, String>();
		
		MTS<Long, String> monolithicModel = new MTSImpl<Long, String>(new Long(0));
		Set<String> actions = new HashSet<String>();
		actions.add("a");
		actions.add("b");
		actions.add("c");
		
		monolithicModel.addActions(actions);
		
		AlphabetDistribution<String> alphabetDistribution = this.createDefaultAD();
		
		distributionFacade.tryDistribute(monolithicModel, alphabetDistribution, MTSConstants.TAU);
	}

	
	private AlphabetDistribution<String> createDefaultAD() {
		Set<Set<String>> alphabets = new HashSet<Set<String>>();
		HashSet<String> alpha1 = new HashSet<String>();
		alpha1.add("a");
		alpha1.add("b");
		
		alphabets.add(alpha1);
		
		HashSet<String> alpha2 = new HashSet<String>();
		alpha2.add("b");
		alpha2.add("c");
		
		alphabets.add(alpha2);
	    return new AlphabetDistribution<String>(alphabets);
	}
			    
}
