package lilytts.progress;

@FunctionalInterface
public interface ProgressListener {
    void onProgress(ProgressEvent event);
}
