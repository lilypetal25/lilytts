package lilytts.yaml;

import java.util.List;

public class TextToSpeechConfig {
    private List<AzureSpeechConnection> azureConnections;

    public List<AzureSpeechConnection> getAzureConnections() {
        return azureConnections;
    }

    public void setAzureConnections(List<AzureSpeechConnection> azureConnections) {
        this.azureConnections = azureConnections;
    }    
}
