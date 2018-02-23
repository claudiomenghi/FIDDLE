package scalabilityAssessment;

import java.text.SimpleDateFormat;

public class MessageHandler {


	public static final SimpleDateFormat time_formatter = new SimpleDateFormat(
			"HH:mm:ss.SSS");
	
	static public void printMessage(String message) {
		System.out.println(time_formatter.format(System.currentTimeMillis())
				+ " - " + message);
	}

	static public void printMessage(String message, long init, long end) {
		System.out.println(time_formatter.format(System.currentTimeMillis())
				+ " - " + message + " [" + (end - init) + " ms]");

	}
}
