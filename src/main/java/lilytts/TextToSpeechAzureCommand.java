package lilytts;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.xml.stream.XMLOutputFactory;

import lilytts.content.ContentItem;
import lilytts.parsing.ContentParser;
import lilytts.parsing.text.TextContentParser;
import lilytts.processing.ContentSplitter;
import lilytts.ssml.SSMLWriter;
import lilytts.synthesis.AzureSynthesizer;
import lilytts.synthesis.AzureVoice;
import lilytts.synthesis.AzureVoiceStyle;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "text-to-speech-azure")
public class TextToSpeechAzureCommand implements Callable<Integer> {
    @Parameters(index = "0")
    private File outputDirectory;

    @Parameters(index = "1..*")
    private List<File> inputFiles;

    @Option(names = { "--subscriptionKey" })
    private String subscriptionKey;
    
    @Option(names = { "--serviceRegion" })
    private String serviceRegion;

    @Option(names = { "--voice" })
    private AzureVoice voice = AzureVoice.Jenny;

    @Option(names = { "--maxPartCharacters" })
    private int maxPartCharacters = 7500;

    @Option(names = { "--skipExistingFiles" })
    private boolean skipExistingFiles = false;

    @Option(names = { "--voiceStyle" })
    private AzureVoiceStyle voiceStyle = AzureVoiceStyle.General;

    @Option(names = { "--prosodyRate" })
    private int prosodyRate = 0;

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();

        final ContentParser contentParser = TextContentParser.builder().build();
        final SSMLWriter ssmlWriter = configureSsmlWriter();
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        final ContentSplitter splitter = configureSplitter();
        final AzureSynthesizer synthesizer = AzureSynthesizer.fromSubscription(subscriptionKey, serviceRegion);

        for (File inputFile : inputFiles) {
            final FileReader inputStream = new FileReader(inputFile);

            final List<ContentItem> content = contentParser.readContent(inputStream);
            final List<List<ContentItem>> parts = splitter.splitContent(content);

            System.out.printf("Converting file %d of %d to speech as %d part(s): %s%n",
                inputFiles.indexOf(inputFile) + 1,
                inputFiles.size(),
                parts.size(),
                inputFile.getName());

            for (int i = 0; i < parts.size(); i++) {
                final String outputFileName = parts.size() > 1 ?
                    removeFileExtension(inputFile.getName()) + " (Part " + (i+1) + ").mp3" :
                    removeFileExtension(inputFile.getName()) + ".mp3";
                final File outputFile = new File(outputDirectory, outputFileName);

                if (this.skipExistingFiles && outputFile.exists() && outputFile.length() > 0) {
                    System.out.printf("  => Skipping file because it already exists:%s%n", outputFile.getName());
                    continue;
                }

                final StringWriter ssmlStringWriter = new StringWriter();
                ssmlWriter.writeSSML(parts.get(i), xmlOutputFactory.createXMLStreamWriter(ssmlStringWriter));
                synthesizer.synthesizeSsmlToFile(outputFile.getAbsolutePath(), ssmlStringWriter.toString());

                System.out.printf("  => Saved audio to file: %s%n", outputFile.getName());
            }
        }

        return 0;
    }

    private SSMLWriter configureSsmlWriter() {
        SSMLWriter.Builder builder = SSMLWriter.builder()
            .withVoice(voice.getVoiceName())
            .withVoiceStyle(this.voiceStyle.getStyleIdentifier());
        
        // TODO: Does this handle negative numbers?
        if (this.prosodyRate != 0) {
            builder = builder.withProsodyRate(String.format(Locale.ENGLISH, "%d%%", this.prosodyRate));
        }
        
        return builder.build();
    }

    private ContentSplitter configureSplitter() {
        return ContentSplitter.builder()
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

    private static String removeFileExtension(String fileName) {
        final int index = fileName.lastIndexOf('.');

        if (index >= 0) {
            return fileName.substring(0, index);
        } else {
            return fileName;
        }
    }
}