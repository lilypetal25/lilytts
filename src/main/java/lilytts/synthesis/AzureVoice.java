package lilytts.synthesis;

public enum AzureVoice {
    Aria("en-US-AriaNeural"),
    Christopher("en-US-ChristopherNeural"),
    Jenny("en-US-JennyNeural"),
    Sara("en-US-SaraNeural");

    private final String voiceName;

    AzureVoice(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getVoiceName() {
        return voiceName;
    }
}
