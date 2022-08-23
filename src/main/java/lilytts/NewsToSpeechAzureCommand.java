package lilytts;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.xml.stream.XMLOutputFactory;

import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;

import lilytts.content.ChapterTitleContent;
import lilytts.content.ContentItem;
import lilytts.parsing.ContentParser;
import lilytts.parsing.text.TextContentParser;
import lilytts.processing.ContentSplitter;
import lilytts.ssml.SSMLWriter;
import lilytts.synthesis.AzureNewsVoice;
import lilytts.synthesis.AzureSynthesizer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "news-to-speech-azure")
public class NewsToSpeechAzureCommand implements Callable<Integer> {
    private static class Article {
        // TODO: Encapsulate these with getters.
        private final String title;
        private final String fileName;
        private final String publisher;
        private final FileTime savedDate;
        private final List<ContentItem> content;

        public Article(String title, String fileName, String publisher, FileTime savedDate, List<ContentItem> content) {
            this.title = title;
            this.fileName = fileName;
            this.publisher = publisher;
            this.savedDate = savedDate;
            this.content = content;
        }
    }

    @Parameters(index = "0")
    private File inputDirectory;

    @Parameters(index = "1")
    private File outputDirectory;

    @Option(names = { "--subscriptionKey" })
    private String subscriptionKey;

    @Option(names = { "--serviceRegion" })
    private String serviceRegion;

    @Option(names = { "--voice" })
    private AzureNewsVoice voice = AzureNewsVoice.AriaCasual;

    @Option(names = { "--prosodyRate" })
    private int prosodyRate = 10;

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();

        final ContentParser contentParser = TextContentParser.builder().build();
        final SSMLWriter ssmlWriter = configureSsmlWriter();
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        final ContentSplitter splitter = ContentSplitter.builder().withMaxPartCharacters(9000).build();
        final AzureSynthesizer synthesizer = AzureSynthesizer.fromSubscription(subscriptionKey, serviceRegion);

        final String currentDateString = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        final String albumName = "News Deep Dive: " + currentDateString;

        int currentTrackNumber = 1;

        List<Article> articles = fetchArticles(contentParser, splitter);

        Collections.sort(articles, (x, y) -> x.savedDate.compareTo(y.savedDate));

        for (Article article : articles) {
            final List<List<ContentItem>> parts = splitter.splitContent(article.content);

            System.out.printf("Converting article '%s' to speech as %s part(s).\n",
                    article.title,
                    parts.size());

            // TODO: Share this code with TextToSpeechAzureCommand.
            for (int i = 0; i < parts.size(); i++) {
                final String outputFileName = parts.size() > 1
                        ? article.fileName + " (Part " + (i + 1) + ").mp3"
                        : article.fileName + ".mp3";
                final File outputFile = new File(outputDirectory, outputFileName);

                final File tempOutputFile = File.createTempFile(article.fileName, ".mp3");
                
                final String title = parts.size() > 1
                    ? article.title + " (Part " + (i + 1) + ")"
                    : article.title;

                tempOutputFile.deleteOnExit();

                if (outputFile.exists() && outputFile.length() > 0) {
                    System.out.printf("Skipping file '%s' because it already exists.\n", outputFile.getName());
                    continue;
                }

                final StringWriter ssmlStringWriter = new StringWriter();
                ssmlWriter.writeSSML(parts.get(i), xmlOutputFactory.createXMLStreamWriter(ssmlStringWriter));
                synthesizer.synthesizeSsmlToFile(tempOutputFile.getAbsolutePath(), ssmlStringWriter.toString());

                final ID3v24Tag metadata = new ID3v24Tag();
                metadata.setArtist(article.publisher);
                metadata.setAlbumArtist("News Deep Dive");
                metadata.setAlbum(albumName);
                metadata.setTitle(title);
                metadata.setTrack(Integer.toString(currentTrackNumber));

                Files.copy(tempOutputFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                final Mp3File mp3File = new Mp3File(tempOutputFile.getAbsolutePath());
                mp3File.setId3v2Tag(metadata);
                mp3File.save(outputFile.getAbsolutePath());

                currentTrackNumber++;
            }
        }

        return 0;
    }

    private void validateCommandLineParameters() {
        if (!inputDirectory.isDirectory() || !inputDirectory.exists()) {
            throw new IllegalArgumentException("Invalid input directory: " + inputDirectory.getAbsolutePath());
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

    private List<Article> fetchArticles(ContentParser contentParser, ContentSplitter splitter) throws IOException {
        final List<Article> articles = new ArrayList<>();

        for (File publisherDirectory : inputDirectory.listFiles(x -> x.isDirectory())) {
            final String publisherName = publisherDirectory.getName();

            for (File articleTextFile : publisherDirectory.listFiles(x -> x.getName().endsWith(".txt"))) {
                final FileReader inputStream = new FileReader(articleTextFile);
                final List<ContentItem> content = contentParser.readContent(inputStream);
                final FileTime savedDate = (FileTime) Files.getAttribute(articleTextFile.toPath(), "creationTime");
                final String fileName = removeFileExtension(articleTextFile.getName());

                final String articleName = content.stream()
                        .filter(x -> x instanceof ChapterTitleContent)
                        .map(x -> ((ChapterTitleContent) x).getContent())
                        .findFirst()
                        .orElseGet(() -> removeFileExtension(articleTextFile.getName()));

                articles.add(new Article(articleName, fileName, publisherName, savedDate, content));
            }
        }

        return articles;
    }
}
