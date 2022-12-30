package lilytts.synthesis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lilytts.StringUtil;
import lilytts.audio.AudioFileMerger;
import lilytts.ssml.SSMLSplitter;

public class ChunkingSpeechSynthesizer implements SpeechSynthesizer {
    private final SpeechSynthesizer inner;
    private final SSMLSplitter splitter;
    private final AudioFileMerger merger = new AudioFileMerger();

    public ChunkingSpeechSynthesizer(final SpeechSynthesizer inner, final SSMLSplitter splitter) {
        this.inner = inner;
        this.splitter = splitter;
    }

    @Override
    public String getDisplayName() {
        return inner.getDisplayName();
    }

    @Override
    public void synthesizeSsmlToFile(String ssml, String outputFilePath) throws SpeechSynthesisException {
        final List<String> chunks = this.splitter.splitSSML(ssml);
        final List<File> completedChunkFiles = new ArrayList<>();

        // If the file is small enough we don't need to split it.
        if (chunks.size() <= 1) {
            inner.synthesizeSsmlToFile(ssml, outputFilePath);
            return;
        }

        System.out.print("Splitting " + outputFilePath + " into " + chunks.size() + " chunks.");
        
        for (int i=0; i < chunks.size(); i++) {
            final String chunkFilePath = StringUtil.removeFileExtension(outputFilePath) + "_chunk" + (i+1) + ".mp3";
            final File chunkFile = new File(chunkFilePath);
            completedChunkFiles.add(chunkFile);

            if (chunkFile.exists() && chunkFile.length() > 0) {
                System.out.println("Skipping chunk " + (i+1) + " because it already exists.");
                continue;
            }

            inner.synthesizeSsmlToFile(ssml, chunkFilePath);
        }

        // Merge all the synthesized chunks into one final audio file.
        merger.mergeAudioFiles(completedChunkFiles, new File(outputFilePath));
    }
    
}
