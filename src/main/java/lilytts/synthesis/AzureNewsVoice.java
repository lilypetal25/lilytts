package lilytts.synthesis;

public enum AzureNewsVoice {
    AriaCasual("en-US-AriaNeural", "newscast-casual"),
    AriaFormal("en-US-AriaNeural", "newscast-formal"),
    Jenny("en-US-JennyNeural", "newscast");

    private final String voiceName;
    private final String voiceStyle;

    AzureNewsVoice(String voiceName, String voiceStyle) {
        this.voiceName = voiceName;
        this.voiceStyle = voiceStyle;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public String getVoiceStyle() {
        return voiceStyle;
    }
}
