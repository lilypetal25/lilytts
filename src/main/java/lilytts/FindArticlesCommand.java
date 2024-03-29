package lilytts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "find-articles")
public class FindArticlesCommand implements Callable<Integer> {
    @Parameters(index = "0")
    private File inputDirectory;

    @Option(names = { "--ignore" } )
    private List<File> ignoreFiles = Collections.emptyList();

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();

        final List<File> articleFiles = findArticleFiles();
        
        // Sort files by creation time, from first created to last created.
        Collections.sort(articleFiles, (file1, file2) -> {
            try {
                final FileTime creationTime1 = (FileTime) Files.getAttribute(file1.toPath(), "creationTime");
                final FileTime creationTime2 = (FileTime) Files.getAttribute(file2.toPath(), "creationTime");
                return creationTime1.compareTo(creationTime2);
            } catch (IOException e) {
                throw new RuntimeException("Unexpected error when reading file creation time: " + e.getMessage(), e);
            }
        });

        for (File articleFile : articleFiles) {
            System.out.println(articleFile.getPath());
        }

        return 0;
    }

    private void validateCommandLineParameters() {
        if (!inputDirectory.isDirectory() || !inputDirectory.exists()) {
            throw new IllegalArgumentException("Invalid input directory: " + inputDirectory.getAbsolutePath());
        }
    }

    // TODO: Share this logic with NewsToSpeechAzureCommand.
    private List<File> findArticleFiles() {
        return Arrays.asList(inputDirectory.listFiles(file -> {
            // Only match *.txt files.
            if (!file.getName().endsWith(".txt")) {
                return false;
            }

            // Ignore files in the ignore list.
            // TODO: Implement a compare file helper method to share the code for comparing files by absolute path.
            if (this.ignoreFiles.stream().anyMatch(ignored -> ignored.getAbsoluteFile().equals(file.getAbsoluteFile()))) {
                return false;
            }

            return true;
        }));
    }
}
