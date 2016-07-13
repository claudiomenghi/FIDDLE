package ltsa.lts.parser;

import java.math.BigDecimal;

/* -------------------------------------------------------------------------------*/
public class Value {
    private BigDecimal val;
    private String sval;
    private boolean sonly;

    public Value(double i) {
    	this(new BigDecimal(i));
    }
    
    protected Value(BigDecimal i) {
        val = i;
        sonly = false;
        sval = String.valueOf(i);
    }
    
    protected Value(String s) {   //convert string to integer of possible
        sval = s;
        try {
            val = new BigDecimal(s);
            sonly = false;
        } catch (NumberFormatException e) {
            sonly = true;
        }
    }

    @Override
    public String toString() {
        return sval;
    }
    
    protected BigDecimal doubleValue() {
    	return val;
    }

    protected boolean isNumeric() {
        return !sonly;
    }

    protected boolean isLabel() {
        return sonly;
    }
}