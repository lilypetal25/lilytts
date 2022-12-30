package lilytts.audio;

import java.io.File;
import java.util.List;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

public class JaveAudioFileMerger implements AudioFileMerger {
    public static JaveAudioFileMerger create() {
        return new JaveAudioFileMerger();
    }

    private JaveAudioFileMerger() {
    }

    public void mergeAudioFiles(List<File> inputFiles, File outputFile)
    {
        final Encoder encoder = new Encoder();
        final List<MultimediaObject> inputMultimediaObjects = inputFiles.stream().map(x -> new MultimediaObject(x)).toList();

        // 48khz, 192K bit rate, mono audio
        final AudioAttributes audioAttributes = new AudioAttributes();
        audioAttributes.setBitRate(192000);
        audioAttributes.setChannels(1);
        audioAttributes.setSamplingRate(48000);

        final EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setInputFormat("mp3");
        encodingAttributes.setOutputFormat("mp3");
        encodingAttributes.setAudioAttributes(audioAttributes);

        try {
            encoder.encode(inputMultimediaObjects, outputFile, encodingAttributes);
        } catch (IllegalArgumentException | EncoderException e) {
            throw new RuntimeException("Unexpected error when attempting to merge audio files: " + e.getMessage(), e);
        }
    }
}