package project.util;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class LiteraryWorks {

    private Map<Document, Map<String, Integer>> wordCounts;
    private Map<String, Set<Document>> wordToDocument;
    private Set<String> wordsThatAppearInMultipleDocuments;

    public LiteraryWorks() throws URISyntaxException, IOException {
        wordCounts = new HashMap<>();
        wordToDocument = new HashMap<>();
        readTexts();
        populateDocsWithThisWord();
        wordsThatAppearInMultipleDocuments = populateWordsThatAppearInMultipleDocuments();

    }


    public int getWordCountFor(Document document, String word) {
        return wordCounts.get(document).getOrDefault(word, 0);
    }

    public Set<String> getWordsThatAppearInMultipleDocuments() {
        return wordsThatAppearInMultipleDocuments;
    }

    public Set<Document> search(String word) {
        return wordToDocument.get(word);
    }

    public Set<Document> getDocuments() {
        return wordCounts.keySet();
    }

    public Set<Document> getDocumentsWithPrefix(String prefix) {
        return getDocuments().stream().filter(doc ->
                doc.getWords().stream().anyMatch(word -> word.startsWith(prefix))
        ).collect(Collectors.toSet());
    }

    public Set<Document> getDocumentsWithWord(String word) {
        return getDocuments().stream().filter(doc -> doc.wordCount(word) > 0).collect(Collectors.toSet());
    }

    /**
     * Reads sample texts into wordCounts and allWords
     */
    private void readTexts() throws URISyntaxException, IOException {
        File[] children = getTextsAsFiles();
        for (File literaryWork : children) {
            Map<String, Integer> fileWordCounts = new HashMap<>();
            String text = String.join(" ", Files.readAllLines(literaryWork.toPath()));
            for (String word : text.split("\\s")) {
                StringBuilder wordBuilder = new StringBuilder();
                for (char c : word.toCharArray()) {
                    if (isAlphaNumeric(c)) {
                        wordBuilder.append(c);
                    }
                }
                fileWordCounts.merge(wordBuilder.toString(), 1, Integer::sum);
            }
            DocumentImpl doc = new DocumentImpl(URI.create("file:///texts/" + literaryWork.getName()), text,null);
            wordCounts.put(doc, fileWordCounts);
        }
    }

    private void populateDocsWithThisWord() {
        for (Map.Entry<Document, Map<String, Integer>> wordCount : wordCounts.entrySet()) {
            for (String wordInDocument : wordCount.getValue().keySet()) {
                Set<Document> documentsWithThisWord = wordToDocument.computeIfAbsent(wordInDocument, k -> new HashSet<>());
                documentsWithThisWord.add(wordCount.getKey());
            }
        }
    }

    private Set<String> populateWordsThatAppearInMultipleDocuments() {
        return wordToDocument.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)//word appears in more than one doc
                .map(Map.Entry::getKey)//give us the word
                .collect(Collectors.toSet());
    }

    private File[] getTextsAsFiles() throws URISyntaxException {
        ClassLoader loader = LiteraryWorks.class.getClassLoader();
        URL folder = loader.getResource("texts");
        assert folder != null;
        File dir = Paths.get(folder.toURI()).toFile();
        File[] children = dir.listFiles();
        assert children != null;
        return children;
    }

    private static boolean inRange(char c, int from, int to) {
        return from <= c && c <= to;
    }

    private static boolean isAlphaNumeric(char c) {
        return isLetter(c) || isNumber(c);
    }

    private static boolean isLetter(char c) {
        return inRange(c, 65, 90) || inRange(c, 97, 122);
    }

    private static boolean isNumber(char c) {
        return inRange(c, 48, 57);
    }
}


