package lilytts.synthesis;

public enum AzureVoiceStyle {
    General(""),
    Newscast("newscast");

    private final String styleIdentifier;

    AzureVoiceStyle(String styleIdentifier) {
        this.styleIdentifier = styleIdentifier;
    }

    public String getStyleIdentifier() {
        return this.styleIdentifier;
    }
}
