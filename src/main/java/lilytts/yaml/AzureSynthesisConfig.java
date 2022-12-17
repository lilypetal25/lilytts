package lilytts.yaml;

import lilytts.synthesis.AzureVoice;

public class AzureSynthesisConfig {
    private AzureVoice voice = AzureVoice.Jenny;
    private int maxPartCharacters = 7500;
    private int prosodyRate = 0;
    private int pitch = 0;

    public AzureVoice getVoice() {
        return voice;
    }

    public void setVoice(AzureVoice voice) {
        this.voice = voice;
    }

    public int getMaxPartCharacters() {
        return maxPartCharacters;
    }

    public void setMaxPartCharacters(int maxPartCharacters) {
        this.maxPartCharacters = maxPartCharacters;
    }

    public int getProsodyRate() {
        return prosodyRate;
    }

    public void setProsodyRate(int prosodyRate) {
        this.prosodyRate = prosodyRate;
    }

    public int getPitch() {
        return pitch;
    }
    
    public void setPitch(int pitch) {
        this.pitch = pitch;
    }
}
