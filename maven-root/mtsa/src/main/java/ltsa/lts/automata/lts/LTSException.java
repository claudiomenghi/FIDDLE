package ltsa.lts.automata.lts;

public class LTSException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3275424071205029666L;

	public Object marker;

	public LTSException(String errorMsg) {
		super(errorMsg);
		this.marker = null;
	}

	public LTSException(String errorMsg, Object marker) {
		super(errorMsg);
		this.marker = marker;
	}

}