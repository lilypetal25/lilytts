package lilytts.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

public class FFMPEGAudioFileMerger implements AudioFileMerger {

    @Override
    public void mergeAudioFiles(List<File> inputFiles, File outputFile) {
        try {
            final File indexFile = File.createTempFile("lilytts_ffmpeg_file_list", ".txt");

            writeIndexFile(indexFile, inputFiles);

            // final String command = String.format("/opt/homebrew/bin/ffmpeg -f concat -safe 0 -i '%s -c copy '%s'", indexFile.getAbsolutePath(), outputFile.getAbsolutePath());

            ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-f",
                "concat",
                "-safe",
                "0",
                "-i",
                indexFile.getAbsolutePath(),
                "-c",
                "copy",
                outputFile.getAbsolutePath());

            final Process process = processBuilder.start();
            final int exitCode = process.waitFor();

            if (exitCode != 0) {
                printTextFromStream(process.getInputStream());
                printTextFromStream(process.getErrorStream());

                throw new RuntimeException("External call to ffmpeg terminated with exit code " + exitCode + ".\n\nFull command: \n\n" + String.join(" | ", processBuilder.command()));
            }

            indexFile.delete();
        } catch (IOException|InterruptedException e) {
            throw new RuntimeException("Unexpected error when attempting to merge audio files: " + e.getMessage(), e);
        }
    }

    private void writeIndexFile(File indexFile, List<File> inputFiles) throws FileNotFoundException {
        try (PrintWriter writer = new PrintWriter(indexFile)) {
            for (File file : inputFiles) {
                writer.println("file '" + file.getAbsolutePath() + "'");
            }

            writer.flush();
        }
    }

    private void printTextFromStream(InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
 
        String line = reader.readLine();

        while (line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
    }
}
