package edu.yu.cs.com1320.project.stage5.impl;


import edu.yu.cs.com1320.project.stage5.Document;

import java.net.URI;
import java.util.*;

public class DocumentImpl implements Document {

    private final URI uri;

    private final byte[] data;

    private final String txt;

    private Map<String, Integer> wordCounts;

    private long lastUseTime;

    public DocumentImpl(URI uri, byte[] binaryData) {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be null!");
        }
        if (binaryData == null || binaryData.length == 0) {
            throw new IllegalArgumentException("Binary data cannot be null or blank!");
        }
        this.uri = uri;
        this.data = binaryData;
        this.txt = null;
        this.wordCounts = null;
        this.lastUseTime = 0;
    }
    public DocumentImpl(URI uri, String txt, Map<String, Integer> wordCounts) {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be null!");
        }
        if (txt == null || txt.isEmpty() || txt.isBlank()) {
            throw new IllegalArgumentException("Text cannot be null, empty, nor blank!");
        }
        if(wordCounts == null){
            wordCounts = stripAndWordsToWordCount(txt.split("\\s"));
        }
        this.uri = uri;
        this.txt = txt;
        this.data = null;
        this.lastUseTime = 0;
        this.wordCounts = wordCounts;
    }


    private Map<String, Integer> stripAndWordsToWordCount(String[] words) {
        Map<String, Integer> wordCounts = new HashMap<>();
        for (String word : words) {
            StringBuilder wordBuilder = new StringBuilder();
            for (char c : word.toCharArray()) {
                if (isAlphaNumeric(c)) {
                    wordBuilder.append(c);
                }
            }
            wordCounts.merge(wordBuilder.toString(), 1, Integer::sum);
        }
        return wordCounts;
    }

    @Override
    public String getDocumentTxt() {
        return this.txt;
    }

    @Override
    public byte[] getDocumentBinaryData() {
        return this.data;
    }

    @Override
    public URI getKey() {
        return this.uri;
    }

    @Override
    public int wordCount(String word) {
        if (this.data != null) {//binary doc
            assert this.wordCounts == null;
            return 0;
        } else {
            assert this.wordCounts != null;
            assert this.txt != null;
            return this.wordCounts.getOrDefault(word, 0);
        }
    }

    @Override
    public Set<String> getWords() {
        return this.wordCounts == null ? new HashSet<>() : this.wordCounts.keySet();
    }

    @Override
    public long getLastUseTime() {
        return this.lastUseTime;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.lastUseTime = timeInNanoseconds;
    }

    @Override
    public Map<String, Integer> getWordMap() {
        return new HashMap<>(wordCounts);
    }

    @Override
    public void setWordMap(Map<String, Integer> wordMap) {
        this.wordCounts = wordMap;
    }

    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + (this.txt != null ? txt.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(this.data);
        return Math.abs(result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document)) {
            return false;
        }
        return this.hashCode() == o.hashCode();
    }

    @Override
    public int compareTo(Document o) {
        return Long.compare(this.getLastUseTime(), o.getLastUseTime());
    }

    //copied from TrieImpl, I should ask if I can have utility methods...
    //we can't really, so I'll settle with this for now...
    private boolean inRange(char c, int from, int to) {
        return from <= c && c <= to;
    }

    private boolean isAlphaNumeric(char c) {
        return isLetter(c) || isNumber(c);
    }

    private boolean isLetter(char c) {
        return inRange(c, 65, 90) || inRange(c, 97, 122);
    }

    private boolean isNumber(char c) {
        return inRange(c, 48, 57);
    }

}
