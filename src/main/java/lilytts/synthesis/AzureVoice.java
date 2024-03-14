package lilytts.synthesis;

public enum AzureVoice {
    Aria("en-US-AriaNeural"),
    Andrew("en-US-AndrewNeural"),
    Brandon("en-US-BrandonNeural"),
    Christopher("en-US-ChristopherNeural"),
    Cora("en-US-CoraNeural"),
    Eric("en-US-EricNeural"),
    Jenny("en-US-JennyNeural"),
    Libby("en-GB-LibbyNeural"),
    Sara("en-US-SaraNeural"),
    Jacob("en-US-JacobNeural"),
    Jane("en-US-JaneNeural"),
    Steffan("en-US-SteffanNeural"),
    Tony("en-US-TonyNeural"),
    Nancy("en-US-NancyNeural"),
    Roger("en-US-RogerNeural");

    private final String voiceName;

    AzureVoice(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getVoiceName() {
        return voiceName;
    }
}
