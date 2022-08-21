package lilytts.synthesis;

import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

public class AzureSynthesizer {
    private final SpeechConfig speechConfig;

    public static AzureSynthesizer fromSubscription(String subscriptionKey, String serviceRegion) {
        SpeechConfig config = SpeechConfig.fromSubscription(subscriptionKey, serviceRegion);
        config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Audio48Khz192KBitRateMonoMp3);

        return new AzureSynthesizer(config);
    }

    private AzureSynthesizer(SpeechConfig speechConfig) {
        this.speechConfig = speechConfig;
    }

    public void synthesizeSsmlToFile(String filePath, String ssml) throws SpeechSynthesisException {
        final AudioConfig fileOutput = AudioConfig.fromWavFileOutput(filePath);
        final SpeechSynthesisResult result;

        try (SpeechSynthesizer synthesizer = new SpeechSynthesizer(this.speechConfig, fileOutput)) {
            result = synthesizer.SpeakSsml(ssml);
        }

        // Checks result.
        if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
            System.out.println("Audio was saved to " + filePath);
        } else if (result.getReason() == ResultReason.Canceled) {
            SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(result);
            System.out.println("Failed to convert " + filePath);
            System.out.println("CANCELED: Reason=" + cancellation.getReason());

            if (cancellation.getReason() == CancellationReason.Error) {
                System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
            }

            // Stop execution.
            throw new SpeechSynthesisException("Encountered a synthesis failure.");
        } else {
            throw new SpeechSynthesisException("Unexpected result reason: " + result.getReason());
        }
    }
}
