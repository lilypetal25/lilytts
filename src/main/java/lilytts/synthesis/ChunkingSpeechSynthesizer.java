package lilytts.synthesis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lilytts.StringUtil;
import lilytts.audio.AudioFileMerger;
import lilytts.progress.NullProgressListener;
import lilytts.progress.ProgressEvent;
import lilytts.progress.ProgressListener;
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
        this.synthesizeSsmlToFile(ssml, outputFilePath, new NullProgressListener());
    }

    @Override
    public void synthesizeSsmlToFile(String ssml, String outputFilePath, ProgressListener listener) throws SpeechSynthesisException {
        final List<String> chunks = this.splitter.splitSSML(ssml);
        final List<File> completedChunkFiles = new ArrayList<>();

        // If the file is small enough we don't need to split it.
        if (chunks.size() <= 1) {
            listener.onProgress(new ProgressEvent("Synthesizing audio", 0, ssml.length()));
            inner.synthesizeSsmlToFile(ssml, outputFilePath);
            listener.onProgress(new ProgressEvent("Done!", ssml.length(), ssml.length()));
            return;
        }

        final long maxProgress = chunks.stream().collect(Collectors.summingLong(x -> x.length()));
        long currentProgress = 0;

        for (int i=0; i < chunks.size(); i++) {
            final String progressMessage = String.format("%d/%d", i+1, chunks.size());
            listener.onProgress(new ProgressEvent(progressMessage, currentProgress, maxProgress));

            final String chunkFilePath = StringUtil.removeFileExtension(outputFilePath) + " chunk " + (i+1) + ".mp3";
            final File chunkFile = new File(chunkFilePath);
            final String chunkSSML = chunks.get(i);

            completedChunkFiles.add(chunkFile);

            if (chunkFile.exists() && chunkFile.length() > 0) {
                continue;
            }

            inner.synthesizeSsmlToFile(chunkSSML, chunkFilePath);
            currentProgress += chunkSSML.length();
        }

        // Merge all the synthesized chunks into one final audio file.
        final String progressMessage = String.format("Merging %d chunks", chunks.size());
        listener.onProgress(new ProgressEvent(progressMessage, maxProgress, maxProgress));

        final File mergedAudioFile = new File(outputFilePath);
        merger.mergeAudioFiles(completedChunkFiles, mergedAudioFile);

        // Delete the temporary audio chunks.
        listener.onProgress(new ProgressEvent("Done!", maxProgress, maxProgress));

        for (final File chunkFile : completedChunkFiles) {
            chunkFile.delete();
        }
    }
}
