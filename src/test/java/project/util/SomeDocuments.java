package project.util;

import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SomeDocuments {
    private final URI helloTextUri = TestingFiles.createTestingDirectoryURI("/docs/hello.txt");
    private final URI binaryDataUri = TestingFiles.createTestingDirectoryURI("/docs/someBinaryDoc");
    private final URI repeatingWordsURI = TestingFiles.createTestingDirectoryURI("/docs/repeat.txt");

    private final String helloTextString = "Hello World!";
    private final byte[] binaryDocData = "I am a string that will be represented as binary data".getBytes();

    private final String repeatWordsString = "The dog is brown and is short and is a dog";
    private final Map<String, Integer> wordCounts = new HashMap<>();

    private final String prefixDoc1 = "seal sear seak seas olive dog";
    private final String prefixDoc2 = "seam sead do seap dog";

    public SomeDocuments() {
        final Set<String> usedWords = new HashSet<>();
        for (String word : this.repeatWordsString.split(" ")) {
            if (!usedWords.contains(word)) {
                usedWords.add(word);
                wordCounts.merge(word, 1, Integer::sum);
            }
        }
    }

    public DocumentImpl helloTextDocument() {
        return new DocumentImpl(helloTextUri, helloTextString,null);
    }

    public DocumentImpl binaryDataDocument() {
        return new DocumentImpl(binaryDataUri, binaryDocData);
    }

    public DocumentImpl prefixDoc1() {
        return new DocumentImpl(TestingFiles.createTestingDirectoryURI("first/prefix"), prefixDoc1,null);
    }

    public DocumentImpl prefixDoc2() {
        return new DocumentImpl(TestingFiles.createTestingDirectoryURI("second/prefix"), prefixDoc2,null);
    }

    public URI getHelloTextUri() {
        return helloTextUri;
    }

    public URI getBinaryDataUri() {
        return binaryDataUri;
    }

    public String getHelloTextString() {
        return helloTextString;
    }

    public byte[] getBinaryDocData() {
        return binaryDocData;
    }

    public DocumentImpl getRepeatWordsDocument() {
        return new DocumentImpl(repeatingWordsURI, repeatWordsString,null);
    }

    public Map<String, Integer> getWordCounts() {
        return wordCounts;
    }
}
