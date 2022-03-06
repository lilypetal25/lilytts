package lilytts;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "text-to-ssml")
class TextToSsmlCommand implements Callable<Integer> {
    @Parameters(index = "0")
    private File outputDirectory;

    @Parameters(index = "1")
    private List<File> inputFiles;

    @Override
    public Integer call() throws Exception {
        System.out.printf("Output directory: %s\n", outputDirectory.getAbsolutePath());
        
        for (File file : inputFiles) {
            System.out.printf("Input file: %s\n", file.getAbsolutePath());
        }

        return 0;
    }
}