package ui;

import lts.LTSError;
import lts.LTSException;

/**
 * An implementation of LTSError which is convinient for command-line execution.
 */
public class StandardError implements LTSError {

	public void displayError(LTSException x) {
		System.err.println(x);
	}

}
