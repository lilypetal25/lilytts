package lilytts.processing;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Predicate;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import lilytts.content.ContentItem;
import lilytts.parsing.ContentParser;
import lilytts.ssml.SSMLWriter;
import lilytts.ssml.SSMLWritingException;
import lilytts.synthesis.SpeechSynthesisException;
import lilytts.synthesis.SpeechSynthesizer;

public class TextFileProcessor {
    private final SpeechSynthesizer speechSynthesizer;
    private final ContentParser contentParser;
    private final ContentSplitter splitter;
    private final SSMLWriter ssmlWriter;
    private final XMLOutputFactory xmlOutputFactory;
    private final MetadataGenerator metadataGenerator;

    public TextFileProcessor(SpeechSynthesizer speechSynthesizer, ContentParser contentParser, ContentSplitter splitter, SSMLWriter ssmlWriter, MetadataGenerator metadataGenerator) {
        this.speechSynthesizer = speechSynthesizer;
        this.contentParser = contentParser;
        this.splitter = splitter;
        this.ssmlWriter = ssmlWriter;
        this.metadataGenerator = metadataGenerator;
        this.xmlOutputFactory = XMLOutputFactory.newFactory();
    }

    public void convertTextFiles(final List<File> textFiles, final File targetFolder) throws SpeechSynthesisException, SSMLWritingException, IOException, XMLStreamException {
        convertTextFiles(textFiles, targetFolder, x -> true);
    }

    public void convertTextFiles(final List<File> textFiles, final File targetFolder, final Predicate<File> fileFilter) throws SpeechSynthesisException, SSMLWritingException, IOException, XMLStreamException {
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            throw new RuntimeException("Unable to create directory: " + targetFolder.getPath());
        }

        int partsProcessed = -1;

        for (File textFile : textFiles) {
            final FileReader inputStream = new FileReader(textFile);
            final List<ContentItem> content = this.contentParser.readContent(inputStream);
            final List<List<ContentItem>> parts = splitter.splitContent(content);

            System.out.printf("Converting file %d of %d to speech as %s part(s): %s%n",
                textFiles.indexOf(textFile) + 1,
                textFiles.size(),
                parts.size(),
                textFile.getName());

            for (int i = 0; i < parts.size(); i++) {
                partsProcessed++;

                final String fileNameWithoutExtension = removeFileExtension(textFile.getName());
                final String outputFileName = parts.size() > 1
                        ? fileNameWithoutExtension + " (Part " + (i + 1) + ").mp3"
                        : fileNameWithoutExtension + ".mp3";
                final File outputFile = new File(targetFolder, outputFileName);

                if (!fileFilter.test(textFile)) {
                    System.out.printf("  => Skipping file.%n", outputFile.getName());
                    continue;
                }

                if (outputFile.exists() && outputFile.length() > 0) {
                    System.out.printf("  => Skipping file because it already exists: %s%n", outputFile.getName());
                    continue;
                }

                final File tempOutputFile = File.createTempFile(fileNameWithoutExtension, ".mp3");
                tempOutputFile.deleteOnExit();

                final StringWriter ssmlStringWriter = new StringWriter();
                ssmlWriter.writeSSML(parts.get(i), xmlOutputFactory.createXMLStreamWriter(ssmlStringWriter));
                speechSynthesizer.synthesizeSsmlToFile(ssmlStringWriter.toString(), tempOutputFile.getAbsolutePath());

                final MetadataContext metadataContext = new MetadataContext();
                metadataContext.setSourceFile(textFile);
                metadataContext.setContent(content);
                metadataContext.setPartsInFile(parts.size());
                metadataContext.setPartIndex(i);
                metadataContext.setTotalProcessedParts(partsProcessed);

                final ID3v24Tag metadata = metadataGenerator.generateMetadata(metadataContext);

                Files.copy(tempOutputFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                try {
                    final Mp3File mp3File = new Mp3File(tempOutputFile.getAbsolutePath());
                    mp3File.setId3v2Tag(metadata);
                    mp3File.save(outputFile.getAbsolutePath());
                } catch (UnsupportedTagException | InvalidDataException | NotSupportedException exception) {
                    throw new RuntimeException("Unexpected error while attempting to write ID3v2 tags to MP3 file: " + exception.getMessage(), exception);
                }

                System.out.printf("  => Saved audio to file: %s%n", outputFile.getName());
            }
        }
    }

    // TODO: Share this method with TextToSpeechAzureCommand.
    private static String removeFileExtension(String fileName) {
        final int index = fileName.lastIndexOf('.');

        if (index >= 0) {
            return fileName.substring(0, index);
        } else {
            return fileName;
        }
    }
}
