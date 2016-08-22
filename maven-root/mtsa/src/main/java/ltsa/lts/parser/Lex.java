package ltsa.lts.parser;

import java.math.BigDecimal;

import ltsa.lts.Diagnostics;
import ltsa.lts.parser.ltsinput.LTSInput;

public class Lex {

	private LTSInput input;
	private Symbol symbol;
	private char ch;
	private boolean eoln;
	private boolean newSymbols = true; // true is return new object per symbol
	/* push back interface to Lex */
	private Symbol current = null;
	private Symbol buffer = null;

	public Lex(LTSInput input) {
		this(input, true);
	}

	public Lex(LTSInput input, boolean newSymbols) {
		this.input = input;
		this.newSymbols = newSymbols;
		if (!newSymbols){
			symbol = new Symbol(); // use this for everything
		}
	}

	private void error(String errorMsg) {
		Diagnostics.fatal(errorMsg, new Integer(input.getMarker()));
	}

	private void nextCh() {
		ch = input.nextChar();
		eoln = (ch == '\n' || ch == '\u0000');
	}

	private void backCh() {
		ch = input.backChar();
		eoln = (ch == '\n' || ch == '\u0000');
	}

	private void inComment() {
		if (ch == '/') { // Skip C++ Style comment
			do {
				nextCh();
			} while (eoln == false);
		} else { // Skip C Style comment
			do {
				do {
					nextCh();
				} while (ch != '*' && ch != '\u0000');
				do {
					nextCh();
				} while (ch == '*' && ch != '\u0000');
			} while (ch != '/' && ch != '\u0000');
			nextCh();
		}
		if (!newSymbols) {
			symbol.kind = Symbol.COMMENT;
			backCh();
		}
	}

	private boolean isodigit(char ch) {
		return ch >= '0' && ch <= '7';
	}

	private boolean isxdigit(char ch) {
		return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F')
				|| (ch >= 'a' && ch <= 'f');
	}

	private boolean isbase(char ch, int base) {
		switch (base) {
		case 10:
			return Character.isDigit(ch);
		case 16:
			return isxdigit(ch);
		case 8:
			return isodigit(ch);
		}
		return true; // dummy statement to stop to remove compiler warning
	}

	private void inNumber() {
		long intValue = 0;
		int digit = 0;
		int base = 10;

		symbol.kind = Symbol.INT_VALUE; // assume number is a INT

		// determine base of number
		if (ch == '0') {
			nextCh();

			if (ch == 'x' || ch == 'X') {
				base = 16;
				nextCh();
			} else {
				base = 8;
			}

		} else {
			base = 10;
		}

		StringBuffer numericBuf = new StringBuffer(); // holds potential real
														// literals

		while (isbase(ch, base)) {
			numericBuf.append(ch);
			switch (base) {
			case 8:
			case 10:
				digit = ch - '0';
				break;
			case 16:
				if (Character.isUpperCase(ch))
					digit = (ch - 'A') + 10;
				else if (Character.isLowerCase(ch))
					digit = (ch - 'a') + 10;
				else
					digit = ch - '0';
			}

			if (intValue * base > Integer.MAX_VALUE - digit) {
				error("Integer Overflow");
				intValue = Integer.MAX_VALUE;
				break;
			} else {
				intValue = intValue * base + digit;
			}

			nextCh();
		}

		if (intValue == 0 && ch == '.') {
			nextCh();
			if (ch != '.')
				base = 10;
			backCh();
		}

		symbol.setValue(BigDecimal.valueOf(intValue));

		if (base == 10) { // check for double value
			boolean numIsDouble = false;

			if (ch == '.' /* && !badDoubleDef */) {
				nextCh();
				if (ch != '.') {
					backCh();
					numIsDouble = true;
					do {
						numericBuf.append(ch);
						nextCh();
					} while (Character.isDigit(ch));
				} else {
					backCh();
				}
			}

			if (numIsDouble) {
				try {
					symbol.setValue(new BigDecimal(numericBuf.toString()));
					// symbol.setValue(BigDecimal.valueOf(Double.valueOf(numericBuf.toString())));
					symbol.kind = Symbol.DOUBLE_VALUE;
				} catch (NumberFormatException msg) {
					error("Bad double value. " + msg);
				}
			}

		} /*
		 * else if (ch == 'U' || ch == 'u' || ch == 'L' || ch == 'U') next_ch
		 * ();
		 */

		backCh();
	}

	
	// _______________________________________________________________________________________
	// IN_STRING

	private void inString() {
		char quote = ch;
		boolean more;

		StringBuffer buf = new StringBuffer();
		do {
			nextCh();
			/*
			 * if (ch == '\\') in_escseq ();
			 */// no esc sequence in strings
			if (more = (ch != quote && eoln == false))
				buf.append(ch);
		} while (more);
		symbol.setString(buf.toString());
		if (eoln){
			error("No closing character for string constant");
		}
		symbol.kind = Symbol.STRING_VALUE;
	}

	// _______________________________________________________________________________
	// IN_IDENTIFIER

	private void inIdentifier() {
		StringBuffer buf = new StringBuffer();
		do {
			buf.append(ch);
			nextCh();
		} while (Character.isLetterOrDigit(ch) || ch == '_' || ch == '?');
		String s = buf.toString();
		symbol.setString(s);
		Integer kind = SymbolTable.get(s);
		if (kind == null) {
			if (Character.isUpperCase(s.charAt(0)))
				symbol.kind = Symbol.UPPERIDENT;
			else
				symbol.kind = Symbol.IDENTIFIER;
		} else {
			symbol.kind = kind.intValue();
		}

		backCh();
	}

	// _______________________________________________________________________________________
	// IN_SYM

	public Symbol inSym() {
		nextCh();
		if (newSymbols) {
			symbol = new Symbol();
		}

		boolean DoOnce = true;

		while (DoOnce) {
			DoOnce = false;

			symbol.startPos = input.getMarker();
			switch (ch) {
			case '\u0000':
				symbol.kind = Symbol.EOFSYM;
				break;

			// Whitespaces, Comments & Line directives
			case ' ':
			case '\t':
			case '\n':
			case '\r':
				// case '\v':
			case '\f':
				while (Character.isWhitespace(ch)){
					nextCh();
				}
				DoOnce = true;
				break;

			case '/':
				nextCh();
				if (ch == '/' || ch == '*') {
					inComment();
					if (newSymbols)
						DoOnce = true;
					continue;
				} else {
					symbol.kind = Symbol.DIVIDE;
					backCh();
				}
				break;

			// Identifiers, numbers and strings
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
			case 'g':
			case 'h':
			case 'i':
			case 'j':
			case 'k':
			case 'l':
			case 'm':
			case 'n':
			case 'o':
			case 'p':
			case 'q':
			case 'r':
			case 's':
			case 't':
			case 'u':
			case 'v':
			case 'w':
			case 'x':
			case 'y':
			case 'z':
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'H':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'O':
			case 'P':
			case 'Q':
			case 'R':
			case 'S':
			case 'T':
			case 'U':
			case 'V':
			case 'W':
			case 'X':
			case 'Y':
			case 'Z':
			case '_':
				inIdentifier();
				break;

			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				inNumber();
				break;

			// Single character symbols
			case '#':
				symbol.kind = Symbol.HASH;
				break;
			case '\'':
				symbol.kind = Symbol.QUOTE;
				break;

			case '"':
				inString();
				break;

			case '+':
				nextCh();
				if (ch == 'c') {
					nextCh();
					if (ch == 'r') {
						symbol.kind = Symbol.PLUS_CR;
					} else {
						symbol.kind = Symbol.PLUS_CA;
					}
				} else if (ch == '+') {
					symbol.kind = Symbol.MERGE;
				} else {
					symbol.kind = Symbol.PLUS;
					backCh();
				}
				break;

			case '*':
				nextCh();
				if (ch == '*') {
					symbol.kind = Symbol.POWER;
				} else {
					symbol.kind = Symbol.STAR;
					backCh();
				}
				break;

			case '%':
				symbol.kind = Symbol.MODULUS;
				break;

			case '^':
				symbol.kind = Symbol.CIRCUMFLEX;
				break;

			case '~':
				symbol.kind = Symbol.SINE;
				break;

			case '?':
				symbol.kind = Symbol.QUESTION;
				break;

			case ',':
				symbol.kind = Symbol.COMMA;
				break;

			case '(':
				symbol.kind = Symbol.LROUND;
				break;

			case ')':
				symbol.kind = Symbol.RROUND;
				break;

			case '{':
				symbol.kind = Symbol.LCURLY;
				break;

			case '}':
				symbol.kind = Symbol.RCURLY;
				break;

			case ']':
				symbol.kind = Symbol.RSQUARE;
				break;

			case ';':
				symbol.kind = Symbol.SEMICOLON;
				break;

			case '@':
				symbol.kind = Symbol.AT;
				break;

			case '\\':
				symbol.kind = Symbol.BACKSLASH;
				break;

			// Double character symbols
			case '[':
				nextCh();
				if (ch == ']')
					symbol.kind = Symbol.ALWAYS;
				else {
					symbol.kind = Symbol.LSQUARE;
					backCh();
				}
				break;

			case '|':
				nextCh();
				if (ch == '|')
					symbol.kind = Symbol.OR;
				else {
					symbol.kind = Symbol.BITWISE_OR;
					backCh();
				}
				break;

			case '&':
				nextCh();
				if (ch == '&')
					symbol.kind = Symbol.AND;
				else {
					symbol.kind = Symbol.BITWISE_AND;
					backCh();
				}
				break;

			case '!':
				nextCh();
				if (ch == '=')
					symbol.kind = Symbol.NOT_EQUAL;
				else {
					symbol.kind = Symbol.PLING;
					backCh();
				}
				break;

			case '<':
				nextCh();
				if (ch == '=')
					symbol.kind = Symbol.LESS_THAN_EQUAL;
				else if (ch == '<')
					symbol.kind = Symbol.SHIFT_LEFT;
				else if (ch == '>')
					symbol.kind = Symbol.EVENTUALLY;
				else if (ch == '-') {
					nextCh();
					if (ch == '>')
						symbol.kind = Symbol.EQUIVALENT;
					else {
						symbol.kind = Symbol.LESS_THAN;
						backCh();
						backCh();
					}
				} else {
					symbol.kind = Symbol.LESS_THAN;
					backCh();
				}
				break;

			case '>':
				nextCh();
				if (ch == '=')
					symbol.kind = Symbol.GREATER_THAN_EQUAL;
				else if (ch == '>')
					symbol.kind = Symbol.SHIFT_RIGHT;
				else {
					symbol.kind = Symbol.GREATER_THAN;
					backCh();
				}
				break;

			case '=':
				nextCh();
				if (ch == '=')
					symbol.kind = Symbol.EQUALS;
				else {
					symbol.kind = Symbol.BECOMES;
					backCh();
				}
				break;

			case '.':
				nextCh();
				if (ch == '.')
					symbol.kind = Symbol.DOT_DOT;
				else {
					symbol.kind = Symbol.DOT;
					backCh();
				}
				break;

			case '-':
				nextCh();
				if (ch == '>')
					symbol.kind = Symbol.ARROW;
				else {
					symbol.kind = Symbol.MINUS;
					backCh();
				}
				break;

			case ':':
				nextCh();
				if (ch == ':')
					symbol.kind = Symbol.COLON_COLON;
				else {
					symbol.kind = Symbol.COLON;
					backCh();
				}
				break;

			default:
				error("unexpected character encountered");
			} // endswitch

		}
		symbol.endPos = input.getMarker();
		return symbol;

	}


	public Symbol nextSymbol() {
		if (buffer == null) {
			current = inSym();
		} else {
			current = buffer;
			buffer = null;
		}
		return current;
	}

	public void pushSymbol() {
		buffer = current;
	}

	public Symbol current() {
		return current;
	}
}