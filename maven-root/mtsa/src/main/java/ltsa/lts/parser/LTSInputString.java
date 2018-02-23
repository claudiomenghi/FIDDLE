package ltsa.lts.parser;

import ltsa.lts.parser.ltsinput.LTSInput;


public class LTSInputString implements LTSInput {

	private String fSrc;
	private int fPos;

	public LTSInputString(String s) {
		fSrc = s;
		fPos = -1;
	}

	@Override
	public char nextChar() {
		fPos = fPos + 1;
		if (fPos < fSrc.length()) {
			return fSrc.charAt(fPos);
		} else {
			return '\u0000';
		}
	}

	@Override
	public char backChar() {
		fPos = fPos - 1;
		if (fPos < 0) {
			fPos = 0;
			return '\u0000';
		} else
			return fSrc.charAt(fPos);
	}

	@Override
	public int getMarker() {
		return fPos;
	}

	@Override
	public void resetMarker() {
		fPos = -1;
	}
}