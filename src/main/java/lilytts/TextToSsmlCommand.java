package lilytts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.stream.XMLOutputFactory;

import content.ContentItem;
import parsing.ContentParser;
import parsing.text.TextContentParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import ssml.SSMLWriter;

@Command(name = "text-to-ssml")
class TextToSsmlCommand implements Callable<Integer> {
    @Parameters(index = "0")
    private File outputDirectory;

    @Parameters(index = "1")
    private List<File> inputFiles;

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();

        final ContentParser contentParser = TextContentParser.builder().build();
        final SSMLWriter ssmlWriter = new SSMLWriter();
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();

        for (File inputFile : inputFiles) {
            final String outputFileName = removeFileExtension(inputFile.getName()) + ".xml";

            final FileReader inputStream = new FileReader(inputFile);
            FileOutputStream outputStream = new FileOutputStream(new File(outputDirectory, outputFileName));

            List<ContentItem> content = contentParser.readContent(inputStream);
            ssmlWriter.writeSSML(content, xmlOutputFactory.createXMLStreamWriter(outputStream));
        }

        return 0;
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

    private static String removeFileExtension(String fileName) {
        final int index = fileName.lastIndexOf('.');

        if (index >= 0) {
            return fileName.substring(0, index);
        } else {
            return fileName;
        }
    }
}