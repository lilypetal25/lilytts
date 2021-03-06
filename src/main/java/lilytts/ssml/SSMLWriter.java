package lilytts.ssml;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import lilytts.content.ChapterEndContent;
import lilytts.content.ChapterTitleContent;
import lilytts.content.ContentItem;
import lilytts.content.ParagraphContent;
import lilytts.content.SectionBreakContent;

public class SSMLWriter {
    public static class Builder {
        private boolean writeVoiceElement = false;
        private String voiceName = null;

        public Builder withVoice(String voiceName) {
            this.writeVoiceElement = true;
            this.voiceName = voiceName;
            return this;
        }

        public SSMLWriter build() {
            return new SSMLWriter(writeVoiceElement, voiceName);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final boolean writeVoiceElement;
    private final String voiceName;

    private SSMLWriter(final boolean writeVoiceElement, final String voiceName) {
        this.writeVoiceElement = writeVoiceElement;
        this.voiceName = voiceName;
    }

    public void writeSSML(Iterable<ContentItem> content, XMLStreamWriter out) throws SSMLWritingException {
        try {
            out.writeStartDocument();
            out.writeStartElement("speak");
            out.writeAttribute("version", "1.0");
            out.writeDefaultNamespace("http://www.w3.org/2001/10/synthesis");
            out.writeAttribute("xml", W3C_XML_SCHEMA_NS_URI, "lang", "en-US");
            
            if (this.writeVoiceElement) {
                out.writeStartElement("voice");
                out.writeAttribute("name", this.voiceName);
            }

            // TODO: This would be better as a visitor pattern.
            for (ContentItem item : content) {
                if (item instanceof ChapterTitleContent) {
                    writeChapterTitle(out, (ChapterTitleContent)item);
                } else if (item instanceof ParagraphContent) {
                    writeParagraph(out, (ParagraphContent)item);
                } else if (item instanceof SectionBreakContent) {
                    writeSectionBreak(out, (SectionBreakContent)item);
                } else if (item instanceof ChapterEndContent) {
                    writeChapterEnd(out, (ChapterEndContent)item);
                } else {
                    throw new IllegalArgumentException("Unknown content item type: " + item.getClass().getSimpleName());
                }
            }

            if (this.writeVoiceElement) {
                out.writeEndElement();
            }

            out.writeEndElement();
            out.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new SSMLWritingException("An I/O error occurred while writing SSML: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new SSMLWritingException("An unexpected error occurred while writing SSML: " + e.getMessage(), e);
        }
    }

    private void writeChapterTitle(XMLStreamWriter out, ChapterTitleContent item) throws XMLStreamException {
        writeBreak(out, "2s");

        out.writeStartElement("p");
        out.writeCharacters(item.getContent());
        out.writeEndElement();

        writeBreak(out, "1s");
    }

    private void writeParagraph(XMLStreamWriter out, ParagraphContent item) throws XMLStreamException {
        out.writeStartElement("p");
        out.writeCharacters(item.getContent());
        out.writeEndElement();
    }

    private void writeSectionBreak(XMLStreamWriter out, SectionBreakContent item) throws XMLStreamException {
        writeBreak(out, "2s");

        if (!item.getSectionTitle().isBlank()) {
            out.writeStartElement("p");
            out.writeCharacters(item.getSectionTitle());
            out.writeEndElement();

            writeBreak(out, "1s");
        }
    }

    private void writeChapterEnd(XMLStreamWriter out, ChapterEndContent item) throws XMLStreamException {
        writeBreak(out, "2s");
    }

    private void writeBreak(XMLStreamWriter out, String time) throws XMLStreamException {
        out.writeStartElement("break");
        out.writeAttribute("time", time);
        out.writeEndElement();
    }
}
