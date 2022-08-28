package lilytts.processing;

import com.mpatric.mp3agic.ID3v24Tag;

public interface MetadataGenerator {
    public ID3v24Tag generateMetadata(MetadataContext context);
}