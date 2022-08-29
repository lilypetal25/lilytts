package lilytts;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;


import com.mpatric.mp3agic.ID3v24Tag;

import lilytts.content.ChapterTitleContent;
import lilytts.parsing.ContentParser;
import lilytts.parsing.text.TextContentParser;
import lilytts.processing.ContentSplitter;
import lilytts.processing.MetadataContext;
import lilytts.processing.MetadataGenerator;
import lilytts.processing.TextFileProcessor;
import lilytts.ssml.SSMLWriter;
import lilytts.synthesis.AzureSynthesizer;
import lilytts.synthesis.AzureVoice;
import lilytts.synthesis.SpeechSynthesizer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "book-to-speech-azure")
public class BookToSpeechAzureCommand implements Callable<Integer> {
    @Parameters(index = "0")
    private File outputDirectory;

    @Parameters(index = "1..*")
    private List<File> inputFiles;

    @Option(names = { "--subscriptionKey" }, required = true)
    private String subscriptionKey;
    
    @Option(names = { "--serviceRegion" }, required = true)
    private String serviceRegion;

    @Option(names = { "--voice" })
    private AzureVoice voice = AzureVoice.Jenny;

    @Option(names = { "--maxPartCharacters" })
    private int maxPartCharacters = 7500;

    @Option(names = { "--prosodyRate" })
    private int prosodyRate = 0;

    @Option(names = { "--author" }, required = true)
    private String authorName;

    @Option(names = { "--bookTitle" }, required = true)
    private String bookTitle;

    @Option(names = { "--publishedYear" }, required = true)
    private String bookYear;

    @Option(names = { "--cover" }, required = true)
    private File coverImageFile;

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();

        final ContentParser contentParser = TextContentParser.builder().build();
        final SSMLWriter ssmlWriter = configureSsmlWriter();
        final ContentSplitter splitter = configureSplitter();
        final SpeechSynthesizer synthesizer = AzureSynthesizer.fromSubscription(subscriptionKey, serviceRegion);

        final byte[] coverImageBytes = Files.readAllBytes(coverImageFile.toPath());
        final String coverImageMimeType = Files.probeContentType(coverImageFile.toPath());

        final MetadataGenerator metadataGenerator = new MetadataGenerator() {
            public ID3v24Tag generateMetadata(MetadataContext context) {
                final String chapterTitle = context.getContent().stream()
                    .filter(x -> x instanceof ChapterTitleContent)
                    .map(x -> ((ChapterTitleContent) x).getContent())
                    .findFirst()
                    .orElseGet(() -> removeFileExtension(context.getSourceFile().getName()));

                final ID3v24Tag metadata = new ID3v24Tag();
                metadata.setArtist(authorName);
                metadata.setAlbum(bookTitle);
                metadata.setTitle(chapterTitle);
                metadata.setTrack(Integer.toString(context.getTotalProcessedParts() + 1));
                metadata.setAlbumImage(coverImageBytes, coverImageMimeType);

                return metadata;
            }
        };

        final TextFileProcessor fileProcessor = new TextFileProcessor(synthesizer, contentParser, splitter, ssmlWriter, metadataGenerator);
        fileProcessor.convertTextFiles(inputFiles, outputDirectory);

        return 0;
    }

    private SSMLWriter configureSsmlWriter() {
        SSMLWriter.Builder builder = SSMLWriter.builder()
            .withVoice(voice.getVoiceName());
        
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