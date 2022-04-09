package lilytts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "merge-lines")
public class MergeLinesCommand implements Callable<Integer> {
    @Parameters(index = "0..*")
    private List<File> textFiles;

    @Override
    public Integer call() throws Exception {
        for (File textFile : textFiles) {
            final BufferedReader reader = new BufferedReader(new FileReader(textFile));
            StringBuilder result = new StringBuilder();
            
            String nextLine = reader.readLine();

            while (nextLine != null) {
                // Separate blocks by an empty line.
                if (result.length() > 0) {
                    result.append(System.lineSeparator());
                    result.append(System.lineSeparator());
                }

                // Skip any blank lines at the beginning of the block.
                while (nextLine != null && nextLine.isBlank()) {
                    nextLine = reader.readLine();
                }

                boolean firstLine = true;

                while (nextLine != null && !nextLine.isBlank()) {
                    // Separate lines by a space.
                    if (!firstLine) {
                        result.append(' ');
                    }

                    result.append(nextLine.trim());
                    nextLine = reader.readLine();
                    firstLine = false;
                }
            }

            reader.close();

            BufferedWriter out = new BufferedWriter(new FileWriter(textFile));

            out.write(result.toString());
            out.close();
        }

        return 0;
    }
}
