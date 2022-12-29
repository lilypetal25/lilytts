package lilytts.synthesis;

import java.util.regex.Pattern;

import com.microsoft.cognitiveservices.speech.CancellationErrorCode;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

public class AzureSynthesizer implements SpeechSynthesizer {
    private static final Pattern REQUEST_THROTTLED_MESSAGE_PATTERN = Pattern.compile(".+The request is throttled because you have exceeded.+", Pattern.CASE_INSENSITIVE);

    private final String displayName;
    private final SpeechConfig speechConfig;

    public static AzureSynthesizer fromSubscription(String displayName, String subscriptionKey, String serviceRegion) {
        SpeechConfig config = SpeechConfig.fromSubscription(subscriptionKey, serviceRegion);
        config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Audio48Khz192KBitRateMonoMp3);

        return new AzureSynthesizer(displayName, config);
    }

    private AzureSynthesizer(String displayName, SpeechConfig speechConfig) {
        this.displayName = displayName;
        this.speechConfig = speechConfig;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void synthesizeSsmlToFile(String ssml, String filePath) throws SpeechSynthesisException {
        final AudioConfig fileOutput = AudioConfig.fromWavFileOutput(filePath);
        final SpeechSynthesisResult result;

        try (com.microsoft.cognitiveservices.speech.SpeechSynthesizer synthesizer =
                new com.microsoft.cognitiveservices.speech.SpeechSynthesizer(this.speechConfig, fileOutput)) {
            result = synthesizer.SpeakSsml(ssml);
        }

        // Checks result.
        if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
            return;
        } else if (result.getReason() == ResultReason.Canceled) {
            SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(result);

            final StringBuilder errorBuilder = new StringBuilder("Encountered a synthesis failure.");
            errorBuilder.append("Failed to convert " + filePath + System.lineSeparator());
            errorBuilder.append("Reason: " + cancellation.getReason() + System.lineSeparator());

            if (cancellation.getReason() == CancellationReason.Error) {
                errorBuilder.append("ErrorCode: " + cancellation.getErrorCode() + System.lineSeparator());
                errorBuilder.append("ErrorDetails: " + cancellation.getErrorDetails() + System.lineSeparator());
            }

            if (isThrottlingError(cancellation)) {
                throw new SpeechSynthesisThrottledException(errorBuilder.toString());
            } else {
                throw new SpeechSynthesisException(errorBuilder.toString());
            }
        } else {
            throw new SpeechSynthesisException("Unexpected result reason: " + result.getReason());
        }
    }

    private static boolean isThrottlingError(SpeechSynthesisCancellationDetails cancellation) {
        return cancellation.getReason() == CancellationReason.Error
            && cancellation.getErrorCode() == CancellationErrorCode.ConnectionFailure
            && REQUEST_THROTTLED_MESSAGE_PATTERN.matcher(cancellation.getErrorDetails()).matches();
    }
}
