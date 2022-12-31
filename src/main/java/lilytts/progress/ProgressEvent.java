package lilytts.progress;

public class ProgressEvent {
    private String message;
    private long currentProgress;
    private long maxProgress;

    public ProgressEvent(String synthesisMessage, long currentProgress, long maxProgress) {
        this.message = synthesisMessage;
        this.currentProgress = currentProgress;
        this.maxProgress = maxProgress;
    }

    public String getMessage() {
        return message;
    }
    public long getCurrentProgress() {
        return currentProgress;
    }
    public long getMaxProgress() {
        return maxProgress;
    }
}
