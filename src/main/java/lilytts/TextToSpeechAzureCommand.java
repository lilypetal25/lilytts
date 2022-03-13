package lilytts;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.stream.XMLOutputFactory;

import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import lilytts.content.ContentItem;
import lilytts.parsing.ContentParser;
import lilytts.parsing.text.TextContentParser;
import lilytts.processing.ContentSplitter;
import lilytts.ssml.SSMLWriter;
import lilytts.synthesis.AzureVoice;
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

    @Override
    public Integer call() throws Exception {
        validateCommandLineParameters();

        final ContentParser contentParser = TextContentParser.builder().build();
        final SSMLWriter ssmlWriter = SSMLWriter.builder().withVoice(voice.getVoiceName()).build();
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        final ContentSplitter splitter = configureSplitter();

        final SpeechConfig config = SpeechConfig.fromSubscription(subscriptionKey, serviceRegion);
        config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Audio48Khz192KBitRateMonoMp3);

        for (File inputFile : inputFiles) {
            final FileReader inputStream = new FileReader(inputFile);

            final List<ContentItem> content = contentParser.readContent(inputStream);
            final List<List<ContentItem>> parts = splitter.splitContent(content);

            System.out.printf("Converting input file '%s' to speech as %f part(s).\n",
                inputFile.getName(),
                parts.size());

            for (int i = 0; i < parts.size(); i++) {
                final String outputFileName = parts.size() > 1 ?
                    removeFileExtension(inputFile.getName()) + " (Part " + (i+1) + ").mp3" :
                    removeFileExtension(inputFile.getName()) + ".mp3";
                final File outputFile = new File(outputDirectory, outputFileName);

                final StringWriter ssmlStringWriter = new StringWriter();

                ssmlWriter.writeSSML(parts.get(i), xmlOutputFactory.createXMLStreamWriter(ssmlStringWriter));

                final AudioConfig fileOutput = AudioConfig.fromWavFileOutput(outputFile.getAbsolutePath());
                final SpeechSynthesisResult result;

                try (SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, fileOutput)) {
                    result = synthesizer.SpeakSsml(ssmlStringWriter.toString());
                }

                // Checks result.
                if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                    System.out.println("Audio was saved to " + outputFile.getAbsolutePath());
                } else if (result.getReason() == ResultReason.Canceled) {
                    SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(result);
                    System.out.println("CANCELED: Reason=" + cancellation.getReason());

                    if (cancellation.getReason() == CancellationReason.Error) {
                        System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                        System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                    }

                    // Stop execution.
                    break;
                } else {
                    throw new Exception("Unexpected result reason: " + result.getReason());
                }
            }
        }

        return 0;
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