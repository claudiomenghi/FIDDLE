package ar.dc.uba.parser;

import ar.dc.uba.model.lsc.Location;

public interface LocationParser {

	Location parseLocation(String locationAsStr);
	
}
