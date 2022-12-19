package lilytts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import com.mpatric.mp3agic.ID3v24Tag;

import lilytts.content.ArticlePublisherContent;
import lilytts.content.ChapterTitleContent;
import lilytts.parsing.ContentParser;
import lilytts.parsing.text.TextContentParser;
import lilytts.processing.ContentSplitter;
import lilytts.processing.MetadataContext;
import lilytts.processing.MetadataGenerator;
import lilytts.processing.TextFileProcessor;
import lilytts.ssml.SSMLWriter;
import lilytts.synthesis.AzureCostEstimator;
import lilytts.synthesis.AzureNewsVoice;
import lilytts.synthesis.SpeechSynthesizer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static lilytts.StringUtil.substringBefore;

@Command(name = "news-to-speech")
public class NewsToSpeechCommand implements Callable<Integer> {
    @Parameters(index = "0")
    private File inputDirectoryOrFile;

    @Parameters(index = "1")
    private File outputDirectory;

    @Option(names = { "--voice" })
    private AzureNewsVoice voice = AzureNewsVoice.AriaCasual;

    @Option(names = { "--prosodyRate" })
    private int prosodyRate = 10;

    @Option(names = { "--date" })
    private Date date = new Date();

    @Option(names = { "--archive" })
    private File archiveDirectory = null;

    @Option(names = { "--resume" })
    private boolean resume = false;

    @Option(names = { "--albumName" })
    private String albumName = null;

    @Option(names = { "--albumArtist" })
    private String albumArtist = null;

    @Option(names = { "--pretend", "-n" } )
    private boolean pretend = false;

    @Option(names = { "--ignore" } )
    private List<File> ignoreFiles = Collections.emptyList();

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();

        final GlobalConfigHelper configHelper = new GlobalConfigHelper();

        final ContentParser contentParser = TextContentParser.builder().setRecognizeArticlePublisher(true).build();
        final SSMLWriter ssmlWriter = configureSsmlWriter();
        final ContentSplitter splitter = ContentSplitter.builder().withMaxPartCharacters(9000).build();
        final SpeechSynthesizer synthesizer = configHelper.setupSpeechSynthesizerFromGlobalConfig();
        final AzureCostEstimator azureCostEstimator = new AzureCostEstimator();

        final List<File> articleFiles = findArticleFiles();
        final File albumTargetFolder = this.resume ? findAlbumTargetFolderToResume() : findAvailableAlbumTargetFolder();
        final String albumName = this.albumName != null ? this.albumName : albumTargetFolder.getName();

        if (this.resume) {
            System.out.printf("Resuming processing of last album.%n");
        }

        System.out.printf("Album name: %s%n", albumName);
        System.out.printf("Output directory: %s%n", albumTargetFolder.getPath());
        System.out.printf("Found %d articles to convert.%n", articleFiles.size());
        System.out.printf("Speech synthesizer: %s%n", synthesizer.getDisplayName());

        final MetadataGenerator metadataGenerator = new MetadataGenerator() {
            public ID3v24Tag generateMetadata(MetadataContext context) {
                final String articleName = context.getContent().stream()
                    .filter(x -> x instanceof ChapterTitleContent)
                    .map(x -> ((ChapterTitleContent) x).getContent())
                    .findFirst()
                    .orElseGet(() -> removeFileExtension(context.getSourceFile().getName()));

                final String publisher = context.getContent().stream()
                    .filter(x -> x instanceof ArticlePublisherContent)
                    .map(x -> ((ArticlePublisherContent) x).getPublisher())
                    .findFirst()
                    .orElse("");

                final ID3v24Tag metadata = new ID3v24Tag();
                metadata.setArtist(publisher);
                metadata.setAlbumArtist(NewsToSpeechCommand.this.albumArtist != null ? NewsToSpeechCommand.this.albumArtist : "News Deep Dive");
                metadata.setAlbum(albumName);
                metadata.setTitle(articleName);
                metadata.setTrack(Integer.toString(context.getTotalProcessedParts() + 1));

                return metadata;
            }
        };

        final TextFileProcessor fileProcessor = new TextFileProcessor(synthesizer, contentParser, splitter, ssmlWriter, metadataGenerator, azureCostEstimator);
        fileProcessor.convertTextFiles(articleFiles, albumTargetFolder, (file) -> !this.pretend);

        if (this.pretend || this.archiveDirectory == null || articleFiles.isEmpty()) {
            return 0;
        }

        System.out.printf("Archiving articles to folder: %s%n", this.archiveDirectory.getPath());
        final String archiveDateFolderName = new SimpleDateFormat("YYYY-MM-dd").format(this.date);
        
        for (File file : articleFiles) {
            Path articleRelativePath = file.isAbsolute()
                ? this.inputDirectoryOrFile.toPath().relativize(file.toPath())
                : file.toPath();

            Path articleArchiveTargetPath = this.archiveDirectory
                .toPath()
                .resolve(archiveDateFolderName)
                .resolve(articleRelativePath);
            
            File articleTargetFolder = articleArchiveTargetPath.getParent().toFile();

            if (!articleTargetFolder.exists() && !articleTargetFolder.mkdirs()) {
                throw new Exception("Unable to create directory: " + articleTargetFolder.getPath());
            }

            Files.move(file.toPath(), articleArchiveTargetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Done!");
        return 0;
    }

    private File findAlbumTargetFolderToResume() {
        File targetFolder = getAlbumTargetFolder(0);
        int nextUpdateNumber = 1;

        if (!targetFolder.exists()) {
            throw new RuntimeException(String.format("Couldn't find an album to resume. Folder does not exist: %s", targetFolder.toString()));
        }

        File nextTargetFolder = getAlbumTargetFolder(nextUpdateNumber);

        while (nextTargetFolder.exists()) {
            targetFolder = nextTargetFolder;
            nextUpdateNumber++;
            nextTargetFolder = getAlbumTargetFolder(nextUpdateNumber);
        }

        return targetFolder;
    }

    private File findAvailableAlbumTargetFolder() {
        for (int updateNumber = 0; updateNumber < 5; updateNumber++) {
            final File targetFolder = getAlbumTargetFolder(updateNumber);

            if (!targetFolder.exists()) {
                return targetFolder;
            }
        }

        // TODO: Use a more specific exception type here.
        throw new RuntimeException(String.format("All the album names for this date have been taken: %s", formatDateForAlbumName(date)));
    }

    private File getAlbumTargetFolder(int updateNumber) {
        return new File(this.outputDirectory, formatAlbumName(updateNumber));
    }

    private String formatAlbumName(int updateNumber) {
        if (updateNumber < 1) {
            return formatDateForAlbumName(date);
        }

        return String.format(Locale.ENGLISH, "%s (Update %d)", formatDateForAlbumName(date), updateNumber);
    }

    private static String formatDateForAlbumName(Date date) {
        return new SimpleDateFormat("EEEE, MMMM d YYYY").format(date);
    }

    private void validateCommandLineParameters() {
        if (!inputDirectoryOrFile.exists()) {
            throw new IllegalArgumentException("Invalid input directory or file`: " + inputDirectoryOrFile.getAbsolutePath());
        }

        if (!(outputDirectory.exists() && outputDirectory.isDirectory())) {
            throw new IllegalArgumentException("Invalid output directory: " + outputDirectory.getAbsolutePath());
        }
    }

    private SSMLWriter configureSsmlWriter() {
        SSMLWriter.Builder builder = SSMLWriter.builder()
                .withVoice(voice.getVoiceName());

        if (!isNullOrEmpty(this.voice.getVoiceStyle())) {
            builder.withVoiceStyle(this.voice.getVoiceStyle());
        }

        // TODO: Does this handle negative numbers?
        if (this.prosodyRate != 0) {
            builder = builder.withProsodyRate(String.format(Locale.ENGLISH, "%d%%", this.prosodyRate));
        }

        return builder.build();
    }

    // TODO: Share this method with TextToSpeechAzureCommand.
    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
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

    // TODO: Share the logic for enumerating text files with the find-articles command.
    private List<File> findArticleFiles() throws IOException {
        if (this.inputDirectoryOrFile.isDirectory()) {
            System.out.printf("Input directory: %s%n", this.inputDirectoryOrFile.getPath());

            final List<File> results = Arrays.asList(this.inputDirectoryOrFile.listFiles(file -> {
                // Only match *.txt files.
                if (!file.getName().endsWith(".txt")) {
                    return false;
                }
    
                // Ignore files in the ignore list.
                // TODO: Implement a compare file helper method to share the code for comparing files by absolute path.
                if (this.ignoreFiles.stream().anyMatch(ignored -> ignored.getAbsoluteFile().equals(file.getAbsoluteFile()))) {
                    return false;
                }
    
                return true;
            }));

            // Sort files by creation time, from first created to last created.
            Collections.sort(results, (file1, file2) -> {
                try {
                    final FileTime creationTime1 = (FileTime) Files.getAttribute(file1.toPath(), "creationTime");
                    final FileTime creationTime2 = (FileTime) Files.getAttribute(file2.toPath(), "creationTime");
                    return creationTime1.compareTo(creationTime2);
                } catch (IOException e) {
                    throw new RuntimeException("Unexpected error when reading file creation time: " + e.getMessage(), e);
                }
            });

            return results;
        } else {
            System.out.printf("Input file list: %s%n", this.inputDirectoryOrFile.getPath());
            
            List<File> results = Files.readAllLines(this.inputDirectoryOrFile.toPath())
                .stream()
                .map(line -> line.trim())
                .map(line -> substringBefore(line, "#"))
                .filter(line -> !line.isBlank())
                .map(path -> new File(path))
                .toList();

            results.stream()
                .filter(file -> !file.exists() || file.isDirectory())
                .findFirst()
                .ifPresent(file -> {
                    throw new IllegalArgumentException("Invalid input file: " + file.getPath());
                });
            
            return results;
        }
    }
}
