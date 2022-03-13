package lilytts.synthesis;

public enum AzureVoice {
    Jenny("en-US-JennyNeural"),
    Christopher("en-US-ChristopherNeural"),
    Sara("en-US-SaraNeural");

    private final String voiceName;

    AzureVoice(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getVoiceName() {
        return voiceName;
    }
}
