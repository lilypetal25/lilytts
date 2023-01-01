package lilytts.processing;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import lilytts.StringUtil;
import lilytts.content.ContentItem;
import lilytts.parsing.ContentParser;
import lilytts.ssml.SSMLWriter;
import lilytts.ssml.SSMLWritingException;
import lilytts.synthesis.CostEstimator;
import lilytts.synthesis.SpeechSynthesisException;
import lilytts.synthesis.SpeechSynthesizer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class TextFileProcessor {
    private static class ParsedTextFile {
        private File inputFile;
        private List<ContentItem> content;
        private String ssml;
        private double estimatedCost;
        private File outputFile;
        private boolean skipped;

        ParsedTextFile(File inputFile, List<ContentItem> content, String ssml, double estimatedCost, File outputFile, boolean skipped) {
            this.inputFile = inputFile;
            this.content = content;
            this.ssml = ssml;
            this.estimatedCost = estimatedCost;
            this.outputFile = outputFile;
            this.skipped = skipped;
        }

        public File getInputFile() {
            return inputFile;
        }

        public List<ContentItem> getContent() {
            return content;
        }

        public String getSsml() {
            return ssml;
        }

        public double getEstimatedCost() {
            return estimatedCost;
        }

        public File getOutputFile() {
            return outputFile;
        }

        public boolean isSkipped() {
            return skipped;
        }
    }

    private final SpeechSynthesizer speechSynthesizer;
    private final ContentParser contentParser;
    private final SSMLWriter ssmlWriter;
    private final XMLOutputFactory xmlOutputFactory;
    private final MetadataGenerator metadataGenerator;
    private final CostEstimator costEstimator;
    private boolean verbose = true;

    public TextFileProcessor(SpeechSynthesizer speechSynthesizer, ContentParser contentParser, SSMLWriter ssmlWriter, MetadataGenerator metadataGenerator, CostEstimator costEstimator) {
        this.speechSynthesizer = speechSynthesizer;
        this.contentParser = contentParser;
        this.ssmlWriter = ssmlWriter;
        this.metadataGenerator = metadataGenerator;
        this.xmlOutputFactory = XMLOutputFactory.newFactory();
        this.costEstimator = costEstimator;
    }

    public void convertTextFiles(final List<File> textFiles, final File targetFolder) throws SpeechSynthesisException, SSMLWritingException, IOException, XMLStreamException {
        convertTextFiles(textFiles, targetFolder, x -> true);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void convertTextFiles(final List<File> textFiles, final File targetFolder, final Predicate<File> fileFilter) throws SpeechSynthesisException, SSMLWritingException, IOException, XMLStreamException {
        // TODO: Encapsulate handling of the print stream.
        final PrintStream verboseOut = this.verbose ? System.out : new PrintStream(OutputStream.nullOutputStream());

        final List<ParsedTextFile> parsedTextFiles = new ArrayList<>(textFiles.size());

        for (File file : textFiles) {
            parsedTextFiles.add(parseTextFile(file, targetFolder, fileFilter));
        }

        double totalEstimatedCost = parsedTextFiles.stream().collect(Collectors.summingDouble(x -> x.getEstimatedCost()));

        final DecimalFormat costFormatter = new DecimalFormat("$######0.00");
        System.out.printf("Estimated cost: %s", costFormatter.format(totalEstimatedCost));

        long currentProgress = 0;
        long maxProgress = parsedTextFiles.stream()
            .filter(x -> !x.skipped)
            .collect(Collectors.summingLong(x -> x.getSsml().length()));

        final int filesToProcess = (int)parsedTextFiles.stream().filter(x -> !x.isSkipped()).count();
        
        if (parsedTextFiles.stream().anyMatch(x -> !x.isSkipped())) {
            // Ensure target folder exists if there are any files to convert.
            if (!targetFolder.exists() && !targetFolder.mkdirs()) {
                throw new RuntimeException("Unable to create directory: " + targetFolder.getPath());
            }
        }

        try (ProgressBar summaryProgressBar = makeProgressBar("Processing " + filesToProcess + " file(s)", maxProgress)) {
            System.out.println("");
            System.out.printf("\033[?25l"); // Hide cursor

            for (ParsedTextFile textFile : parsedTextFiles) {
                final int fileIndex = parsedTextFiles.indexOf(textFile);

                verboseOut.printf("Converting file %d of %d to speech: %s%n",
                    fileIndex + 1,
                    parsedTextFiles.size(),
                    textFile.getInputFile().getName());

                if (textFile.isSkipped()) {
                    verboseOut.printf("  => Skipping file.%n", textFile.getOutputFile().getName());
                    continue;
                }

                summaryProgressBar.stepTo(currentProgress);
                final long progressBeforeFile = currentProgress;
                currentProgress += textFile.getSsml().length();

                if (textFile.getOutputFile().exists() && textFile.getOutputFile().length() > 0) {
                    verboseOut.printf("  => Skipping file because it already exists: %s%n", textFile.getOutputFile().getName());
                    continue;
                }

                final File tempOutputFile = new File(targetFolder, StringUtil.removeFileExtension(textFile.getOutputFile().getName()) + " audio.mp3");

                speechSynthesizer.synthesizeSsmlToFile(textFile.getSsml(), tempOutputFile.getAbsolutePath(), progress -> {
                    printProgressMessage(textFile.getOutputFile().getName(), progress.getMessage());

                    final double percentDone = (double)progress.getCurrentProgress() / progress.getMaxProgress();
                    final long fileProgress = Math.round(textFile.getSsml().length() * percentDone);
                    summaryProgressBar.stepTo(progressBeforeFile + fileProgress);
                });

                final MetadataContext metadataContext = new MetadataContext();
                metadataContext.setSourceFile(textFile.getInputFile());
                metadataContext.setContent(textFile.getContent());
                metadataContext.setFileIndex(fileIndex);

                final ID3v24Tag metadata = metadataGenerator.generateMetadata(metadataContext);

                Files.copy(tempOutputFile.toPath(), textFile.getOutputFile().toPath(), StandardCopyOption.REPLACE_EXISTING);

                try {
                    final Mp3File mp3File = new Mp3File(tempOutputFile.getAbsolutePath());
                    mp3File.setId3v2Tag(metadata);
                    mp3File.save(textFile.getOutputFile().getAbsolutePath());
                } catch (UnsupportedTagException | InvalidDataException | NotSupportedException exception) {
                    throw new RuntimeException("Unexpected error while attempting to write ID3v2 tags to MP3 file: " + exception.getMessage(), exception);
                }

                tempOutputFile.delete();
                verboseOut.printf("  => Saved audio to file: %s%n", textFile.getOutputFile().getName());
            }
        } finally {
            System.out.printf("\r%1$-100s\r", ""); // Clear last progress message.
            System.out.printf("\033[?25h"); // Show cursor again
            System.out.println("Done!");
        }
    }

    private void printProgressMessage(final String taskName, final String progressMessage) {
        String formattedMessage = String.format("%s (%s)", taskName, progressMessage);

        if (formattedMessage.length() > 100) {
            formattedMessage = formattedMessage.substring(0, 101);
        }

        System.out.printf("\r%1$-100s", formattedMessage);
    }

    private ParsedTextFile parseTextFile(File inputFile, final File targetFolder, final Predicate<File> fileFilter) throws IOException, SSMLWritingException, XMLStreamException {
        final FileReader inputStream = new FileReader(inputFile);
        final List<ContentItem> content = this.contentParser.readContent(inputStream);

        final String fileNameWithoutExtension = StringUtil.removeFileExtension(inputFile.getName());
        final File outputFile = new File(targetFolder, fileNameWithoutExtension + ".mp3");

        final StringWriter ssmlStringWriter = new StringWriter();
        ssmlWriter.writeSSML(content, xmlOutputFactory.createXMLStreamWriter(ssmlStringWriter));
        final String ssml = ssmlStringWriter.toString();

        final double estimatedCost = costEstimator.getEstimatedCost(ssml);
        final boolean skipped = !fileFilter.test(inputFile);

        return new ParsedTextFile(inputFile, content, ssml, estimatedCost, outputFile, skipped);
    }

    private static ProgressBar makeProgressBar(String taskName, long initialMax) {
        final ProgressBarBuilder builder = new ProgressBarBuilder()
                    .setTaskName(taskName)
                    .setStyle(ProgressBarStyle.UNICODE_BLOCK)
                    .setInitialMax(initialMax);

        final ProgressBar progressBar = builder.build();
        progressBar.stepTo(0);
        return progressBar;
    }
}
