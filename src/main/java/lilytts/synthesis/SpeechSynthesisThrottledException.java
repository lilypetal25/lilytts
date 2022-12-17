package lilytts.synthesis;

public class SpeechSynthesisThrottledException extends SpeechSynthesisException {

    public SpeechSynthesisThrottledException() {
    }

    public SpeechSynthesisThrottledException(String message) {
        super(message);
    }

    public SpeechSynthesisThrottledException(Throwable cause) {
        super(cause);
    }

    public SpeechSynthesisThrottledException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
