package edu.yu.cs.com1320.project.stage5.impl;


import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.*;
import edu.yu.cs.com1320.project.impl.*;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class DocumentStoreImpl implements DocumentStore {

    private final BTree<URI, Document> bTree;
    private final Stack<Undoable> commandStack;
    private final Trie<URI> wordCounts;
    private final MinHeap<URI> memoryHeap;
    private final Set<URI> newlyReadInURIs;
    private int maxDocCount;
    private int maxByteCount;
    private int usedMemory;
    private int docCount;

    public DocumentStoreImpl(File baseDir){
        this.bTree = new BTreeImpl<>();
        bTree.setPersistenceManager(persistenceManagerThatUpdatesDocTime(baseDir));
        this.commandStack = new StackImpl<>();
        this.wordCounts = new TrieImpl<>();
        this.memoryHeap = minHeapThatReferencesBTree();
        this.newlyReadInURIs = new HashSet<>();
        this.maxByteCount = -1;//use -1 to represent no limit
        this.maxDocCount = -1;
        this.usedMemory = 0;
        this.docCount = 0;
    }

    public DocumentStoreImpl(){
        this(null);
    }

    /**
     * Creates a MinHeap that compares URI's by finding their respective Documents in the
     * BTree and comparing them
     * @return the heap
     */
    private MinHeapImpl<URI> minHeapThatReferencesBTree(){
        return new MinHeapImpl<URI>(){
            @Override
            protected boolean isGreater(int i, int j) {
                Comparable[] el = super.elements;
                URI uri1 = (URI)el[i];
                URI uri2 = (URI)el[j];
                Document doc1 = bTree.get(uri1);
                Document doc2 = bTree.get(uri2);
                return doc1.compareTo(doc2) > 0;
            }
        };
    }

    /**
     * When a document is brought back in from disk, we keep track of it to know what to do with it later
     * ie does it need to go into the memory heap, or is it already there?
     * @return a PersistenceManager that does this
     */
    private PersistenceManager<URI, Document> persistenceManagerThatUpdatesDocTime(File baseDir){
        return new DocumentPersistenceManager(baseDir){
            @Override
            public Document deserialize(URI uri) throws IOException {
                Document doc =  super.deserialize(uri);
                newlyReadInURIs.add(uri);
                return doc;
            }
        };
    }

    @Override
    public Document get(URI uri) {
        Document document = this.bTree.get(uri);
        if (document != null) {
            handleDocumentFromBTree(document, System.nanoTime());
        }
        return document;
    }

    @Override
    public List<Document> search(String keyword) {
        List<URI> uriSearchResults = this.wordCounts.getAllSorted(keyword, byCountOf(keyword).reversed());
        List<Document> documentSearchResults = uriSearchResults.stream().map(bTree::get).toList();
        handleDocumentsFromBTree(documentSearchResults);
        return documentSearchResults;
    }

    @Override
    public List<Document> searchByPrefix(String keywordPrefix) {
        List<URI> uriSearchResultsPrefix = this.wordCounts.getAllWithPrefixSorted(keywordPrefix, byCountOfPrefix(keywordPrefix).reversed());
        List<Document> documentSearchResultsPrefix = uriSearchResultsPrefix.stream().map(bTree::get).toList();
        handleDocumentsFromBTree(documentSearchResultsPrefix);
        return documentSearchResultsPrefix;
    }

    @Override
    public int put(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if (uri == null || format == null) {
            throw new IllegalArgumentException("Either URI or format are null!");
        }
        Document prev;
        if (input == null) {//delete
            prev = this.deleteLogic(uri);
        } else {//put
            prev = putLogic(input, uri, format);
        }
        return prev == null ? 0 : prev.hashCode();
    }

    //The inner workings of how a "put" works - data reading in particular
    private Document putLogic(InputStream input, URI uri, DocumentFormat format) throws IOException {
        byte[] data = input.readAllBytes();
        Document doc;
        if (format == DocumentFormat.BINARY) {
            doc = new DocumentImpl(uri, data);
        } else {
            String txt = new String(data);
            doc = new DocumentImpl(uri, txt,null);
        }

        Document prev = this.bTree.get(uri);
        if (prev == null) {//undo should be to delete the document put
            this.commandStack.push(commandToDelete(doc));
        } else { //undo should be to insert the previous document
            this.commandStack.push(commandToPut(prev, doc));
            //removing some other doc from doc store
            this.removeFromAllLocations(prev.getKey());
        }
        if(maxByteCount<getBytes(doc) && maxByteCount!=-1){//if we have a document thats bigger than capacity, just send it straight to disk
            insertIntoBTreeAndTrie(uri, doc);
            try {
                bTree.moveToDisk(uri);
            } catch (Exception e) {
                return prev;
            }
        }else{
            this.insertEverywhere(uri, doc);
        }
        return prev;
    }

    @Override
    public boolean delete(URI uri) {
        return this.deleteLogic(uri) != null;
    }


    /**
     * If it exists, removes the document mapped to this URI from both the internal hashtable and word count trie
     *
     * @param uri the URI to delete the map mapping from
     * @return the deleted document, null if nothing deleted
     */
    private Document deleteLogic(URI uri) {
        Document removed = removeFromAllLocations(uri);
        if(removed!=null){ //we deleted something
            this.commandStack.push(commandToPut(removed, null));
        }
        return removed;
    }

    @Override
    public Set<URI> deleteAll(String keyword) {
        Set<URI> deletedDocuments = this.wordCounts.deleteAll(keyword);
        return processMassDelete(deletedDocuments);
    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<URI> deletedDocuments = this.wordCounts.deleteAllWithPrefix(keywordPrefix);
        return processMassDelete(deletedDocuments);
    }

    /**
     * Runs the logic to process the mass deletion of a set of documents after they are removed from the trie
     *
     * @param deleted the documents to remove from other parts of the doc store
     * @return the URI's deleted
     */
    private Set<URI> processMassDelete(Set<URI> deleted) {
        CommandSet<URI> undoCommands = new CommandSet<>();
        Set<URI> deletedUris = new HashSet<>();
        for (URI deletedURI : deleted) {//1. remove all words from word count trie
            Document deletedDoc = bTree.get(deletedURI);
            removeFromAllLocations(deletedDoc.getKey());
            deletedUris.add(deletedURI);
            undoCommands.addCommand(commandToPut(deletedDoc, null));//4. push the undo command to the stack
        }
        this.commandStack.push(undoCommands);
        return deletedUris;
    }

    @Override
    public void undo() throws IllegalStateException {
        Undoable lastCommand = this.commandStack.peek();
        if (lastCommand == null) {
            throw new IllegalStateException("No commands to undo!");
        }
        boolean successUndone = lastCommand.undo();
        //Check if either one command (then definitely undone) or it's a CommandSet and all were undone.
        if (lastCommand instanceof GenericCommand<?> || successUndone) {
            this.commandStack.pop();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void undo(URI uri) throws IllegalStateException {
        Stack<Undoable> helper = new StackImpl<>();
        Undoable current;
        do {//find next command with this URI
            current = this.commandStack.pop();
            helper.push(current);
        } while (current != null && !hasCommandForURI(current, uri));
        /*
         * If top command on helper stack is null, couldn't find one
         * Otherwise, we have a match!
         */
        Undoable toUndo = helper.peek();
        if (toUndo == null) {
            throw new IllegalStateException("No commands to undo for URI " + uri.toString());
        } else if (toUndo instanceof GenericCommand<?>) {
            toUndo.undo();
            helper.pop();
        } else {
            CommandSet<URI> commandSet = (CommandSet<URI>) toUndo;
            commandSet.undo(uri);
            if (commandSet.size() == 0) {//CommandSet - don't pop unless done
                helper.pop();
            }
        }
        while (helper.peek() != null) { //Put everything back on command stack
            this.commandStack.push(helper.pop());
        }
    }

    @Override
    public void setMaxDocumentCount(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Cannot set a max document count of a negative number");
        }
        this.maxDocCount = limit;
        handleMemoryOverflow();
    }

    @Override
    public void setMaxDocumentBytes(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Cannot set a max document byte count of a negative number");
        }
        this.maxByteCount = limit;
        handleMemoryOverflow();
    }

    /**
     * Returns a comparator that orders Documents by the count of a certain word
     *
     * @param keyword the word to order by
     * @return the comparator
     */
    private Comparator<URI> byCountOf(String keyword) {
        return Comparator.comparingInt(uri -> bTree.get(uri).wordCount(keyword));
    }

    /**
     * Returns a comparator that orders documents by the count of a prefix
     *
     * @param prefix the prefix to order by
     * @return the comparator
     */
    private Comparator<URI> byCountOfPrefix(String prefix) {
        return Comparator.comparingInt(uri -> getNumOccurencesOfPrefix(bTree.get(uri), prefix));
    }

    /**
     * Gets the number of occurrences of a prefix in a given document.
     * This number depends on the occurrence of the prefix in *separate* words. A duplicity
     * in the same word that occurs multiple times is not counted.
     *
     * @param document the document to search
     * @param prefix   the prefix to search for
     * @return the number of time the prefix occurs
     */
    private int getNumOccurencesOfPrefix(Document document, String prefix) {
        return document.getWords().stream()
                //.distinct() Not needed - getWords returns a Set
                .filter(it -> it.startsWith(prefix))//find words that start with the prefix. This part is inefficient
                .mapToInt(document::wordCount)//count how many times that word appears in the document
                .sum();//sum all of those counts together
    }

    /**
     * Generates a GenericCommand which inserts a document into the doc store, overwriting
     * anything at its previous URI
     *
     * @param previousDocument
     * @param newDocument
     * @return
     */
    private GenericCommand<URI> commandToPut(Document previousDocument, Document newDocument) {
        return new GenericCommand<>(previousDocument.getKey(), it -> {
            if (newDocument != null) {//if newDocument is null, that means we're undoing a delete.
                //remove references of the newer document
                removeFromAllLocations(newDocument.getKey());
            }
            //insert the previous doc
            this.insertEverywhere(previousDocument.getKey(), previousDocument);
            return true;
        });
    }

    /**
     * Generates a GenericCommand which deletes a document from the docstore
     *
     * @param docPut the document to delete
     * @return the generic command representing this action
     */
    private GenericCommand<URI> commandToDelete(Document docPut) {
        return new GenericCommand<>(docPut.getKey(), it -> {
            removeFromAllLocations(docPut.getKey());
            return true;
        });
    }

    /**
     * Checks if this Undoable has an undo command for a specific URI. If it's a single undo command,
     * checks if this command is for the target URI. If it's a CommandSet, checks if the set contains some
     * undo command for the given URI.
     *
     * @param someUndoCommand the undoable thing
     * @param uri             the uri
     * @return a boolean if the undoable has a command for the URI
     */
    @SuppressWarnings("unchecked cast")
    private boolean hasCommandForURI(Undoable someUndoCommand, URI uri) {
        boolean isGeneric = someUndoCommand instanceof GenericCommand<?>;
        if (isGeneric) {
            return ((GenericCommand<?>) someUndoCommand).getTarget().equals(uri);
        } else {
            assert someUndoCommand instanceof CommandSet<?>;
            return ((CommandSet<URI>) someUndoCommand).containsTarget(uri);
        }
    }

    /**
     * Removes document from hashtable, trie, the heap, decrements used memory & doc count
     *
     * @param uri                   the document to remove
     */
    private Document removeFromAllLocations(URI uri) {
        Document doc = bTree.get(uri);
        if(doc!=null){ // there is something to delete
            this.removeFromWordCount(doc);
            if(!newlyReadInURIs.contains(doc.getKey())){//exists in memory
                doc.setLastUseTime(Long.MIN_VALUE);
                this.memoryHeap.reHeapify(uri);
                URI removed = this.memoryHeap.remove();
                this.usedMemory -= getBytes(doc);
                this.docCount -= 1;
            }
            this.bTree.put(doc.getKey(), null);
            newlyReadInURIs.remove(uri);
        }
        return doc;
    }

    /**
     * Inserts document into the hashtable, trie, heap, increments mem usage and doc count.
     * If we overflow on memory/doc count limit, handles that by removing last used docs
     *
     * @param uri      the uri to map the doc to
     * @param document the document to insert
     * @return previous mapping to URI, if exists
     */
    private Document insertEverywhere(URI uri, Document document) {
        Document prev = insertIntoBTreeAndTrie(uri, document);
        document.setLastUseTime(System.nanoTime());
        memoryHeap.insert(uri);
        this.usedMemory += getBytes(document);
        this.docCount += 1;
        this.handleMemoryOverflow();
        return prev;
    }

    private Document insertIntoBTreeAndTrie(URI uri, Document document){
        Document prev = this.bTree.put(uri, document);
        if (document.getDocumentTxt() != null) {
            for (String word : document.getWords()) {
                this.wordCounts.put(word, uri);
            }
        }
        return prev;
    }

    private void updateMemoryFor(Document doc, long newTime){
        doc.setLastUseTime(newTime);
        memoryHeap.reHeapify(doc.getKey());
    }

    /**
     * Checks whether the doc was newly deserialized or not and acts appropiately
     * @param doc
     */
    private void handleDocumentFromBTree(Document doc,long newLastUseTime){
        if(newlyReadInURIs.contains(doc.getKey())){//newly read in
            newlyReadInURIs.remove(doc.getKey());
            insertIntoHeapAndHandleMemoryOverflow(doc, newLastUseTime);
        }else{//not newly read in
            updateMemoryFor(doc,newLastUseTime);
        }
    }

    private void handleDocumentsFromBTree(Collection<Document> docs){
        long newLastUseTime = System.nanoTime();
        for(Document doc:docs){
            handleDocumentFromBTree(doc, newLastUseTime);
        }
    }

    private void insertIntoHeapAndHandleMemoryOverflow(Document doc, long newLastTime){
        doc.setLastUseTime(newLastTime);
        this.usedMemory+=getBytes(doc);
        this.docCount+=1;
        memoryHeap.insert(doc.getKey());
        handleMemoryOverflow();
    }
    private void handleMemoryOverflow() {
        while (overflowingMemory()) {
            URI removedURI = memoryHeap.remove();
            Document removed = bTree.get(removedURI);
            this.usedMemory -= getBytes(removed);
            this.docCount -= 1;
            try {
                this.bTree.moveToDisk(removedURI);
                this.newlyReadInURIs.remove(removedURI);
            } catch (Exception ignored) {
                return;
            }
        }
    }

    private boolean overflowingMemory(){
        return (docCount > maxDocCount && maxDocCount != -1) || (usedMemory > maxByteCount && maxByteCount != -1);
    }

    private int getBytes(Document doc) {
        if (doc.getDocumentTxt() != null) {
            return doc.getDocumentTxt().getBytes().length;
        } else {
            return doc.getDocumentBinaryData().length;
        }
    }

    private boolean removeFromWordCount(Document doc) {
        boolean deletedSomething = false;
        for (String word : doc.getWords()) {
            URI deleted = this.wordCounts.delete(word, doc.getKey());
            if(deleted!=null){
                deletedSomething = true;
            }
        }
        return deletedSomething;
    }
}
