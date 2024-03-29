package lilytts.ssml;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import lilytts.content.ArticlePublisherContent;
import lilytts.content.ChapterEndContent;
import lilytts.content.ChapterTitleContent;
import lilytts.content.ContentItem;
import lilytts.content.ParagraphContent;
import lilytts.content.SectionBreakContent;
import static lilytts.StringUtil.nullOrEmpty;

public class SSMLWriter {
    public static class Builder {
        private boolean writeVoiceElement = false;
        private String voiceName = null;
        private String voiceStyle = null;
        private String prosodyRate = null;
        private String pitch = null;

        public Builder withVoice(String voiceName) {
            this.writeVoiceElement = true;
            this.voiceName = voiceName;
            return this;
        }

        public Builder withVoiceStyle(String voiceStyle) {
            this.voiceStyle = voiceStyle;
            return this;
        }

        public Builder withProsodyRate(String prosodyRate) {
            this.prosodyRate = prosodyRate;
            return this;
        }

        public Builder withPitch(String pitch) {
            this.pitch = pitch;
            return this;
        }

        public SSMLWriter build() {
            return new SSMLWriter(writeVoiceElement, voiceName, voiceStyle, prosodyRate, pitch);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final boolean writeVoiceElement;
    private final String voiceName;
    private final String voiceStyle;
    private final String prosodyRate;
    private final String pitch;

    private SSMLWriter(
        final boolean writeVoiceElement,
        final String voiceName,
        final String voiceStyle,
        final String prosodyRate,
        final String pitch) {
        this.writeVoiceElement = writeVoiceElement;
        this.voiceName = voiceName;
        this.voiceStyle = voiceStyle;
        this.prosodyRate = prosodyRate;
        this.pitch = pitch;
    }

    public void writeSSML(Iterable<ContentItem> content, XMLStreamWriter out) throws SSMLWritingException {
        try {
            final boolean writeStyleElement = !nullOrEmpty(this.voiceStyle);
            final boolean writeProsodyElement = !nullOrEmpty(this.prosodyRate) || !nullOrEmpty(this.pitch);

            out.writeStartDocument();
            out.writeStartElement("speak");
            out.writeAttribute("version", "1.0");
            out.writeDefaultNamespace("http://www.w3.org/2001/10/synthesis");
            out.writeNamespace("mstts", "http://www.w3.org/2001/mstts");
            out.writeAttribute("xml", W3C_XML_SCHEMA_NS_URI, "lang", "en-US");
            
            if (this.writeVoiceElement) {
                out.writeStartElement("voice");
                out.writeAttribute("name", this.voiceName);
            }

            if (writeStyleElement) {
                out.writeStartElement("http://www.w3.org/2001/mstts", "express-as");
                out.writeAttribute("style", this.voiceStyle);
            }

            if (writeProsodyElement) {
                out.writeStartElement("prosody");

                if (!nullOrEmpty(this.prosodyRate)) {
                    out.writeAttribute("rate", this.prosodyRate);
                }

                if (!nullOrEmpty(this.pitch)) {
                    out.writeAttribute("pitch", this.pitch);
                }
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
                } else if (item instanceof ArticlePublisherContent) {
                    writeArticlePublisher(out, (ArticlePublisherContent)item);
                } else {
                    throw new IllegalArgumentException("Unknown content item type: " + item.getClass().getSimpleName());
                }
            }

            if (writeProsodyElement) {
                out.writeEndElement();
            }

            if (writeStyleElement) {
                out.writeEndElement();
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
    
    private void writeArticlePublisher(XMLStreamWriter out, ArticlePublisherContent item) throws XMLStreamException {
        out.writeStartElement("p");
        out.writeCharacters(item.getContent());
        out.writeEndElement();
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
