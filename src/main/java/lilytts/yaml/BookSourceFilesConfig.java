package lilytts.yaml;

import java.util.Collections;
import java.util.List;

public class BookSourceFilesConfig {
    private List<String> ignoreFiles = Collections.emptyList();

    public List<String> getIgnoreFiles() {
        return ignoreFiles;
    }

    public void setIgnoreFiles(List<String> ignoreFiles) {
        this.ignoreFiles = ignoreFiles;
    }
}
