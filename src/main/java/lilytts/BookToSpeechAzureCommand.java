package lilytts;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.mpatric.mp3agic.ID3v24Tag;

import lilytts.content.ChapterTitleContent;
import lilytts.parsing.ContentParser;
import lilytts.parsing.text.TextContentParser;
import lilytts.processing.MetadataContext;
import lilytts.processing.MetadataGenerator;
import lilytts.processing.TextFileProcessor;
import lilytts.ssml.SSMLWriter;
import lilytts.synthesis.AzureCostEstimator;
import lilytts.synthesis.AzureSynthesizer;
import lilytts.synthesis.AzureVoice;
import lilytts.synthesis.CostEstimator;
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

    @Option(names = { "--prosody" })
    private int prosodyRate = 0;

    @Option(names = { "--pitch" })
    private int pitch = 0;

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

    @Option(names = { "--pretend", "-n" } )
    private boolean pretend = false;

    @Option(names = { "--ignore" } )
    private List<File> ignoreFiles = Collections.emptyList();

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();

        final ContentParser contentParser = TextContentParser.builder().build();
        final SSMLWriter ssmlWriter = configureSsmlWriter();
        final SpeechSynthesizer synthesizer = AzureSynthesizer.fromSubscription("Azure speech service", subscriptionKey, serviceRegion);
        final CostEstimator azureCostEstimator = new AzureCostEstimator();

        final byte[] coverImageBytes = Files.readAllBytes(coverImageFile.toPath());
        final String coverImageMimeType = Files.probeContentType(coverImageFile.toPath());

        final MetadataGenerator metadataGenerator = new MetadataGenerator() {
            public ID3v24Tag generateMetadata(MetadataContext context) {
                final String chapterTitle = context.getContent().stream()
                    .filter(x -> x instanceof ChapterTitleContent)
                    .map(x -> ((ChapterTitleContent) x).getContent())
                    .findFirst()
                    .orElseGet(() -> StringUtil.removeFileExtension(context.getSourceFile().getName()));

                final ID3v24Tag metadata = new ID3v24Tag();
                metadata.setArtist(authorName);
                metadata.setAlbum(bookTitle);
                metadata.setTitle(chapterTitle);
                metadata.setTrack(Integer.toString(context.getFileIndex() + 1));
                metadata.setAlbumImage(coverImageBytes, coverImageMimeType);
                metadata.setYear(bookYear);

                return metadata;
            }
        };

        final List<File> chapterFiles = findChapterFiles();
        sortChapterFiles(chapterFiles);

        Predicate<File> fileFilter;

        if (this.pretend) {
            fileFilter = (file) -> false;
        } else if (this.onlyFile != null) {
            if (!chapterFiles.stream().anyMatch(x -> x.equals(this.onlyFile))) {
                throw new IllegalArgumentException("File does not appear in the list of chapters to convert: " + this.onlyFile.getPath());
            }

            fileFilter = (file) -> this.onlyFile == null || this.onlyFile.equals(file);
        } else {
            fileFilter = (file) -> true;
        }

        final TextFileProcessor fileProcessor = new TextFileProcessor(synthesizer, contentParser, ssmlWriter, metadataGenerator, azureCostEstimator);
        fileProcessor.convertTextFiles(chapterFiles, outputDirectory, fileFilter);

        return 0;
    }

    private List<File> findChapterFiles() {
        return Arrays.stream(inputDirectory.listFiles(x -> x.getName().endsWith(".txt")))
            .filter(file -> !this.ignoreFiles.stream().anyMatch(ignored -> ignored.getAbsoluteFile().equals(file.getAbsoluteFile())))
            .collect(Collectors.toList());
    }

    private void sortChapterFiles(List<File> chapterFiles) {
        final Map<String, Integer> weightingsByName = Map.of(
            "front matter", -4,
            "preface", -3,
            "prologue", -2,
            "introduction", -1,
            "conclusion", 1,
            "postscript", 2
        );

        chapterFiles.sort((file1, file2) -> {
            final String file1Key = StringUtil.removeFileExtension(file1.getName()).toLowerCase();
            final String file2Key = StringUtil.removeFileExtension(file2.getName()).toLowerCase();

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
        
        if (this.prosodyRate != 0) {
            builder = builder.withProsodyRate(formatPercent(this.prosodyRate));
        }

        if (this.pitch != 0) {
            builder = builder.withPitch(formatPercent(this.pitch));
        }
        
        return builder.build();
    }

    private static String formatPercent(int value) {
        return String.format(Locale.ENGLISH, "%d%%", value);
    }

    private void validateCommandLineParameters() {
        if (!(inputDirectory.exists() && inputDirectory.isDirectory())) {
            throw new IllegalArgumentException("Invalid input directory: " + inputDirectory.getAbsolutePath());
        }

        if (!(outputDirectory.exists() && outputDirectory.isDirectory())) {
            throw new IllegalArgumentException("Invalid output directory: " + outputDirectory.getAbsolutePath());
        }

        if (!(coverImageFile.exists() && coverImageFile.isFile())) {
            throw new IllegalArgumentException("Invalid cover image file: " + coverImageFile.getAbsolutePath());
        }
    }
}