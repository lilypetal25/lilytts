package lilytts.synthesis;

public interface SpeechSynthesizer {
    void synthesizeSsmlToFile(String ssml, String outputFilePath) throws SpeechSynthesisException;
}
