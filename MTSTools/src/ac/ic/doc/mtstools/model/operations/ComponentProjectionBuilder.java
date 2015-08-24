package ac.ic.doc.mtstools.model.operations;

import java.util.Set;

import ac.ic.doc.mtstools.model.MTS;

/**
 * @author gsibay
 *
 */
public interface ComponentProjectionBuilder {

	/**
	 * Builds the component with alphabet componentActions from the centralisedMTS
	 * @param centralisedMTS
	 * @param componentActions
	 * @return
	 */
	public abstract MTS<Long, String> buildComponentProjection(MTS<Long, String> centralisedMTS, Set<String> componentActions);
	
}
