package lilytts.synthesis;

import java.util.List;

public class CompoundSynthesizer implements SpeechSynthesizer {
    public static CompoundSynthesizer tryInPriorityOrder(List<? extends SpeechSynthesizer> synthesizers) {
        return new CompoundSynthesizer(synthesizers);
    }

    private final List<? extends SpeechSynthesizer> speechSynthesizers;
    private int currentSynthesizerIndex = 0;

    private CompoundSynthesizer(List<? extends SpeechSynthesizer> speechSynthesizers) {
        if (speechSynthesizers == null || speechSynthesizers.isEmpty()) {
            throw new IllegalArgumentException("speechSynthesizers must not be null or empty");
        }

        this.speechSynthesizers = speechSynthesizers;
    }

    public void synthesizeSsmlToFile(String ssml, String filePath) throws SpeechSynthesisException {
        while (true) {
            if (this.speechSynthesizers.size() <= currentSynthesizerIndex) {
                throw new IllegalStateException("Ran out of speech synthesizers to try.");
            }

            try {
                this.speechSynthesizers.get(currentSynthesizerIndex).synthesizeSsmlToFile(ssml, filePath);
            } catch (SpeechSynthesisThrottledException exception) {
                this.currentSynthesizerIndex++;

                if (this.speechSynthesizers.size() <= currentSynthesizerIndex) {
                    throw exception;
                } else {
                    System.out.println("Speech synthesis request was throttled on synthesizer " + (this.currentSynthesizerIndex + 1) + ". Switching to synthesizer " + this.currentSynthesizerIndex + ".");
                }
            }
        }
    }
}
