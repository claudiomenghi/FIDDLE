package ltsa.lts.ltl.formula;

import java.util.BitSet;
import java.util.Set;

import ltsa.lts.ltl.visitors.FormulaVisitor;

/**
 * Describes a Formula
 */
public abstract class Formula implements Comparable<Formula> {
	private int id = -1;

	private int untilsIndex = -1;
	private BitSet rightOfWhichUntil;
	private boolean visited = false;

	public void setId(int i) {
		id = i;
	}

	public int getId() {
		return id;
	}

	public boolean visited() {
		return visited;
	}

	public void setVisited() {
		visited = true;
	}

	public int getUI() {
		return untilsIndex;
	}

	public void setUI(int i) {
		untilsIndex = i;
	}

	public void setRofUI(int i) {
		if (rightOfWhichUntil == null)
			rightOfWhichUntil = new BitSet();
		rightOfWhichUntil.set(i);
	}

	public BitSet getRofWU() {
		return rightOfWhichUntil;
	}

	public boolean isRightOfUntil() {
		return rightOfWhichUntil != null;
	}

	@Override
	public int compareTo(Formula obj) {
		return id - (obj).id;
	}

	public abstract Formula accept(FormulaVisitor v);

	public boolean isLiteral() {
		return false;
	}

	public Formula getSub1() {
		return accept(Sub1.get());
	}

	public Formula getSub2() {
		return accept(Sub2.get());
	}
	
	public abstract Set<Proposition> getPropositions();
}

/*
 * get left sub formula or right for R
 */
class Sub1 implements FormulaVisitor {
	private static Sub1 inst;

	private Sub1() {
	}

	public static Sub1 get() {
		if (inst == null)
			inst = new Sub1();
		return inst;
	}

	@Override
	public Formula visit(True t) {
		return null;
	}

	@Override
	public Formula visit(False f) {
		return null;
	}

	@Override
	public Formula visit(Proposition p) {
		return null;
	}

	@Override
	public Formula visit(Not n) {
		return n.getNext();
	}

	@Override
	public Formula visit(Next n) {
		return n.getNext();
	}

	@Override
	public Formula visit(And a) {
		return a.getLeft();
	}

	@Override
	public Formula visit(Or o) {
		return o.getLeft();
	}

	@Override
	public Formula visit(Until u) {
		return u.getLeft();
	}

	@Override
	public Formula visit(Release r) {
		return r.getRight();
	}
}

/*
 * get right sub formula or left for R
 */

class Sub2 implements FormulaVisitor {
	private static Sub2 inst;

	private Sub2() {
	}

	public static Sub2 get() {
		if (inst == null)
			inst = new Sub2();
		return inst;
	}

	@Override
	public Formula visit(True t) {
		return null;
	}

	@Override
	public Formula visit(False f) {
		return null;
	}

	@Override
	public Formula visit(Proposition p) {
		return null;
	}

	@Override
	public Formula visit(Not n) {
		return null;
	}

	@Override
	public Formula visit(Next n) {
		return null;
	}

	@Override
	public Formula visit(And a) {
		return a.getRight();
	}

	@Override
	public Formula visit(Or o) {
		return o.getRight();
	}

	@Override
	public Formula visit(Until u) {
		return u.getRight();
	}

	@Override
	public Formula visit(Release r) {
		return r.getLeft();
	}
}










