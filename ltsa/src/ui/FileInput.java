package ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import lts.LTSInput;

/**
 * An implementation of LTSInput which is convinient for command-line execution.
 */
public class FileInput implements LTSInput {

	int fPos = -1;
	String fSrc;
	
	public FileInput(File f) throws FileNotFoundException, IOException {
		FileInputStream s = new FileInputStream(f);
		byte[] bytes = new byte[(int) f.length()];
		s.read(bytes);
		fSrc = new String(bytes);
	}
	
	public char backChar() {
		fPos = fPos - 1;
		if (fPos < 0) {
			fPos = 0;
			return '\u0000';
		}
		else
			return fSrc.charAt (fPos);
	}

	public int getMarker() {
		return fPos;
	}

	public char nextChar() {
		fPos = fPos + 1;
		if (fPos < fSrc.length ()) {
			return fSrc.charAt (fPos);
		} else {
			// XXX: Why is this commented out in ui.HPWindow.nextChar?
			// fPos = fPos - 1;
			return '\u0000';
		}
	}

	public void resetMarker() {
		fPos = -1;		
	}
}
