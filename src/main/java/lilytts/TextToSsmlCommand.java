package lilytts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.stream.XMLOutputFactory;

import lilytts.content.ContentItem;
import lilytts.parsing.ContentParser;
import lilytts.parsing.text.TextContentParser;
import lilytts.processing.ContentSplitter;
import lilytts.processing.MultiFileContentSplitter;
import lilytts.ssml.SSMLWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "text-to-ssml")
public class TextToSsmlCommand implements Callable<Integer> {
    @Parameters(index = "0")
    private File outputDirectory;

    @Parameters(index = "1..*")
    private List<File> inputFiles;

    @Option(names = { "--voice" })
    private String voice = null;

    @Option(names = { "--maxPartCharacters" })
    private int maxPartCharacters = 7500;

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();

        final ContentParser contentParser = TextContentParser.builder().build();
        final SSMLWriter ssmlWriter = configureSsmlWriter();
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        final ContentSplitter splitter = configureSplitter();

        for (File inputFile : inputFiles) {
            final FileReader inputStream = new FileReader(inputFile);

            final List<ContentItem> content = contentParser.readContent(inputStream);
            final List<List<ContentItem>> chunks = splitter.splitContent(content);

            for (int i = 0; i < chunks.size(); i++) {
                final String outputFileName = chunks.size() > 1 ?
                    removeFileExtension(inputFile.getName()) + " (Part " + (i+1) + ").xml" :
                    removeFileExtension(inputFile.getName()) + ".xml";

                final FileOutputStream outputStream = new FileOutputStream(new File(outputDirectory, outputFileName));

                ssmlWriter.writeSSML(chunks.get(i), xmlOutputFactory.createXMLStreamWriter(outputStream));
            }
        }

        return 0;
    }

    private ContentSplitter configureSplitter() {
        return MultiFileContentSplitter.builder()
            .withMaxPartCharacters(maxPartCharacters)
            .build();
    }

    private void validateCommandLineParameters() {
        if (!(outputDirectory.exists() && outputDirectory.isDirectory())) {
            throw new IllegalArgumentException("Invalid output directory: " + outputDirectory.getAbsolutePath());
        }
        
        for (File file : inputFiles) {
            if (!(file.exists() && file.isFile())) {
                throw new IllegalArgumentException("Invalid input file: " + file.getAbsolutePath());
            }
        }
    }

    private SSMLWriter configureSsmlWriter() {
        final SSMLWriter.Builder builder = SSMLWriter.builder();

        if (this.voice != null && !this.voice.isBlank()) {
            builder.withVoice(this.voice);
        }

        return builder.build();
    }

    private static String removeFileExtension(String fileName) {
        final int index = fileName.lastIndexOf('.');

        if (index >= 0) {
            return fileName.substring(0, index);
        } else {
            return fileName;
        }
    }
}