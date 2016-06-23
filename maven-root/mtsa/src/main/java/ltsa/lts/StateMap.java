package ltsa.lts;

public interface StateMap {
	
	public int get(byte[] state);
	/**
	 * 
	 * @param state
	 * @throws UnsupportedOperationException
	 */
	public void add(byte[] state);
	/**
	 * 
	 * @param state
	 * @param depth
	 * @throws UnsupportedOperationException
	 */
	public void add(byte[] state, int depth);
	/**
	 * 
	 * @param state
	 * @param action
	 * @param parent
	 * @throws UnsupportedOperationException
	 */
	public void add(byte[] state, int action, byte[] parent) ;	// tracing enabled version of add()
	public boolean empty();
	public byte[] getNextState();
	public void markNextState(int stateNumber);
	public boolean nextStateIsMarked();
	public void removeNextState();
	public boolean contains(byte[] state);
	public int getNextStateNumber();
}
