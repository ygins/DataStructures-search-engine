package project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage5.impl.DocumentPersistenceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import project.util.Account;
import project.util.Accounts;
import project.util.LiteraryWorks;
import project.util.TestingFiles;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static project.util.Assertions.assertThrowsIllegalArg;
import static project.util.TestingFiles.deleteTestingFiles;

public class DocumentPersistenceManagerTest {

    private DocumentPersistenceManager pm;
    private URI account1URI = URI.create("google/accounts/account1");

    @BeforeEach
    void setup() throws IOException {
        this.pm = new DocumentPersistenceManager(TestingFiles.DIR.toFile());
        deleteTestingFiles();
    }

    @AfterAll
    static void tearDown() throws IOException {
        deleteTestingFiles();
    }

    @Test
    void basicFunctionality() throws IOException {
        var doc = new DocumentImpl(account1URI, "Account #1 - Name: Yonatan. Major: Comp Sci",null);
        pm.serialize(account1URI, doc);
        assertEquals(doc, pm.deserialize(account1URI));
    }

    @Test
        //Ensure that we overwrite with new input (shorter)
    void overwritingShorterJson() throws IOException {
        var docShorter = new DocumentImpl(account1URI, "Account #1",null);
        var docLonger = new DocumentImpl(account1URI, "Account #100!",null);
        serialize(docLonger);
        serialize(docShorter);
        assertEquals(docShorter, pm.deserialize(account1URI));
    }

    @Test
    void canCreateALotOfFiles() throws IOException {
        var accounts = Accounts.randomAccounts(150);
        Document[] accountDocs = new DocumentImpl[150];
        for (int i = 0; i < accounts.length; i++) {
            Account account = accounts[i];
            var uri = account.getURI();
            var doc = new DocumentImpl(uri, account.toString(),null);
            accountDocs[i] = doc;
            serialize(doc);
        }
        for (int i = 0; i < accounts.length; i++) {
            assertEquals(accountDocs[i], pm.deserialize(accounts[i].getURI()));
        }
    }

    @Test
        //Ensure that word map remains consistent across serialization/deserialization
    void forLargeTextsConsistentWordMap() throws IOException, URISyntaxException {
        var literaryWorks = new LiteraryWorks();
        var docs = literaryWorks.getDocuments();
        for (Document doc : docs) {
            serialize(doc);
        }
        for (Document doc : docs) {
            Document deserialized = pm.deserialize(doc.getKey());
            assertEquals(doc, deserialized);
            assertEquals(doc.getWordMap(), deserialized.getWordMap());
        }
    }

    /*
    @Test
    void relativeBaseDIR() throws IOException {
        this.pm = new DocumentPersistenceManager(TestingFiles.RELATIVE_DIR.toFile());
        basicFunctionality();
    }
     */

    @Test
    void canSerializeBinaryDocs() throws IOException{
        var accounts = Accounts.randomAccounts(10);
        for(Account account:accounts){
            serialize(account.toBinaryDocument());
        }
        for(Account account:accounts){
            Document deserialize = pm.deserialize(account.getURI());
            assertEquals(account.toBinaryDocument(), deserialize);
            assertArrayEquals(account.toBinaryDocument().getDocumentBinaryData(), deserialize.getDocumentBinaryData());
        }
    }

    @Test
    void nullInputToSerialize(){
        Account account = new Account("Bob", 1, "red");
        assertThrowsIllegalArg(()-> {
            pm.serialize(null, account.toDocument());
        });
        assertThrowsIllegalArg(()->pm.serialize(account.getURI(),null));
    }

    @Test
    void nullInputToDeserialize(){
        assertThrowsIllegalArg(()->pm.deserialize(null));
    }

    @Test
    void fileDoesNotExistForDeserializeReturnsNull() throws Exception{
        assertNull(pm.deserialize(URI.create("one/two")));
    }

    @Test
    void deserializesWordCountCorrectly() throws IOException {
        String text = "one two two three three three four four four four";
        Map<String, Integer> wordCounts = Map.of("one",1,"two",2,"three",3,"four",4);
        Document doc = new DocumentImpl(URI.create("words"),text,null);
        serialize(doc);
        assertEquals(wordCounts,pm.deserialize(doc.getKey()).getWordMap());
    }

    private void serialize(Document document) throws IOException {
        pm.serialize(document.getKey(), document);
    }
}
