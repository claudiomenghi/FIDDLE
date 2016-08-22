package ltsa.lts.automata.automaton.event;

public class LTSEvent {

	public static final int NEWSTATE = 0;
	public static final int INVALID = 1;
	public static final int KILL = 2;

	public static final int NEWCOMPOSITES = 3;
	public static final int NEWPROCESSES = 4;
	public static final int NEWLABELSETS = 5;

	public int kind;
	public Object info;
	public String name;

	public LTSEvent(int kind, Object info) {
		this.kind = kind;
		this.info = info;
	}

	public LTSEvent(int kind, Object info, String name) {
		this(kind, info);
		this.name = name;
	}

}
