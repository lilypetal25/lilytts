package lilytts.ssml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class SSMLSplitter {
    private static final String[] WRAPPER_ELEMENT_NAMES = new String[] { "speak", "voice" };
    private static final int DEFAULT_MAX_CHUNK_WEIGHT = 7000;

    private final int maxChunkWeight;

    public SSMLSplitter() {
        this(DEFAULT_MAX_CHUNK_WEIGHT);
    }

    public SSMLSplitter(int maxChunkWeight) {
        this.maxChunkWeight = maxChunkWeight;
    }

    public List<String> splitSSML(String ssml) {
        try {
            final XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(new StringReader(ssml));
            final List<XMLEvent> wrapperEventStack = new ArrayList<>();
            final List<String> chunks = new ArrayList<>();

            // Start writing the SSML contents to a chunk.
            int currentWeight = 0;
            StringWriter chunkWriter = new StringWriter();
            XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(chunkWriter);

            while(reader.hasNext()) {
                // Write any wrappers we've already encountered.
                writeEvents(writer, wrapperEventStack);

                XMLEvent nextTag = reader.nextTag();
            
                if (nextTag.isStartDocument()) {
                    wrapperEventStack.add(nextTag);
                    writer.add(nextTag);
                    continue;
                } else if (nextTag.isStartElement() && isWrapperElement(nextTag.asStartElement())) {
                    // Write the wrapper element onto the stack and to the output stream.
                    pushWrapperElement(reader, wrapperEventStack, nextTag.asStartElement());
                    continue;
                } else if (nextTag.isEndElement()) {
                    // Write the end tag to the current SSML document and then pop all the events
                    // for the current wrapper off the end of the stack.
                    writer.add(nextTag);
                    popWrapperElement(wrapperEventStack);
                } else if (nextTag.isEndDocument()) {
                    // Stop and return the parts we have so far.
                    break;
                }

                final List<XMLEvent> elementEvents = readElement(reader, nextTag.asStartElement());
                final int elementWeight = getWeight(elementEvents);

                if (currentWeight + elementWeight > this.maxChunkWeight) {
                    // Close the current chunk, writing any necessary end tags.
                    closeXmlDocument(writer);
                    chunks.add(chunkWriter.toString());

                    // Start a new chunk.
                    currentWeight = 0;
                    chunkWriter = new StringWriter();
                    writer = XMLOutputFactory.newInstance().createXMLEventWriter(chunkWriter);

                    // Replay the current set of wrapper elements in the new chunk.
                    //
                    // Note: Passing a stack to the ArrayList constructor results in a list
                    // with the elements in first-in-first-out order.
                    writeEvents(writer, new ArrayList<>(wrapperEventStack));
                }

                writeEvents(writer, elementEvents);
                currentWeight += elementWeight;
            }

            closeXmlDocument(writer);
            chunks.add(chunkWriter.toString());

            return chunks;
        } catch (XMLStreamException|FactoryConfigurationError e) {
            throw new RuntimeException("Unexpected exception while splitting SML into chunks: " + e.getMessage(), e);
        }
    }

    private List<XMLEvent> pushWrapperElement(final XMLEventReader reader, List<XMLEvent> wrapperEventStack, StartElement startElement) throws XMLStreamException {
        final List<XMLEvent> events = new ArrayList<>();

        events.add(startElement);
        wrapperEventStack.add(startElement);

        while (reader.peek().isAttribute()) {
            final XMLEvent next = reader.nextEvent();

            events.add(next);
            wrapperEventStack.add(next);
        }

        return events;
    }

    private void popWrapperElement(List<XMLEvent> wrapperEventStack) {
        while (!wrapperEventStack.get(wrapperEventStack.size()-1).isStartElement()) {
            wrapperEventStack.remove(wrapperEventStack.size()-1);
        }

        wrapperEventStack.remove(wrapperEventStack.size()-1);
    }

    private List<XMLEvent> readElement(final XMLEventReader reader, StartElement startElement) throws XMLStreamException {
        final List<XMLEvent> result = new ArrayList<>();
        result.add(startElement);

        while (!reader.peek().isEndElement()) {
            result.add(reader.nextEvent());
        }

        result.add(reader.nextEvent().asEndElement());
        return result;
    }

    private boolean isWrapperElement(final StartElement startElement) {
        return Arrays.stream(WRAPPER_ELEMENT_NAMES).anyMatch(x -> x.equalsIgnoreCase(startElement.getName().getLocalPart()));
    }

    private int getWeight(List<XMLEvent> events) {
        return events.stream()
            .filter(x -> x.isCharacters())
            .map(x -> x.asCharacters())
            .collect(Collectors.summingInt(x -> x.getData().length()));
    }

    private void writeEvents(XMLEventWriter writer, List<XMLEvent> events) throws XMLStreamException {
        for (XMLEvent event : events) {
            writer.add(event);;
        }
    }

    private void closeXmlDocument(XMLEventWriter writer) throws XMLStreamException
    {
        // TODO: Not sure if we need to write the end element events here.
        writer.flush();
        writer.close();
    }
}
