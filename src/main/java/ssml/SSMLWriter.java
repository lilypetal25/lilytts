package ssml;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import content.ChapterTitleContent;
import content.ContentItem;
import content.ParagraphContent;
import content.SectionBreakContent;

public class SSMLWriter {
    public SSMLWriter() {
    }

    public void writeSSML(Iterable<ContentItem> content, XMLStreamWriter out) throws SSMLWritingException {
        try {
            out.writeStartDocument();
            out.writeStartElement("speak");
            out.writeAttribute("version", "1.0");
            out.writeDefaultNamespace("http://www.w3.org/2001/10/synthesis");
            out.writeAttribute(W3C_XML_SCHEMA_NS_URI, "lang", "en-US");

            for (ContentItem item : content) {
                if (item instanceof ChapterTitleContent) {
                    writeChapterTitle(out, (ChapterTitleContent)item);
                } else if (item instanceof ParagraphContent) {
                    writeParagraph(out, (ParagraphContent)item);
                } else if (item instanceof SectionBreakContent) {
                    writeSectionBreak(out, (SectionBreakContent)item);
                } else {
                    throw new IllegalArgumentException("Unknown content item type: " + item.getClass().getSimpleName());
                }
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

        writeBreak(out, "2s");
    }

    private void writeParagraph(XMLStreamWriter out, ParagraphContent item) throws XMLStreamException {
        out.writeStartElement("p");
        out.writeCharacters(item.getContent());
        out.writeEndElement();
    }

    private void writeSectionBreak(XMLStreamWriter out, SectionBreakContent item) throws XMLStreamException {
        writeBreak(out, "3s");
    }

    private void writeBreak(XMLStreamWriter out, String time) throws XMLStreamException {
        out.writeStartElement("break");
        out.writeAttribute("time", time);
        out.writeEndElement();
    }
}
