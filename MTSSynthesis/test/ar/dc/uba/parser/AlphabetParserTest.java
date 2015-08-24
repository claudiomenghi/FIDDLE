package ar.dc.uba.parser;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import ar.dc.uba.model.language.Alphabet;
import ar.dc.uba.model.language.SingleSymbol;
import ar.dc.uba.model.language.Symbol;

/**
 * 
 * @author gsibay
 *
 */
public class AlphabetParserTest {

	/**
	 */
	@Test
	public void testParseSingletonAlphabet() {
		String alphabetStr = "{a}";
		
		HashSet<Symbol> symbols = new HashSet<Symbol>();
		Collections.addAll(symbols, new SingleSymbol("a"));
		
		assertEquals(new Alphabet(symbols),this.parseAlphabet(alphabetStr));
	}

	/**
	 */
	@Test
	public void testParseAlphabet() {
		String alphabetStr = "{a,turnOn,turnOff,k,up,down}";
		
		HashSet<Symbol> symbols = new HashSet<Symbol>();
		Collections.addAll(symbols, new SingleSymbol("a"), new SingleSymbol("turnOn"), new SingleSymbol("turnOff"), new SingleSymbol("k"),
				new SingleSymbol("up"), new SingleSymbol("down"));
		
		assertEquals(new Alphabet(symbols),this.parseAlphabet(alphabetStr));
	}
	
	private Alphabet parseAlphabet(String alphabet) {
		return AlphabetParser.getInstance().parseAlphabet(alphabet);
	}

}
