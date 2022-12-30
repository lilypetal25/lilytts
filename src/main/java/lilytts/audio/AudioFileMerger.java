package lilytts.audio;

import java.io.File;
import java.util.List;

public interface AudioFileMerger {
    void mergeAudioFiles(List<File> inputFiles, File outputFile);
}