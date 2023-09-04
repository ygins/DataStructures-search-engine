package project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage5.impl.DocumentStoreImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import project.util.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.of;
import static project.util.TestingFiles.*;
import static project.util.Util.repeat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DocumentStoreImplTest {
    private final String[] commonPrefixes = new String[]{"a", "be", "de", "dis", "ex", "in", "mis", "over"};
    private DocumentStoreImpl docStore;

    //these can be more computationally expensive, so compute only once
    private SomeDocuments docs = new SomeDocuments();
    private LiteraryWorks literaryWorks;

    @BeforeAll
    void setupLiteraryWorks() throws URISyntaxException, IOException {
        docs = new SomeDocuments();
        literaryWorks = new LiteraryWorks();
    }

    @BeforeEach
    void setup() throws IOException {
        TestingFiles.deleteTestingFiles();
        this.docStore = new DocumentStoreImpl(TestingFiles.DIR.toFile());
    }

    @AfterEach
    void deleteJsonFiles() throws IOException {
        TestingFiles.deleteTestingFiles();
    }

    //Testing DocumentStoreImpl#put

    //Intuitive basic functionality test
    @Test
    void testPutAndGet() {
        var entries = createEntries();
        for (Map.Entry<URI, Document> entry : entries.entrySet()) {
            assertEquals(entry.getValue(), this.docStore.get(entry.getKey()));
        }
    }

    //Section 1 - InputStream != null (put operation)
    //1.1 - No previous values
    @Test
    void putReturnsZeroForNewElements() {
        int shouldBeZero = -1;
        try {
            shouldBeZero = this.docStore.put(new ByteArrayInputStream("Hi".getBytes()), TestingFiles.createTestingDirectoryURI("/s"), DocumentStore.DocumentFormat.TXT);
        } catch (IOException e) {
            handle(e);
        }
        assertEquals(0, shouldBeZero);
    }

    //1.2 - Previous values
    @Test
    void putReturnsPrevHashCode() {
        var entries = createEntries();
        for (Map.Entry<URI, Document> uriDocumentEntry : entries.entrySet()) {
            ByteArrayInputStream newInput = new ByteArrayInputStream((uriDocumentEntry.getValue().getDocumentTxt() + " (Modified)").getBytes());
            try {
                int oldHash = this.docStore.put(newInput, uriDocumentEntry.getKey(), DocumentStore.DocumentFormat.TXT);
                assertEquals(uriDocumentEntry.getValue().hashCode(), oldHash);
            } catch (IOException e) {
                handle(e);
            }
        }
    }

    //Section 2 - input == null (Delete operation)
    //2.1 - Previous Value
    @Test
    void deleteRemovesDocument() {
        var entries = new ArrayList<>(createEntries().entrySet());
        Collections.shuffle(entries);
        var deletedElements = new ArrayList<Map.Entry<URI, Document>>();
        int i = 0;
        var iter = entries.iterator();
        while (i < entries.size() / 2) {
            var next = iter.next();
            deletedElements.add(next);
            iter.remove();
            try {
                int prevHashCode = this.docStore.put(null, next.getKey(), DocumentStore.DocumentFormat.TXT);
                assertEquals(prevHashCode, next.getValue().hashCode());
            } catch (IOException e) {
                handle(e);
            }
            i++;
        }
        for (Map.Entry<URI, Document> deletedElement : deletedElements) {
            assertDocumentIsCompletelyRemoved(deletedElement.getValue()); //element is removed
        }
    }

    //2.2 - No prev value
    @Test
    void deleteNoPreviousValueReturnsZero() {
        try {
            assertEquals(0, this.docStore.put(null, TestingFiles.createTestingDirectoryURI("/x"), DocumentStore.DocumentFormat.TXT));
        } catch (IOException e) {
            handle(e);
        }
    }

    //Testing DocumentStore#delete
    //1 - Previous document detected
    @Test
    void deleteReturnsTrue() {
        var entries = createEntries();
        var singleElement = entries.entrySet().iterator().next();
        assertTrue(this.docStore.delete(singleElement.getKey()));
    }

    //2 - Previous document not detected
    @Test
    void deleteReturnsFalse() {
        assertFalse(this.docStore.delete(this.rainbowURI()));
    }

    /*
    Now we begin testing things added in stage 2!
     */

    //The undo mechanism
    /*
        Need to test for both general and URI specific
        1. undo put non-null --> should remove
        2. undo put null
            a. If no document there, no need to undo
            b. If document deleted --> should put back
        3. undo delete --> same as above

        As of stage 3, I'm altering the tests from stage 2 to also check the word count trie
     */

    @Test
    void basicUndoPutUnpopulated() {
        this.undoPut(false);
    }

    @Test
    void basicUndoPutPopulated() {
        this.undoPut(true);
    }

    private void undoPut(boolean populateBeforehand) {
        if (populateBeforehand) {
            this.createEntries();
        }
        this.insertHelloDocument();
        this.docStore.undo();
        assertDocumentIsCompletelyRemoved(docs.helloTextDocument());
    }

    /*
    @Test
    void undoPutNullNoPrevDocument(){
       With the given API, there's no way to test this
    }
     */

    @Test
    void basicUndoPutNullDeleteUnpopulated() {
        this.undoPutNullDelete(false);
    }

    @Test
    void basicUndoPutNullDeletePopulated() {
        this.undoPutNullDelete(true);
    }

    private void undoPutNullDelete(boolean populateBeforehand) {
        if (populateBeforehand) {
            this.createEntries();
        }
        this.insertHelloDocument();
        this.insertTxt(null, this.docs.getHelloTextUri());
        this.docStore.undo();
        assertAllWordsInDocAreInTrie(docs.helloTextDocument());
    }

    @Test
    void basicPutNullDoesNotCreateUndo() {
        this.insertTxt(null, docs.getHelloTextUri());
        assertThrows(IllegalStateException.class, () -> this.docStore.undo());
        assertDocumentIsCompletelyRemoved(docs.helloTextDocument());
    }

    @Test
    void basicUndoDeleteUnpopulated() {
        this.undoDelete(false);
    }

    @Test
    void basicUndoDeletePopulated() {
        this.undoDelete(true);
    }

    private void undoDelete(boolean populateBeforehand) {
        if (populateBeforehand) {
            this.createEntries();
        }
        this.insertHelloDocument();
        this.docStore.delete(this.docs.getHelloTextUri());
        this.docStore.undo();
        assertDocumentIsCorrectlyInserted(docs.helloTextDocument());
    }

    //Now we test undo of specific URLs

    @Test
    void undoPutSpecificURI() {
        randomKeys().forEach(selectedEntry -> {
            this.docStore.undo(selectedEntry.getKey());
            assertDocumentIsCompletelyRemoved(selectedEntry.getValue());
        });
    }

    @Test
    void undoPutNullDeleteSpecificURI() {
        this.undoSomeDeleteCommand(uri -> this.insertTxt(null, uri));
    }

    @Test
    void undoDeleteSpecificURI() {
        this.undoSomeDeleteCommand(this.docStore::delete);
    }


    private void undoSomeDeleteCommand(Consumer<URI> deleteFunc) {
        var randomKeys = randomKeys();
        randomKeys.forEach(selectedEntry -> {
            deleteFunc.accept(selectedEntry.getKey());
        });
        randomKeys.forEach(selectedEntry -> {
            this.docStore.undo(selectedEntry.getKey());
        });
        randomKeys.forEach(selectedEntry -> {
            assertDocumentIsCorrectlyInserted(selectedEntry.getValue());
        });
    }

    @Test
    void multipleActionsOnlyLatestUndone() {
        for (int i = 0; i < 3; i++) {
            this.insertHelloDocument();
            this.docStore.delete(this.docs.getHelloTextUri());
        }
        this.docStore.undo();
        assertEquals(docs.helloTextDocument(), this.docStore.get(this.docs.getHelloTextUri()));
    }

    @Test
    void multipleActionsOnlyLatestUndoneSpecificURI() {
        var randomEntry = randomKeys(1).iterator().next();
        for (int i = 0; i < 3; i++) {
            this.insertTxt(new ByteArrayInputStream(randomEntry.getValue().getDocumentTxt().getBytes()), randomEntry.getKey());
            this.docStore.delete(randomEntry.getKey());
        }
        this.docStore.undo();
        assertEquals(randomEntry.getValue(), this.docStore.get(randomEntry.getKey()));
    }

    @Test
    void undoPutDoesOverwrite() {
        this.insertHelloDocument();
        this.insertTxt(new ByteArrayInputStream("New".getBytes()), this.docs.getHelloTextUri());
        this.insertHelloDocument(); //same URI
        this.docStore.undo();//now that URI should map to "new"
        assertEquals("New", this.docStore.get(this.docs.getHelloTextUri()).getDocumentTxt());
        DocumentImpl newDoc = new DocumentImpl(docs.getHelloTextUri(), "New", null);
        assertAllWordsInDocAreInTrie(newDoc);
        this.docStore.undo();//now undo back to hello document
        assertEquals(this.docs.getHelloTextString(), this.docStore.get(this.docs.getHelloTextUri()).getDocumentTxt());
        assertDocumentIsNotInTrie(newDoc);//new should not be in the trie
    }


    @Test
    void undoURIThrowsException() {
        assertThrows(IllegalStateException.class, () -> this.docStore.undo(this.docs.getHelloTextUri()));
    }

    @Test
    void undoDoesNotCreateNewUndo() {
        this.insertHelloDocument();
        this.docStore.undo();
        assertThrows(IllegalStateException.class, () -> this.docStore.undo());//if added undo, would not throw exception
    }

    /*
     *Begin testing stage 3
     */

    //search
    @Test
    void testSearchOrdered() {
        insertLiteraryWorks();
        Set<String> wordsToSearchFor = literaryWorks.getWordsThatAppearInMultipleDocuments();
        for (String word : wordsToSearchFor) {
            Set<Document> search = literaryWorks.search(word);
            List<Integer> expectedWordCounts = sortByWordCountDescending(search, word).stream().map(doc -> doc.wordCount(word)).toList();
            List<Integer> wordCounts = docStore.search(word).stream().map(doc -> doc.wordCount(word)).toList();
            assertEquals(expectedWordCounts, wordCounts);
        }
    }

    //searchByPrefix
    @Test
    void testSearchByPrefix() {
        insertPrefixDocuments();
        assertEquals(Set.of(docs.prefixDoc1(), docs.prefixDoc2()), new HashSet<>(docStore.searchByPrefix("sea")));
        assertEquals(Set.of(docs.prefixDoc1()), new HashSet<>(docStore.searchByPrefix("ol")));
    }

    @Test
    void searchByPrefixReturnsEmptyListIfNothingFound() {
        assertEquals(0, docStore.searchByPrefix("supercalifragalisticexpialidocious").size());
    }

    @Test
    void testSearchByPrefixOrdered() {
        insertPrefixDocuments();
        assertEquals(List.of(docs.prefixDoc1(), docs.prefixDoc2()), docStore.searchByPrefix("sea"));
        assertEquals(List.of(docs.prefixDoc2(), docs.prefixDoc1()), docStore.searchByPrefix("do"));
    }

    @Test
    void searchByPrefixWorksWithMultipleOccurencesOfTheSamePrefix() {
        insertTxt(input("beach beat"), TestingFiles.createTestingDirectoryURI("/same/one"));
        insertTxt(input("beach beach beach"), TestingFiles.createTestingDirectoryURI("/same/two"));
        insertTxt(input("board"), TestingFiles.createTestingDirectoryURI("/same/three"));
        assertEquals(List.of("beach beach beach", "beach beat"), docStore.searchByPrefix("bea").stream().map(Document::getDocumentTxt).collect(Collectors.toList()));
    }

    //deleteAll
    @Test
    void deleteAll() {
        Set<String> wordsToDelete = literaryWorks.getWordsThatAppearInMultipleDocuments().stream().limit(10).collect(Collectors.toSet());
        for (String word : wordsToDelete) {
            insertLiteraryWorks();
            //ensure that documents are in the trie
            Set<Document> documentsWithThatWord = literaryWorks.getDocumentsWithWord(word);
            assertEquals(documentsWithThatWord, new HashSet<>(docStore.search(word)));
            //call deleteAll
            docStore.deleteAll(word);
            //ensure that each document with that word was removed from the docStore
            for (Document doc : documentsWithThatWord) {
                assertNull(docStore.get(doc.getKey()));
                //ensure removed from the trie
                for (String s : doc.getWords()) {
                    assertFalse(docStore.search(s).contains(doc));
                }
            }
            docStore = new DocumentStoreImpl();
        }
    }

    //deleteAllUndo - test that undo() undoes ALL actions
    @Test
    void deleteAllUndoesAllCommands() {
        repeat(20, time -> {
            insertLiteraryWorks();
            Random random = new Random();
            Set<String> repeatedWords = literaryWorks.getWordsThatAppearInMultipleDocuments();
            String toDelete = repeatedWords.stream().skip(random.nextInt(repeatedWords.size())).findFirst().orElseThrow();
            docStore.deleteAll(toDelete);
            //test deleteAll, and document should be deleted if it's word count for the given word is >0
            this.testUndoOnSomeDeletionCommand(literaryWorks.getDocumentsWithWord(toDelete), () -> docStore.search(toDelete));
            docStore = new DocumentStoreImpl();
        });
    }

    @Test
    void deleteAllReturnsEmptySetIfNothingFound() {
        assertEquals(0, docStore.deleteAll("supercalifragilisticexpialidocious").size());
    }

    @Test
    void deleteAllIsCaseSensitive() {
        //tested in trie, so we'll leave this here for now I suppose. I'm not sure what the best way to test this is
        //given that I tested this exact function already. Should I write a somewhat identical test?
    }

    //deleteAllWithPrefix
    @Test
    void deleteAllWithPrefix() {
        for (String prefix : this.commonPrefixes) {
            insertLiteraryWorks();
            Set<URI> deleted = docStore.deleteAllWithPrefix(prefix);
            //removed from hashtable?
            deleted.forEach(doc -> assertNull(docStore.get(doc)));
            //removed from trie?
            assertEquals(0, docStore.searchByPrefix(prefix).size());
            docStore = new DocumentStoreImpl();
        }
    }

    //deleteAllWithPrefix - undo() undoes ALL commands
    @Test
    void deleteAllWithPrefixUndoesAllCommands() {
        for (String prefix : this.commonPrefixes) {
            insertLiteraryWorks();
            docStore.deleteAllWithPrefix(prefix);
            this.testUndoOnSomeDeletionCommand(literaryWorks.getDocumentsWithPrefix(prefix), () -> docStore.searchByPrefix(prefix));
            docStore = new DocumentStoreImpl();
        }
    }

    /**
     * After some documents are deleted, asserts that undo puts them all back in the docstore
     *
     * @param documentsDeleted   the documents deleted
     * @param getDocumentsInTrie how to get the documents after deletion
     */
    private void testUndoOnSomeDeletionCommand(Set<Document> documentsDeleted, Supplier<List<Document>> getDocumentsInTrie) {
        List<Document> documentsFoundInTrie = getDocumentsInTrie.get();
        for (Document deletedDocument : documentsDeleted) {
            assertNull(docStore.get(deletedDocument.getKey()));
            assertFalse(documentsFoundInTrie.contains(deletedDocument));
        }
        docStore.undo();
        documentsFoundInTrie = getDocumentsInTrie.get();//should now be non-empty
        for (Document deletedDocument : documentsDeleted) {
            //put back into hash table?
            assertEquals(deletedDocument, docStore.get(deletedDocument.getKey()));
            //put back into trie?
            assertTrue(documentsFoundInTrie.contains(deletedDocument));
        }
    }

    //undo(URI) after a whole set undoes only actions relevant to that URI
    //test 1) after deleteAll and 2)deleteAlLWithPrefix

    @Test
    void undoURIAfterASetOnlyAffectsThatURIDeleteAll() {
        repeat(20, time -> {
            insertLiteraryWorks();
            Random random = new Random();
            Set<String> repeatedWords = literaryWorks.getWordsThatAppearInMultipleDocuments();
            String toDelete = repeatedWords.stream().skip(random.nextInt(repeatedWords.size())).findFirst().orElseThrow();
            docStore.deleteAll(toDelete);
            //test deleteAll, and document should be deleted if it's word count for the given word is >0
            this.testUndoURIAfterSetDeletion(literaryWorks.getDocumentsWithWord(toDelete), () -> docStore.search(toDelete));
            docStore = new DocumentStoreImpl();
        });
    }

    @Test
    void undoURIAfterASetOnlyAffectsThatURIDeleteAllWithPrefix() {
        for (String prefix : this.commonPrefixes) {
            insertLiteraryWorks();
            docStore.deleteAllWithPrefix(prefix);
            this.testUndoURIAfterSetDeletion(literaryWorks.getDocumentsWithPrefix(prefix), () -> docStore.searchByPrefix(prefix));
            docStore = new DocumentStoreImpl();
        }
    }

    /**
     * After some documents are deleted, undoes them one by one and ensures that they are put back in,
     * and that no other documents are put back in
     *
     * @param deleted
     * @param getDocumentsFromTrie
     */
    private void testUndoURIAfterSetDeletion(Set<Document> deleted, Supplier<List<Document>> getDocumentsFromTrie) {
        Set<Document> deletedCopy = new HashSet<>(deleted);
        List<Document> documentsFoundInTrie = getDocumentsFromTrie.get();
        for (Document doc : deleted) {//for each document, undo its deletion and ensure it exists
            assertNull(docStore.get(doc.getKey()));
            assertFalse(documentsFoundInTrie.contains(doc));
            //undo
            docStore.undo(doc.getKey());
            //in hash table?
            assertEquals(doc, docStore.get(doc.getKey()));
            //in trie?
            documentsFoundInTrie = getDocumentsFromTrie.get();
            assertTrue(documentsFoundInTrie.contains(doc));
            //ensure that all previous documents are still deleted
            deletedCopy.remove(doc);
            for (Document stillDeleted : deletedCopy) {
                assertNull(docStore.get(stillDeleted.getKey()));
                assertFalse(documentsFoundInTrie.contains(stillDeleted));
            }
        }
    }

    //testing misc. undo functions

    @Test
    void undoGoesToNextCommandAfterFinishingCommandSetViaMultipleUndos() {
        insertLiteraryWorks();
        String customString = "This is not in the LiteraryWorks";
        URI customURI = TestingFiles.createTestingDirectoryURI("/unique");
        insertTxt(input(customString), customURI);
        String someWord = literaryWorks.getWordsThatAppearInMultipleDocuments().iterator().next();
        Set<URI> deleted = docStore.deleteAll(someWord);
        //Now if we insert all of the URI's separately via undo, next undo should remove our custom input
        for (URI deletedURI : deleted) {
            docStore.undo(deletedURI);
        }
        assertEquals(customString, docStore.get(customURI).getDocumentTxt());
        docStore.undo();//undo the initial custom insert
        assertNull(docStore.get(customURI));
    }

    @Test
    void undoGoesToNextCommandAfterFinishingCommandSetViaOneUndo() {
        insertLiteraryWorks();
        String customString = "This is not in the LiteraryWorks";
        URI customURI = TestingFiles.createTestingDirectoryURI("/unique");
        insertTxt(input(customString), customURI);
        String someWord = literaryWorks.getWordsThatAppearInMultipleDocuments().iterator().next();
        docStore.deleteAll(someWord);
        docStore.undo();//undo deleteAll
        assertEquals(customString, docStore.get(customURI).getDocumentTxt());
        docStore.undo();//undo the insert
        assertNull(docStore.get(customURI));
    }

    //Stage 4 - test memory usage
    //test max doc count

    @ParameterizedTest
    @MethodSource("numberedDocumentsAndMaxDocSize")
    void canSetMaxDocAmountBeforeInsertion(int amountDocs, int limit) {
        this.docStore.setMaxDocumentCount(limit);
        var docs = numberedDocuments(amountDocs);
        for (Document doc : docs) {
            insert(doc);
        }
        for (int i = docs.length - limit; i < docs.length; i++) {//last documents should be in memory..
            Document document = docs[i];
            assertNotOnDisk(document.getKey());
            assertEquals(document, docStore.get(document.getKey()));
        }
        for (int i = 0; i < docs.length - limit; i++) {
            assertOnDisk(docs[i].getKey());//but first ones out on disk
            assertEquals(docs[i], docStore.get(docs[i].getKey()));
        }
    }

    @ParameterizedTest
    @MethodSource("numberedDocumentsAndMaxDocSize")
    void canSetMaxDocCountAfterInsertion(int amountDocs, int limit) {
        var docs = numberedDocuments(amountDocs);
        for (Document doc : docs) {
            insert(doc);
        }
        this.docStore.setMaxDocumentCount(limit);
        for (int i = docs.length - limit; i < docs.length; i++) {//last documents should be in memory...
            Document document = docs[i];
            TestingFiles.assertNotOnDisk(document.getKey());
            assertEquals(document, docStore.get(document.getKey()));
        }
        for (int i = 0; i < docs.length - limit; i++) {
            //but first ones are out of memory
            TestingFiles.assertOnDisk(docs[i].getKey());
            assertEquals(docs[i], docStore.get(docs[i].getKey()));
        }
    }

    private Stream<Arguments> numberedDocumentsAndMaxDocSize() {
        return Stream.of(//number of documents to generate and limit to set
                of(4, 3),
                of(10, 6),
                of(356, 23),
                of(78, 34),
                of(500, 20),
                of(700, 345)
        );
    }

    @Test
    void canSetByteLimitBeforeInsertion() {
        docStore.setMaxDocumentBytes(10);
        var firstURi = insert("first", "6bytes");
        var secondURI = insert("second", "7bytess");
        TestingFiles.assertOnDisk(firstURi);
        TestingFiles.assertNotOnDisk(secondURI);
    }

    @Test
    void canSetByteLimitAfterInsertion() {
        docStore.setMaxDocumentBytes(10);
        URI[] urisInserted = new URI[9];
        for (int i = 0; i < 9; i++) {//insert 9 single byte docs
            urisInserted[i] = insert(String.valueOf(i), String.valueOf(i));
        }
        URI bigDocURI = insert("big-doc", "12345");
        for (int i = 0; i < 4; i++) {//should auto delete the first 4 inserted
            TestingFiles.assertOnDisk(urisInserted[i]);
            assertEquals(String.valueOf(i), docStore.get(urisInserted[i]).getDocumentTxt());
            TestingFiles.assertNotOnDisk(urisInserted[i]);
        }
        for (int i = 4; i < 9; i++) {//but others are still there
            assertEquals(String.valueOf(i), docStore.get(urisInserted[i]).getDocumentTxt());
        }
        assertEquals("12345", docStore.get(bigDocURI).getDocumentTxt());
    }

    @RepeatedTest(value = 3)
    void ifWeDontUpdateAnythingTheTestFails(RepetitionInfo repetitionInfo) {
        this.testThatSomeMethodUpdatesLastUseTime(doc -> {
        }, repetitionInfo.getCurrentRepetition(), true);
    }

    @RepeatedTest(value = 3)
    void getUpdatesLastUseTime(RepetitionInfo repetitionInfo) {
        Consumer<Document> get = doc -> this.docStore.get(doc.getKey());
        this.testThatSomeMethodUpdatesLastUseTime(get, repetitionInfo.getCurrentRepetition());
    }

    @RepeatedTest(value = 3)
    void searchUpdatesLastUseTime(RepetitionInfo repetitionInfo) {
        Consumer<Document> search = doc -> this.docStore.search(doc.getWords().iterator().next());
        //note - this is assuming that search.apply(doc) will only affect doc. This is true
        //in this circumstance because in the private method we generate unique docs with just numbers
        this.testThatSomeMethodUpdatesLastUseTime(search, repetitionInfo.getCurrentRepetition());
    }

    @Test
    void searchUpdatesLastUseTimeMultipleDocsAtOnce() {
        var uri3 = insert("3", "apple 3");
        var uri2 = insert("2", "apple 2");
        insert("1", "1");
        insert("0", "0");
        this.docStore.search("apple");
        this.docStore.setMaxDocumentCount(2);//since we searched, apple docs are not deleted
        assertEquals("apple 2", docStore.get(uri2).getDocumentTxt());
        assertEquals("apple 3", docStore.get(uri3).getDocumentTxt());
    }

    @Test
    void searchByPrefixUpdatesLastUseTimeMultipleDocsAtOnce() {
        var uri3 = insert("3", "apple 3");
        var uri2 = insert("2", "apply 2");
        insert("1", "1");
        insert("0", "0");
        this.docStore.searchByPrefix("appl");
        this.docStore.setMaxDocumentCount(2);//since we searched, appl docs are not deleted
        assertEquals("apply 2", docStore.get(uri2).getDocumentTxt());
        assertEquals("apple 3", docStore.get(uri3).getDocumentTxt());
    }

    @RepeatedTest(value = 3)
    void searchByPrefixUpdatesLastUseTime(RepetitionInfo repetitionInfo) {
        Consumer<Document> searchByPrefix = doc -> this.docStore.searchByPrefix(doc.getDocumentTxt());
        this.testThatSomeMethodUpdatesLastUseTime(searchByPrefix, repetitionInfo.getCurrentRepetition());
    }

    private void testThatSomeMethodUpdatesLastUseTime(Consumer<Document> methodToTest, int repetition, boolean assertNegative) {
        int amountDocuments = (int) Math.pow(10, repetition);
        var docs = numberedDocuments(amountDocuments);
        for (Document doc : docs) {
            insert(doc);
        }
        int amountToGet = (int) Math.pow(3, repetition);
        for (int i = amountDocuments / 2 - 1, e = amountToGet; e > 0; e--, i++) {
            methodToTest.accept(docs[i]);//update the document to be last used
        }
        docStore.setMaxDocumentCount(amountToGet);//limit the doc store to just those docs
        for (int i = amountDocuments / 2 - 1, e = amountToGet; e > 0; e--, i++) {
            Document expected = docs[i];
            if (!assertNegative) {
                TestingFiles.assertNotOnDisk(docs[i].getKey());//docs in doc store should be just those that we updated - not in memory
            } else {
                TestingFiles.assertOnDisk(docs[i].getKey());
            }
        }
        if (!assertNegative) {
            Document docInFile = docs[amountDocuments / 2 - 2];
            TestingFiles.assertOnDisk(docInFile.getKey()); //assert other docs are not there
        }
    }

    private void testThatSomeMethodUpdatesLastUseTime(Consumer<Document> method, int repetition) {
        this.testThatSomeMethodUpdatesLastUseTime(method, repetition, false);
    }

    @RepeatedTest(value = 2)
    void undoURIUpdatesLastUsedTime(RepetitionInfo repetitionInfo) {
        this.testThatSomeUndoUpdatesLastUsedTime(doc -> docStore.undo(doc.getKey()), repetitionInfo.getCurrentRepetition());
    }

    @RepeatedTest(value = 2)
    void undoGeneralUpdatesLastUsedTime(RepetitionInfo repetitionInfo) {
        this.testThatSomeUndoUpdatesLastUsedTime($ -> docStore.undo(), repetitionInfo.getCurrentRepetition());
    }

    private void testThatSomeUndoUpdatesLastUsedTime(Consumer<Document> undoCommand, int repetition) {
        int amountDocuments = (int) Math.pow(10, repetition);
        var docs = numberedDocuments(amountDocuments);
        int amountToGet = (int) Math.pow(3, repetition);
        for (Document doc : docs) {
            insert(doc);
        }
        for (int i = amountDocuments / 2 - 1, e = amountToGet; e > 0; e--, i++) {
            Document doc = docs[i];
            docStore.delete(doc.getKey());
        }
        for (int i = amountDocuments / 2 - 1, e = amountToGet; e > 0; e--, i++) {
            undoCommand.accept(docs[i]);
        }
        docStore.setMaxDocumentCount(amountToGet);//limit the doc store to just those docs
        for (int i = amountDocuments / 2 - 1, e = amountToGet; e > 0; e--, i++) {
            assertNotOnDisk(docs[i].getKey()); //document was not moved to disk
            assertEquals(docs[i], docStore.get(docs[i].getKey()));
        }
        assertOnDisk(docs[amountDocuments / 2 - 2].getKey());//this doc was moved to disk
        assertEquals(docs[amountDocuments / 2 - 2], docStore.get(docs[amountDocuments / 2 - 2].getKey()));
    }

    @Test
    void documentsInMassActionHaveSameLastUseTime() {
        var uri1 = insert("First", "PrefixFirst");
        var uri2 = insert("Second", "PrefixSecond");
        var doc1 = docStore.get(uri1);
        var doc2 = docStore.get(uri2);
        assertNotEquals(doc1.getLastUseTime(), doc2.getLastUseTime());
        docStore.searchByPrefix("Prefix");
        assertEquals(doc1.getLastUseTime(), doc2.getLastUseTime());
    }


    @Test
    void deletingFromMemoryDoesNotRemovesFromCommandStack() {
        var first = insert("1", "1");
        docStore.delete(first);
        assertNotOnDisk(first);
        insert("1", "1");
        var second = insert("2", "2");
        var third = insert("3", "3");
        assertNotOnDisk(second);
        assertNotOnDisk(third);
        docStore.setMaxDocumentCount(2);//should move doc 1 to disk
        assertOnDisk(first);
        docStore.undo();//delete 3
        assertNull(docStore.get(third));
        docStore.undo();//delete 2
        assertNull(docStore.get(second));
        assertDoesNotThrow(() -> docStore.undo());
        assertNull(docStore.get(first));
        docStore.undo();
        assertEquals("1", docStore.get(first).getDocumentTxt());
    }

    @Test
//if I have a command set of 3 diff docs, and 1 is moved to disk
    void deletingFromMemoryDoesNotPartiallyAffectACommandSet() {
        var one = insert("1", "a1");
        var two = insert("2", "a2");
        var three = insert("3", "a3");
        docStore.deleteAllWithPrefix("a");
        insert("1", "a1");
        insert("2", "a2");
        insert("3", "a3");
        docStore.setMaxDocumentCount(2);//1 should be moved to disk
        docStore.undo();//undo 3 - delete it
        docStore.undo();//undo 2 - delete it
        docStore.undo();//undo 1
        //Now none should be there
        assert one != null;
        assert two != null;
        assert three != null;
        Set.of(one, two, three).forEach(it -> assertNull(docStore.get(it)));
        assertNull(docStore.get(one));
    }

    @Test
    void deletingNormallyDoesNotAffectACommandSet() {
        var one = insert("1", "a1");
        var two = insert("2", "a2");
        insert("3", "a3");
        docStore.deleteAllWithPrefix("a");
        docStore.undo();
        assertEquals("a2", docStore.get(two).getDocumentTxt());
        assertEquals("a1", docStore.get(one).getDocumentTxt());
        docStore.undo();
        assertEquals("a1", docStore.get(one).getDocumentTxt());
    }

    @Test
    void insertingTooBigDoesNotThrowIllegalArg() {
        docStore.setMaxDocumentBytes(8);
        final URI[] uri = new URI[1];
        assertDoesNotThrow(() -> {
            uri[0] = insert("1", "1234567890");
        });
        TestingFiles.assertOnDisk(uri[0]);
    }

    @Test
    void insertingTooBigDoesNotAffectOtherDocuments() {
        docStore.setMaxDocumentBytes(8);
        var urisInMemory = new HashSet<URI>();
        for (int i = 0; i < 7; i++) {
            urisInMemory.add(insert(String.valueOf(i), String.valueOf(i)));
        }
        var bigURI = insert("Large-document", "1234567890987654321");
        assertOnDisk(bigURI);//too large doc goes straight to disk
        //assertEquals(bigURI, docStore.get(bigURI).getKey());
        urisInMemory.forEach(it -> {
            assertNotOnDisk(it);//small doc should remain in memory
            assertEquals(it, docStore.get(it).getKey());
        });
    }

    @Test
    void removingWhenFileIsOnDisk() {
        var uri = insert("test_file", "cool");
        docStore.setMaxDocumentCount(0);//moves to disk
        assertDoesNotThrow(() -> docStore.delete(uri));//should delete fine
        assertNull(docStore.get(uri));
        assertNotOnDisk(uri);
    }

    @Test
    void memoryOverflowDoesNotRemoveFromTrie() {
        docStore.setMaxDocumentCount(1);
        var first = insert("First", "ABCDEFG");
        var second = insert("Second", "HIJKLMNOP");
        assertOnDisk(first);
        assertNotOnDisk(second);
        assertEquals(first, docStore.search("ABCDEFG").iterator().next().getKey());
    }

    @Test
    void undoInsertWillRemoveFromMemory() {
        var uri = insert("one", "one");
        docStore.undo();
        assertNotOnDisk(uri);
        assertNull(docStore.get(uri));
    }

    @Test
    void undoInsertWillRemoveFromDisk() {
        docStore.setMaxDocumentBytes(0);
        var uri = insert("one", "one");
        assertOnDisk(uri);
        docStore.undo();
        assertNotOnDisk(uri);
        assertNull(docStore.get(uri));
    }

    @Test
    void undoDeleteFromDiskWillPutIntoMemory() {
        docStore.setMaxDocumentBytes(0);
        var uri = insert("one", "one");
        assertOnDisk(uri);
        docStore.delete(uri);
        assertNotOnDisk(uri);
        assertNull(docStore.get(uri));
        docStore.setMaxDocumentBytes(100);
        docStore.undo();
        assertNotOnDisk(uri);
        assertEquals(uri, docStore.get(uri).getKey());
    }

    @Test
    void undoDeleteFromMemoryWillPutIntoDisk() {
        var uri = insert("one", "one");
        docStore.delete(uri);
        assertNotOnDisk(uri);
        assertNull(docStore.get(uri));
        docStore.setMaxDocumentBytes(0);
        docStore.undo();
        assertOnDisk(uri);
        assertEquals(uri, docStore.get(uri).getKey());
    }

    @Test
    void sequencesOfMoves() {
        var uri1 = insert("one", "one");
        var uri2 = insert("two", "two");
        docStore.setMaxDocumentCount(1);
        assertOnDisk(uri1);
        assertNotOnDisk(uri2);
        docStore.get(uri1);
        assertNotOnDisk(uri1);
        assertOnDisk(uri2);
        docStore.setMaxDocumentCount(2);
        docStore.get(uri2);
        assertNotOnDisk(uri1);
        assertNotOnDisk(uri2);
        var uri3 = insert("three", "three");
        assertOnDisk(uri1);
        assertNotOnDisk(uri2);
        assertNotOnDisk(uri3);
    }

    @Test
    void settingDifferentFolder() throws IOException {
        Path wd = Paths.get(System.getProperty("user.dir")).resolve("subfolder");
        docStore = new DocumentStoreImpl(wd.toFile());
        docStore.setMaxDocumentCount(0);
        var uri = insert("one", "one");
        assertTrue(Files.exists(wd.resolve("one.json")));
        Files.deleteIfExists(wd.resolve("one.json"));
        Files.deleteIfExists(wd);
    }

    @Test
    void noDocumentsGoToDiskByDefault() {
        var accounts = Accounts.randomAccounts(100, true);
        for (Account account : accounts) {
            insert(account.getURI().toString(), account.toString());
        }
        for (Account account : accounts) {
            assertNotOnDisk(account.getURI());
        }
    }

    //test various URI's

    @ParameterizedTest
    @ValueSource(strings = {"http", "https"})
    void webURI(String header) {
        String str = header + "://www.yu.edu/documents/ap_courses/3";
        docStore.setMaxDocumentCount(0);
        var uri = insert(str, "APUSH: 4 credits");
        assertOnDiskAt(uri, str);
        assertEquals(uri, docStore.get(uri).getKey());
    }

    @Test
    void justAFolder() {
        var uri = insert("https://www.yu.edu", "YU!");
        docStore.setMaxDocumentCount(0);
        assertOnDiskAt(uri, "www.yu.edu.json");
        assertEquals("YU!", docStore.get(uri).getDocumentTxt());
    }

    @Test
    void webUriWithInvalidCharacters() {
        String str = "mailto://ygins:gmail.com/ema@ils/3*/5/Tu?esday";
        docStore.setMaxDocumentCount(0);
        var uri = insert(str, "This is an email!");
        assertOnDiskAt(uri, "yginsgmail.com/emails/3/5/Tu?esday.json");
    }
    //Utility methods here on down

    private List<Document> sortByWordCountDescending(Set<Document> toSort, String word) {
        return toSort.stream().sorted(Comparator.comparingInt(doc -> doc.wordCount(word) * -1)).toList();
    }

    private void assertDocumentIsCorrectlyInserted(Document doc) {
        assertEquals(doc, docStore.get(doc.getKey()));
        assertAllWordsInDocAreInTrie(doc);
    }

    private void assertDocumentIsCompletelyRemoved(Document doc) {
        assertNull(docStore.get(doc.getKey()));
        assertDocumentIsNotInTrie(doc);
    }

    private void assertDocumentIsNotInTrie(Document doc) {
        doc.getWords().forEach(word -> assertFalse(docStore.search(word).contains(doc)));
    }

    private void assertAllWordsInDocAreInTrie(Document doc) {
        doc.getWords().forEach(word -> assertTrue(docStore.search(word).contains(doc)));
    }

    private Document[] numberedDocuments(int size) {
        return IntStream.range(0, size).mapToObj(num -> new DocumentImpl(TestingFiles.createTestingDirectoryURI("numbered/" + num), String.valueOf(num), null)).toArray(Document[]::new);
    }

    private Set<Map.Entry<URI, Document>> randomKeys(int amount) {
        var entries = this.createEntries();
        var selectedKeys = new HashSet<Map.Entry<URI, Document>>();
        for (int e = 0; e < amount; e++) {
            var iter = entries.entrySet().iterator();
            Map.Entry<URI, Document> selectedEntry = null;
            for (int i = new Random().nextInt(5, entries.size()); i < entries.size(); i++) {
                selectedEntry = iter.next();
            }
            selectedKeys.add(selectedEntry);
            iter.remove();
        }
        return selectedKeys;
    }

    private Set<Map.Entry<URI, Document>> randomKeys() {
        return this.randomKeys(6);
    }

    private Map<URI, Document> createEntries() {
        var map = new HashMap<URI, Document>();
        for (char x = 'a'; x <= 'z'; x++) {
            URI key = TestingFiles.createTestingDirectoryURI("/" + x);
            String text = "Stored at location: " + x;
            try {
                this.docStore.put(new ByteArrayInputStream(text.getBytes()), key, DocumentStore.DocumentFormat.TXT);
            } catch (IOException e) {
                throw new RuntimeException("IO Error in testing, moving on", e);
            }
            map.put(key, new DocumentImpl(key, text, null));
        }
        return map;
    }

    private URI rainbowURI() {
        return TestingFiles.createTestingDirectoryURI("/somewhere_over_the_rainbow");
    }

    private void insertHelloDocument() {
        this.insertTxt(input(this.docs.getHelloTextString()), this.docs.getHelloTextUri());
    }

    private void insertBinaryDocument() {
        try {
            this.docStore.put(new ByteArrayInputStream(this.docs.getBinaryDocData()), this.docs.getBinaryDataUri(), DocumentStore.DocumentFormat.BINARY);
        } catch (IOException e) {
            handle(e);
        }
    }

    private void insertPrefixDocuments() {
        this.insertTxt(input(this.docs.prefixDoc1().getDocumentTxt()), this.docs.prefixDoc1().getKey());
        this.insertTxt(input(this.docs.prefixDoc2().getDocumentTxt()), this.docs.prefixDoc2().getKey());
    }

    private void insertTxt(InputStream inputStream, URI uri) {
        try {
            this.docStore.put(inputStream, uri, DocumentStore.DocumentFormat.TXT);
        } catch (IOException e) {
            handle(e);
        }
    }

    private URI insert(String uri, String doc) {
        try {
            URI uri1 = TestingFiles.createTestingDirectoryURI(uri);
            this.docStore.put(input(doc), uri1, DocumentStore.DocumentFormat.TXT);
            return uri1;
        } catch (IOException e) {
            handle(e);
        }
        return null;
    }

    private void insert(Document doc) {
        this.insertTxt(input(doc.getDocumentTxt()), doc.getKey());
    }

    private void insertLiteraryWorks() {
        for (Document document : literaryWorks.getDocuments()) {
            insertTxt(input(document.getDocumentTxt()), document.getKey());
        }
    }

    private InputStream input(String string) {
        return new ByteArrayInputStream(string.getBytes());
    }

    private void handle(IOException e) {
        throw new RuntimeException("Exception during testing", e);
    }
}
