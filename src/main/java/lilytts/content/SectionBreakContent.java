package lilytts.content;

public class SectionBreakContent extends ContentItem {
    private String sectionTitle;

    public SectionBreakContent() {
        this("");
    }

    public SectionBreakContent(String sectionTitle) {
        this.sectionTitle = sectionTitle != null ? sectionTitle : "";
    }

    public String getSectionTitle() {
        return this.sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }
}
