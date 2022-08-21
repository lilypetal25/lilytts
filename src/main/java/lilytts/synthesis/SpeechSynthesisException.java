package lilytts.synthesis;

public class SpeechSynthesisException extends Exception {

    public SpeechSynthesisException() {
    }

    public SpeechSynthesisException(String message) {
        super(message);
    }

    public SpeechSynthesisException(Throwable cause) {
        super(cause);
    }

    public SpeechSynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
