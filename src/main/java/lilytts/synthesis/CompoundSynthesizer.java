package lilytts.synthesis;

import java.util.List;
import java.util.stream.Collectors;

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

    public String getDisplayName() {
        return this.speechSynthesizers.stream().map(x -> x.getDisplayName()).collect(Collectors.joining(", "));
    }

    public void synthesizeSsmlToFile(String ssml, String filePath) throws SpeechSynthesisException {
        while (true) {
            if (this.speechSynthesizers.size() <= currentSynthesizerIndex) {
                throw new IllegalStateException("Ran out of speech synthesizers to try.");
            }

            final SpeechSynthesizer currentSynthesizer = this.speechSynthesizers.get(currentSynthesizerIndex);

            try {
                currentSynthesizer.synthesizeSsmlToFile(ssml, filePath);
                return;
            } catch (SpeechSynthesisThrottledException exception) {
                this.currentSynthesizerIndex++;

                if (this.speechSynthesizers.size() <= currentSynthesizerIndex) {
                    throw exception;
                } else {
                    final SpeechSynthesizer nexSynthesizer = this.speechSynthesizers.get(currentSynthesizerIndex);
                    System.out.println("Speech synthesis request was throttled on synthesizer [" + currentSynthesizer.getDisplayName() + "]. Switching to synthesizer [" + nexSynthesizer.getDisplayName() + "].");
                }
            }
        }
    }
}
