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
import java.util.List;
import java.util.function.Predicate;

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

public class TextFileProcessor {
    private final SpeechSynthesizer speechSynthesizer;
    private final ContentParser contentParser;
    private final ContentSplitter splitter;
    private final SSMLWriter ssmlWriter;
    private final XMLOutputFactory xmlOutputFactory;
    private final MetadataGenerator metadataGenerator;
    private final CostEstimator costEstimator;
    private boolean verbose = true;

    public TextFileProcessor(SpeechSynthesizer speechSynthesizer, ContentParser contentParser, ContentSplitter splitter, SSMLWriter ssmlWriter, MetadataGenerator metadataGenerator, CostEstimator costEstimator) {
        this.speechSynthesizer = speechSynthesizer;
        this.contentParser = contentParser;
        this.splitter = splitter;
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

        int partsProcessed = -1;
        double totalEstimatedCost = 0.0;

        for (File textFile : textFiles) {
            final FileReader inputStream = new FileReader(textFile);
            final List<ContentItem> content = this.contentParser.readContent(inputStream);
            final List<List<ContentItem>> parts = splitter.splitContent(content);

            if (parts.size() > 1) {
                verboseOut.printf("Converting file %d of %d to speech as %s part(s): %s%n",
                    textFiles.indexOf(textFile) + 1,
                    textFiles.size(),
                    parts.size(),
                    textFile.getName());
            } else {
                verboseOut.printf("Converting file %d of %d to speech: %s%n",
                    textFiles.indexOf(textFile) + 1,
                    textFiles.size(),
                    textFile.getName());
            }

            for (int i = 0; i < parts.size(); i++) {
                partsProcessed++;

                final String fileNameWithoutExtension = StringUtil.removeFileExtension(textFile.getName());
                final String outputFileName = parts.size() > 1
                        ? fileNameWithoutExtension + " (Part " + (i + 1) + ").mp3"
                        : fileNameWithoutExtension + ".mp3";
                final File outputFile = new File(targetFolder, outputFileName);

                final StringWriter ssmlStringWriter = new StringWriter();
                ssmlWriter.writeSSML(parts.get(i), xmlOutputFactory.createXMLStreamWriter(ssmlStringWriter));

                totalEstimatedCost += costEstimator.getEstimatedCost(ssmlStringWriter.toString());

                if (outputFile.exists() && outputFile.length() > 0) {
                    verboseOut.printf("  => Skipping file because it already exists: %s%n", outputFile.getName());
                    continue;
                }

                if (!fileFilter.test(textFile)) {
                    verboseOut.printf("  => Skipping file.%n", outputFile.getName());
                    continue;
                }

                // Ensure target folder exists now that we've found at least one file to convert.
                if (!targetFolder.exists() && !targetFolder.mkdirs()) {
                    throw new RuntimeException("Unable to create directory: " + targetFolder.getPath());
                }

                final File tempOutputFile = new File(targetFolder, StringUtil.removeFileExtension(outputFileName) + " audio.mp3");
                final String ssml = ssmlStringWriter.toString();

                try (ProgressBar progressBar = makeProgressBar(outputFile, ssml.length())) {
                    speechSynthesizer.synthesizeSsmlToFile(ssml, tempOutputFile.getAbsolutePath(), progress -> {
                        progressBar.setExtraMessage(progress.getMessage());
                        progressBar.stepTo(progress.getCurrentProgress());
                        progressBar.maxHint(progress.getMaxProgress());
                    });
                }

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

                tempOutputFile.delete();
                verboseOut.printf("  => Saved audio to file: %s%n", outputFile.getName());
            }
        }

        final DecimalFormat costFormatter = new DecimalFormat("$######0.00");
        System.out.printf("Estimated cost: %s%n", costFormatter.format(totalEstimatedCost));
    }

    private static ProgressBar makeProgressBar(File outputFile, long initialMax) {
        final ProgressBar progressBar = new ProgressBarBuilder()
                    .setTaskName(outputFile.getName())
                    .clearDisplayOnFinish()
                    .continuousUpdate()
                    .hideETA()
                    .setInitialMax(initialMax)
                    .build();

        progressBar.stepTo(0);
        return progressBar;
    }
}
