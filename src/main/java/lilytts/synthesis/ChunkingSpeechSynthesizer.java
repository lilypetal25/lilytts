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
    private final AudioFileMerger merger;

    public ChunkingSpeechSynthesizer(final SpeechSynthesizer inner, final SSMLSplitter splitter, final AudioFileMerger merger) {
        this.inner = inner;
        this.splitter = splitter;
        this.merger = merger;
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

        System.out.println("     Splitting text into " + chunks.size() + " chunks.");
        
        for (int i=0; i < chunks.size(); i++) {
            final String chunkFilePath = StringUtil.removeFileExtension(outputFilePath) + " chunk " + (i+1) + ".mp3";
            final File chunkFile = new File(chunkFilePath);
            final String chunkSSML = chunks.get(i);

            completedChunkFiles.add(chunkFile);

            if (chunkFile.exists() && chunkFile.length() > 0) {
                System.out.println("     Skipping chunk " + (i+1) + " because it already exists.");
                continue;
            }

            inner.synthesizeSsmlToFile(chunkSSML, chunkFilePath);
            System.out.println("     Wrote audio chunk to file " + chunkFile.getName());
        }

        // Merge all the synthesized chunks into one final audio file.
        merger.mergeAudioFiles(completedChunkFiles, new File(outputFilePath));
    }
    
}
