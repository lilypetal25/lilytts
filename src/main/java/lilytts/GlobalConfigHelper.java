package lilytts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import lilytts.audio.FFMPEGAudioFileMerger;
import lilytts.ssml.SSMLSplitter;
import lilytts.synthesis.AzureSynthesizer;
import lilytts.synthesis.ChunkingSpeechSynthesizer;
import lilytts.synthesis.CompoundSynthesizer;
import lilytts.synthesis.SpeechSynthesizer;
import lilytts.yaml.TextToSpeechConfig;

public class GlobalConfigHelper {
    public SpeechSynthesizer setupSpeechSynthesizerFromGlobalConfig() throws JsonMappingException, JsonProcessingException, IOException {
        final File yamlFile = new File(getUserHomeDirectory(), ".lilytts.yaml");

        if (!(yamlFile.exists() && !yamlFile.isDirectory())) {
            throw new IllegalArgumentException("Could not find yaml config file at expected path: " + yamlFile.getAbsolutePath());
        }

        final ObjectMapper yamlMapper = YAMLMapper.builder().build();
        TextToSpeechConfig config = yamlMapper.readValue(Files.readString(yamlFile.toPath()), TextToSpeechConfig.class);

        // Validate azure connection info.
        if (config.getAzureConnections() == null || config.getAzureConnections().size() < 1) {
            throw missingYamlParam(yamlFile, "azureConnections");
        }

        config.getAzureConnections().stream().forEach(x -> {
            if (isNullOrEmpty(x.getServiceRegion())) {
                throw missingYamlParam(yamlFile, "azureConnections.serviceRegion");
            }

            if (isNullOrEmpty(x.getSubscriptionKey())) {
                throw missingYamlParam(yamlFile, "azureConnections.subscriptionKey");
            }
        });

        List<AzureSynthesizer> synthesizers = config.getAzureConnections().stream()
            .map(x -> AzureSynthesizer.fromSubscription(x.getDisplayName(), x.getSubscriptionKey(), x.getServiceRegion()))
            .toList();
        
        return new ChunkingSpeechSynthesizer(CompoundSynthesizer.tryInPriorityOrder(synthesizers), new SSMLSplitter(), new FFMPEGAudioFileMerger());
    }

    private static IllegalArgumentException missingYamlParam(File yamlFile, String propertyName) {
        return new IllegalArgumentException("Error in " + yamlFile.getPath() + ": " + propertyName + " is missing or empty.");
    }

    // TODO: Share the various copies of this method.
    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    // TODO: Move this method to a shared location.
    private static File getUserHomeDirectory() {
        return new File(System.getProperty("user.home"));
    }
}
