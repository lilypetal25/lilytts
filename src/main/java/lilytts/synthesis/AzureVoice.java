package lilytts.synthesis;

public enum AzureVoice {
    Aria("en-US-AriaNeural"),
    Brandon("en-US-BrandonNeural"),
    Christopher("en-US-ChristopherNeural"),
    Cora("en-US-CoraNeural"),
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
