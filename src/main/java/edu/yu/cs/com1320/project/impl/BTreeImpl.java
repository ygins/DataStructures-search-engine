package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * A B-Tree Implementation with persistent storage
 *
 * @param <Key>
 * @param <Value>
 */
public class BTreeImpl<Key extends Comparable<Key>, Value> implements BTree<Key, Value> {

    private static final int MAX = 6;
    private Node<Key, Value> root;
    private int height;
    private PersistenceManager<Key, Value> pm;

    //The sentinel will alwaus be less than any other entry - see lessThan and equals methods
    private final Comparable<Key> sentinelKey;

    public BTreeImpl() {
        this.root = new Node<>();
        this.height = 0;
        this.sentinelKey = o -> {
            return -1; //also less than anything else
        };
        //sentinel is the first entry
        this.root.entries[0] = new Entry<>(sentinelKey,null,null,false);
        this.root.entryCount++;
    }

    @Override
    public Value get(Key k) {
        Entry<Key, Value> entry = get(root, k, height);
        if(entry==null){//no such entry exists
            return null;
        }
        assert entry.getChild()==null; //if it's just a parent node, we have a problem
        //it is either a reference to disk, or if not, has the value (nullable)
        if(entry.getReferenceToDisk()!=null){
            Value valueFromDisk=this.readAndDeleteFromDisk(entry.getReferenceToDisk());
            assert valueFromDisk!=null;
            entry.setIsReferenceToDisk(false);
            entry.setValue(valueFromDisk);
            return valueFromDisk;
        }else{
            return entry.getValue();
        }
    }

    private Entry<Key, Value> get(Node<Key, Value> currentNode, Key k, int height) {
        Entry<Key, Value>[] entries = currentNode.entries;
        if (height == 0) {//external node
            for (Entry<Key, Value> entry : entries) {
                if (entry!=null && equal(k, entry.getKey())) {
                    return entry;
                }
            }
        } else {
            for (int i = 0; i < currentNode.entryCount; i++) {
                if (i + 1 == currentNode.entryCount || (lessThan(k, entries[i + 1].getKey()))) {
                    return get(entries[i].child, k, height - 1);
                }
            }
        }
        return null;
    }

    private Value readAndDeleteFromDisk(Key key){
        try {
            Value value = pm.deserialize(key);
            pm.delete(key);
            return value;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Value put(Key k, Value v) {
        Entry<Key, Value> alreadyThere = this.get(this.root, k, this.height);
        if (alreadyThere != null) {
            return this.putIfEntryAlreadyExists(k,v,alreadyThere);
        } else {
            Node<Key, Value> newNode = this.put(this.root, k, v, this.height);
            if (newNode != null) {//we did a split, let's deal with it. Need to make a new root to hold
                //the two nodes
                Node<Key, Value> newRoot = new Node<>();
                newRoot.setEntryCount(2);
                newRoot.entries[0] = new Entry<>(this.root.entries[0].getKey(), null, (Node<Key,Value>)this.root,false);
                newRoot.entries[1] = new Entry<>(newNode.entries[0].getKey(), null, newNode,false);
                this.root = newRoot;
                this.height++;
            }
            return null;//no previous value to return
        }
    }

    /**
     * Called if the entry in a put call already exists in the b tree
     * Deletes old file from disk if applicable
     * @param k the key
     * @param v the value
     * @param entry the entry
     * @return previous value (either on disk or in memory)
     */
    private Value putIfEntryAlreadyExists(Key k, Value v, Entry<Key, Value> entry){
        Value prev = null;
        if(entry.getReferenceToDisk()!=null){//value on disk
            try {
                prev = pm.deserialize(k);
                pm.delete(k);
            } catch (IOException e) {
                return null;
            }
            entry.setIsReferenceToDisk(false);
            entry.setValue(v);
        }else{
            prev = entry.getValue();
            entry.setValue(v);
        }
        return prev;
    }

    //Recursive logic for public put method
    private Node<Key, Value> put(Node<Key, Value> currentNode, Key k, Value value, int height) {
        int j;
        Entry<Key, Value> newEntry = new Entry<>(k, value, null,false);
        if (height == 0) {//leaf
            for (j = 0; j < currentNode.entryCount; j++) {
                if (lessThan(k, currentNode.entries[j].getKey())) {
                    break;//Will go in index j
                }
            }
        } else { //internal node
            for (j = 0; j < currentNode.entryCount; j++) {
                if ((j + 1 == currentNode.entryCount) || lessThan(k, currentNode.entries[j + 1].getKey())) {//should recur into the child here
                    Node<Key, Value> newNode = this.put(currentNode.entries[j++].child, k, value, height - 1);
                    if (newNode == null) {//we didn't split, return null
                        return null;
                    }
                    newEntry.setKey(newNode.entries[0].getKey());//key to point to child node (same key)
                    newEntry.setValue(null);//no value - just a pointer to a child node
                    newEntry.child = newNode;//child node
                    break;
                }
            }
        }
        for (int i = currentNode.entryCount; i > j; i--) {//move over all entries greater
            currentNode.entries[i] = currentNode.entries[i - 1];
        }
        currentNode.entries[j] = newEntry;//insert
        currentNode.entryCount++;
        if (currentNode.entryCount < MAX) {
            return null;
        } else {//Need to split
            return split(currentNode); //TODO - height parameter?
        }
    }

    /**
     * Splits the target node into two, copying over the last bunch of entries from the current
     * node to the new one
     * @param currentNode the node to split
     * @return the newly created node
     */
    private Node<Key, Value> split(Node<Key, Value> currentNode) {
        Node<Key, Value> newNode = new Node<>();
        int halfOfMax = MAX / 2;
        for (int i = 0; i < halfOfMax; i++) {
            newNode.entries[i] = currentNode.entries[halfOfMax + i];
            currentNode.entries[halfOfMax + i] = null;
        }
        currentNode.entryCount = halfOfMax;
        newNode.entryCount = halfOfMax;
        return newNode;
    }

    @Override
    public void moveToDisk(Key k) throws Exception {
        if(k==null){
            throw new IllegalArgumentException("Null key input");
        }
        if(pm==null){
            throw new IllegalStateException("Persistence manager is null!");
        }
        Entry<Key, Value> entry = get(this.root, k,this.height);
        assert entry != null;
        Value valueInMemory = entry.getValue();
        if(valueInMemory==null){
            throw new NoSuchElementException("No element to move to disk!");
        }
        pm.serialize(k, valueInMemory);
        entry.setIsReferenceToDisk(true);
        entry.setValue(null);
    }

    @Override
    public void setPersistenceManager(PersistenceManager<Key, Value> pm) {
        this.pm = pm;
    }


    /**
     * one is always equal to two if both are the sentinel key, otherwise, call compare
     * @param one the first comparable
     * @param two the second comparable
     * @return if the two are equal
     */
    private boolean equal(Comparable<Key> one, Comparable<Key> two) {
        return two == sentinelKey ? one==two : one.compareTo((Key) two) == 0;
    }

    /**
     * one is always less than two if two is the sentinel. Otherwise, call compare (both Keys)
     * @param one the first comparable
     * @param two the second comparable
     * @return if the first is less than the second
     */
    private boolean lessThan(Comparable<Key> one, Comparable<Key> two) {
        return two != sentinelKey && one.compareTo((Key) two) < 0;
    }

    private static final class Node<Key extends Comparable<Key>, Value> {

        private int entryCount;
        Entry<Key, Value>[] entries;

        @SuppressWarnings("unchecked")
        Node() {
            this.entries = (Entry<Key, Value>[]) new Entry[MAX];
            this.entryCount = 0;
        }

        void setEntryCount(int newCount) {
            this.entryCount = newCount;
        }
    }

    /**
     * Entry in the B-Tree
     * Value can be one of three things - Value (it is in memory), Node (child), or Key (Reference to file system)
     */
    private static final class Entry<Key extends Comparable<Key>, Value> {
        private Comparable<Key> key;
        private Value value;
        private Node<Key, Value> child;
        private boolean isReferenceToDisk;

        Entry(Comparable<Key> key, Value value, Node<Key, Value> child, boolean isReferenceToDisk) {
            this.key = key;
            this.value = value;
            this.child = child;
            this.isReferenceToDisk = isReferenceToDisk;
        }

        Node<Key, Value> getChild(){
            return this.child;
        }

        Comparable<Key> getKey() {
            return key;
        }

        Value getValue() {
            return value;
        }

        Key getReferenceToDisk() {
            return isReferenceToDisk ? (Key)key : null;
        }

        void setValue(Value newValue) {
            this.value = newValue;
        }

        void setKey(Comparable<Key> newKey) {
            this.key = newKey;
        }

        void setIsReferenceToDisk(boolean isReferenceToDisk){
            this.isReferenceToDisk = isReferenceToDisk;
        }
    }

}
