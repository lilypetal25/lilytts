package lilytts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.mpatric.mp3agic.ID3v24Tag;

import lilytts.content.ChapterTitleContent;
import lilytts.parsing.ContentParser;
import lilytts.parsing.text.TextContentParser;
import lilytts.processing.MetadataContext;
import lilytts.processing.MetadataGenerator;
import lilytts.processing.TextFileProcessor;
import lilytts.ssml.SSMLWriter;
import lilytts.synthesis.AzureCostEstimator;
import lilytts.synthesis.AzureVoice;
import lilytts.synthesis.CostEstimator;
import lilytts.synthesis.SpeechSynthesizer;
import lilytts.yaml.AzureSynthesisConfig;
import lilytts.yaml.BookConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "book-to-speech")
public class BookToSpeechCommand implements Callable<Integer> {
    private static final int DEFAULT_PROSODY_RATE = 0;
    private static final int DEFAULT_PITCH = 0;
    private static final AzureVoice DEFAULT_VOICE = AzureVoice.Jenny;

    @Parameters(index = "0")
    private File inputDirectory;

    @Parameters(index = "1")
    private File outputDirectory;

    @Option(names = { "--only" } )
    private File onlyFile = null;

    @Option(names = { "--pretend", "-n" } )
    private boolean pretend = false;

    private AzureVoice voice;
    private int prosodyRate;
    private int pitch;
    private String authorName;
    private String bookTitle;
    private String bookYear;
    private File coverImageFile;
    private List<File> ignoreFiles;

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();
        loadBookConfig();

        final GlobalConfigHelper configHelper = new GlobalConfigHelper();

        final ContentParser contentParser = TextContentParser.builder().build();
        final SSMLWriter ssmlWriter = configureSsmlWriter();
        final SpeechSynthesizer synthesizer = configHelper.setupSpeechSynthesizerFromGlobalConfig();
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
        fileProcessor.setVerbose(this.pretend);
        fileProcessor.convertTextFiles(chapterFiles, outputDirectory, fileFilter);

        return 0;
    }

    private List<File> findChapterFiles() {
        return Arrays.stream(inputDirectory.listFiles(x -> x.getName().endsWith(".txt")))
            .filter(file -> !this.ignoreFiles.stream().anyMatch(ignored -> ignored.getAbsoluteFile().equals(file.getAbsoluteFile())))
            .collect(Collectors.toList());
    }

    private void sortChapterFiles(List<File> chapterFiles) {
        final Comparator<CharSequence> alphaNumComparator = StringUtil.alphaNumComparator();

        final Map<String, Integer> weightingsByName = Map.of(
            "front matter", -6,
            "forward", -5,
            "foreword", -4,
            "preface", -3,
            "introduction", -2,
            "prologue", -1,
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

            return alphaNumComparator.compare(file1.getName(), file2.getName());
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
    }

    private void loadBookConfig() throws JsonMappingException, JsonProcessingException, IOException {
        final File yamlFile = new File(inputDirectory, "book.yaml");

        if (!(yamlFile.exists() && !yamlFile.isDirectory())) {
            throw new IllegalArgumentException("Could not find yaml config file at expected path: " + yamlFile.getAbsolutePath());
        }

        final ObjectMapper yamlMapper = YAMLMapper.builder().build();
        BookConfig config = yamlMapper.readValue(Files.readString(yamlFile.toPath()), BookConfig.class);

        // Parse settings about how to generate the audio for the book.
        final Optional<AzureSynthesisConfig> synthesisConfig = Optional.of(config.getAudio());
        this.voice = synthesisConfig.map(x -> x.getVoice()).orElse(DEFAULT_VOICE);
        this.prosodyRate = synthesisConfig.map(x -> x.getProsodyRate()).orElse(DEFAULT_PROSODY_RATE);
        this.pitch = synthesisConfig.map(x -> x.getPitch()).orElse(DEFAULT_PITCH);

        // Parse info about the book being converted.
        if (config.getMetadata() == null) {
            throw missingYamlParam(yamlFile, "bookInfo");
        }

        if (isNullOrEmpty(config.getMetadata().getCoverImage())) {
            throw missingYamlParam(yamlFile, "bookInfo.coverImage");
        }

        if (isNullOrEmpty(config.getMetadata().getTitle())) {
            throw missingYamlParam(yamlFile, "bookInfo.bookTitle");
        }

        this.authorName = config.getMetadata().getAuthor();
        this.bookTitle = config.getMetadata().getTitle();
        this.bookYear = config.getMetadata().getPublishedYear();
        this.coverImageFile = new File(config.getMetadata().getCoverImage());

        if (!(this.coverImageFile.exists() && this.coverImageFile.isFile())) {
            throw new IllegalArgumentException("Invalid cover image file: " + this.coverImageFile.getAbsolutePath());
        }

        // Parse info about which files contain the audio to generate.
        this.ignoreFiles = Optional.of(config.getFiles())
            .map(x -> x.getIgnoreFiles())
            .orElse(Collections.emptyList())
            .stream()
            .map(path -> new File(yamlFile.getParentFile(), path))
            .toList();
    }

    private static IllegalArgumentException missingYamlParam(File yamlFile, String propertyName) {
        return new IllegalArgumentException("Error in " + yamlFile.getPath() + ": " + propertyName + " is missing or empty.");
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}