package lilytts;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

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
    private File inputDirectory;

    @Parameters(index = "1")
    private File outputDirectory;

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

    @Option(names = { "--only" } )
    private File onlyFile = null;

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

        final List<File> chapterFiles = findChapterFiles();
        sortChapterFiles(chapterFiles);

        if (this.onlyFile != null && !chapterFiles.stream().anyMatch(x -> x.equals(this.onlyFile))) {
            throw new IllegalArgumentException("File does not appear in the list of chapters to convert: " + this.onlyFile.getPath());
        }

        final Predicate<File> fileFilter = (file) -> this.onlyFile == null || this.onlyFile.equals(file);

        final TextFileProcessor fileProcessor = new TextFileProcessor(synthesizer, contentParser, splitter, ssmlWriter, metadataGenerator);
        fileProcessor.convertTextFiles(chapterFiles, outputDirectory, fileFilter);

        return 0;
    }

    private List<File> findChapterFiles() {
        return Arrays.asList(inputDirectory.listFiles(x -> x.getName().endsWith(".txt")));
    }

    private void sortChapterFiles(List<File> chapterFiles) {
        final Map<String, Integer> weightingsByName = Map.of(
            "front matter", -3,
            "introduction", -2,
            "preface", -1,
            "conclusion", 1,
            "postscript", 2
        );

        chapterFiles.sort((file1, file2) -> {
            final String file1Key = removeFileExtension(file1.getName()).toLowerCase();
            final String file2Key = removeFileExtension(file2.getName()).toLowerCase();

            final int file1Weighting = weightingsByName.getOrDefault(file1Key, 0);
            final int file2Weighting = weightingsByName.getOrDefault(file2Key, 0);

            if (file1Weighting != file2Weighting) {
                return Integer.compare(file1Weighting, file2Weighting);
            }

            return file1.getName().compareTo(file2.getName());
        });
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
        
        if (!(inputDirectory.exists() && inputDirectory.isFile())) {
            throw new IllegalArgumentException("Invalid input directory: " + inputDirectory.getAbsolutePath());
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