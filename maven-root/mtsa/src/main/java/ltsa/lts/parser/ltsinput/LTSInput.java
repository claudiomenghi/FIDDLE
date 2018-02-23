package ltsa.lts.parser.ltsinput;

public interface LTSInput {
	public char nextChar();

	public char backChar();

	public int getMarker();

	// >>> AMES: Enhanced Modularity
	void resetMarker();
	// <<< AMES
}