package lilytts.synthesis;

import lilytts.progress.ProgressListener;

public interface SpeechSynthesizer {
    String getDisplayName();

    default void synthesizeSsmlToFile(String ssml, String outputFilePath, ProgressListener listener) throws SpeechSynthesisException {
        synthesizeSsmlToFile(ssml, outputFilePath);
    }

    void synthesizeSsmlToFile(String ssml, String outputFilePath) throws SpeechSynthesisException;
}
