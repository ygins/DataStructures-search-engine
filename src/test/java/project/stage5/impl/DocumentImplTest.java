package project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import project.util.SomeDocuments;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentImplTest {
    private final SomeDocuments docs = new SomeDocuments();


    @Test
    void getTextWorksForTextDoc() {
        assertEquals(docs.getHelloTextString(), docs.helloTextDocument().getDocumentTxt());
    }

    @Test
    void getTextNullForBinaryDoc() {
        assertNull(docs.binaryDataDocument().getDocumentTxt());
    }

    @Test
    void getBytesWorksForByteDoc() {
        assertEquals(docs.getBinaryDocData(), docs.binaryDataDocument().getDocumentBinaryData());
    }

    @Test
    void getBytesDoesntWorkForStringDoc() {
        assertNull(docs.helloTextDocument().getDocumentBinaryData());
    }

    @SuppressWarnings("all")
    @Test
    void nullParamsThrowExceptionString() {
        Executable nullUri = () -> {
            new DocumentImpl(null, docs.getHelloTextString(),null);
        };
        Executable nullText = () -> {
            new DocumentImpl(docs.getHelloTextUri(), (String) null,null);
        };
        Executable both = () -> {
            new DocumentImpl(null, (String) null,null);
        };
        assertThrows(IllegalArgumentException.class, nullUri);
        assertThrows(IllegalArgumentException.class, nullText);
        assertThrows(IllegalArgumentException.class, both);
    }

    @SuppressWarnings("all")
    @Test
    void nullParamsThrowExceptionByte() {
        Executable nullUri = () -> {
            new DocumentImpl(null, docs.getBinaryDocData());
        };
        Executable nullData = () -> {
            new DocumentImpl(docs.getHelloTextUri(), (byte[]) null);
        };
        Executable both = () -> {
            new DocumentImpl(null, (byte[]) null);
        };
        assertThrows(IllegalArgumentException.class, nullUri);
        assertThrows(IllegalArgumentException.class, nullData);
        assertThrows(IllegalArgumentException.class, both);
    }

    @Test
    void getURIStringDoc() {
        assertEquals(docs.getHelloTextUri(), docs.helloTextDocument().getKey());
    }

    @Test
    void getURIDataDoc() {
        assertEquals(docs.getBinaryDataUri(), docs.binaryDataDocument().getKey());
    }

    @Test
    void setWordMap(){
        Map<String, Integer> words = Map.of("one", 1, "two", 2, "three", 3, "four", 4);
        Document doc1= new DocumentImpl(URI.create("one/two"),"one two two three three three four four four four", words);
        assertEquals(words,doc1.getWordMap());
        doc1.setWordMap(new HashMap<>());
        assertEquals(new HashMap<>(),doc1.getWordMap());
    }
}
