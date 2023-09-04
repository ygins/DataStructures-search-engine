package project.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.impl.DocumentPersistenceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import project.util.Account;
import project.util.Accounts;
import project.util.LiteraryWorks;
import project.util.TestingFiles;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static project.util.TestingFiles.deleteTestingFiles;

public class BTreeImplTest {

    private BTree<URI, Document> bTree = new BTreeImpl<>();
    private DocumentPersistenceManager pm;

    @BeforeEach
    void setup() throws IOException {
        bTree = new BTreeImpl<>();
        this.pm = new DocumentPersistenceManager(TestingFiles.DIR.toFile());
        bTree.setPersistenceManager(pm);
        deleteTestingFiles();
    }

    @AfterAll
    static void tearDown() throws IOException {
        deleteTestingFiles();
    }

    @Test
    void basicPutGet() {
        var accounts = Accounts.randomAccounts(200, true);//works with 5 consistently but not 6, split is off
        for (Account account : accounts) {
            bTree.put(account.getURI(), account.toDocument());
        }
        List<Account> accountList = new ArrayList<Account>(List.of(accounts));
        Collections.shuffle(accountList);
        for (Account account : accountList) {
            assertEquals(account.toDocument(), bTree.get(account.getURI()));
        }
    }

    @Test
    void problemWithSentinel() {
        var accounts = Accounts.randomAccounts(7);
        //first account has lowest ID
        for (int i = 1; i < accounts.length; i++) {
            insert(accounts[i]);
        }
        //insert first one
        insert(accounts[0]);
        for (Account account : accounts) {//put a debug here, not sure at the moment how to tell if sentinel is working or not
            assertEquals(account.toDocument(), bTree.get(account.getURI()));
        }
    }

    @Test
    void serializesToDisk() throws Exception {
        var accounts = Accounts.randomAccounts(200);
        for (Account account : accounts) {
            insert(account);
        }
        for (Account account : accounts) {
            bTree.moveToDisk(account.getURI());//move accounts to disk
        }
        for (Account account : accounts) {
            var deserialized = pm.deserialize(account.getURI());//assert all accounts are there
            assertEquals(account.toDocument(), deserialized);
        }
    }

    @Test
    void getsFromDisk() throws Exception {
        var accounts = Accounts.randomAccounts(200, true);
        for (Account account : accounts) {
            insert(account);
        }
        var shuffledAccounts = new ArrayList<>(List.of(accounts));
        Collections.shuffle(shuffledAccounts);
        for (Account account : shuffledAccounts) {
            bTree.moveToDisk(account.getURI());
        }
        Collections.shuffle(shuffledAccounts);
        for (Account account : shuffledAccounts) {
            TestingFiles.assertOnDisk(account.getURI());
            var deserialized = bTree.get(account.getURI());
            assertEquals(deserialized, account.toDocument());
            TestingFiles.assertNotOnDisk(account.getURI());
        }
        TestingFiles.assertNoFilesCreated();
    }

    @Test
    void gettingFromDiskMaintainsWordMap() throws Exception {
        var literaryWorks = new LiteraryWorks();
        for (Document work : literaryWorks.getDocuments()) {
            this.bTree.put(work.getKey(), work);
            this.bTree.moveToDisk(work.getKey());
        }
        for (Document work : literaryWorks.getDocuments()) {
            assertEquals(work.getWordMap(), bTree.get(work.getKey()).getWordMap());
        }
        TestingFiles.assertNoFilesCreated();
    }

    @Test
    void deletingRemovesFromDisk() throws Exception {
        var accounts = Accounts.randomAccounts(100, true);
        for (Account account : accounts) {
            insert(account);
            bTree.moveToDisk(account.getURI());
        }
        var shuffledAccounts = new ArrayList<>(List.of(accounts));
        Collections.shuffle(shuffledAccounts);
        for (Account account : shuffledAccounts) {//delete, should be removed from disk
            bTree.put(account.getURI(), null);
        }
        Collections.shuffle(shuffledAccounts);
        for (Account account : shuffledAccounts) {//assert removed
            assertNull(bTree.get(account.getURI()));
            TestingFiles.assertNotOnDisk(account.getURI());
        }
        TestingFiles.assertNoFilesCreated();
    }

    @Test
    void bTreePutOverwritesInMemory() {
        var accounts = Accounts.randomAccounts(200, true);
        for (Account account : accounts) {
            insert(account);
        }
        for (int i = 0; i < accounts.length - 1; i++) {
            bTree.put(accounts[i + 1].getURI(), accounts[i].toDocument());//map each account to the URI of the one AFTER it
        }
        for (int i = 0; i < accounts.length - 1; i++) {
            var read = bTree.get(accounts[i + 1].getURI());
            assertEquals(accounts[i].toDocument(), read);
        }
    }

    @Test
    //ensure that when we have a file on disk, put overwrites it
    void bTreePutOverwritesDisk() throws Exception {
        var accounts = Accounts.randomAccounts(200, true);
        for (int i = 1; i < accounts.length - 1; i++) {
            insert(accounts[i]);
            bTree.moveToDisk(accounts[i].getURI());
        }
        for (int i = 0; i < accounts.length - 1; i++) {
            bTree.put(accounts[i + 1].getURI(), accounts[i].toDocument());
            TestingFiles.assertNotOnDisk(accounts[i + 1].getURI());
            bTree.moveToDisk(accounts[i + 1].getURI());
            TestingFiles.assertOnDisk(accounts[i + 1].getURI());
        }
        for (int i = 0; i < accounts.length - 1; i++) {
            var read = bTree.get(accounts[i + 1].getURI());
            assertEquals(accounts[i].toDocument(), read);
        }
    }

    @Test
    void expectedBehaviorMoveToDiskNullValue() throws Exception {
        Account a = new Account("Ploni",56,"red");
        insert(a);
        bTree.put(a.getURI(),null);
        TestingFiles.assertNotOnDisk(a.getURI());
        assertThrows(NoSuchElementException.class, ()->bTree.moveToDisk(a.getURI()));
    }

    @Test
    void nullKeyForMoveToDisk(){
        assertThrows(IllegalArgumentException.class, ()->bTree.moveToDisk(null));
    }

    @Test
    void nullPmForMoveToDisk(){
        bTree.setPersistenceManager(null);
        assertThrows(IllegalStateException.class, ()->bTree.moveToDisk(URI.create("one/two")));
    }

    private void insert(Account account) {
        bTree.put(account.getURI(), account.toDocument());
    }
}
