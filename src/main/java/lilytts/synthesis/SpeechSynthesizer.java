package lilytts.synthesis;

public interface SpeechSynthesizer {
    String getDisplayName();

    void synthesizeSsmlToFile(String ssml, String outputFilePath) throws SpeechSynthesisException;
}
