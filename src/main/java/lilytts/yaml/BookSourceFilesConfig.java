package lilytts.yaml;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class BookSourceFilesConfig {
    private List<File> ignoreFiles = Collections.emptyList();

    public List<File> getIgnoreFiles() {
        return ignoreFiles;
    }

    public void setIgnoreFiles(List<File> ignoreFiles) {
        this.ignoreFiles = ignoreFiles;
    }
}
