package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TrieImpl<T> implements Trie<T> {

    private final Node<T> root;

    public TrieImpl() {
        this.root = new Node<T>();
    }

    @Override
    public void put(String key, T val) {
        if(key==null){
            throw new IllegalArgumentException("Cannot call Trie#put with a null key");
        }
        if(val!=null){
            this.root.put(key, val);
        }
    }

    @Override
    public List<T> getAllSorted(String key, Comparator<T> comparator) {//ASK what to do if null
        if(key==null||comparator==null){
            throw new IllegalArgumentException("Cannot call Trie#getAllSorted with negative arguments");
        }
        Set<T> values = this.root.get(key);
        if(values==null){
            return new ArrayList<>();
        }else{
            return values.stream().sorted(comparator).toList();
        }
    }

    @Override
    public List<T> getAllWithPrefixSorted(String prefix, Comparator<T> comparator) {
        if(prefix==null||comparator==null){
            throw new IllegalArgumentException("Cannot call Trie#getAllWithPrefixSorted with negative arguments");
        }
        return this.getValuesWithPrefix(prefix).distinct().sorted(comparator).toList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<T> deleteAllWithPrefix(String prefix) {
        if(prefix==null){
            throw new IllegalArgumentException("Cannot call Trie#deleteAllWithPrefix with a negative prefix argument");
        }
        Node<T> endOfPrefix = this.root.walk(prefix, null);
        if (endOfPrefix == null) {
            return new HashSet<>();
        }
        //get all with prefix
        Set<T> values = this.getValuesWithPrefix(prefix).collect(Collectors.toSet());
        //detach subtree:
        //1. Remove all the prefix's edges
        endOfPrefix.edges.clear();
        //2. delete the prefix itself from the trie
        this.deleteAll(prefix);
        return values;
    }

    /**
     * Gets all values associated with a key that begins with the given prefix
     * @param prefix the prefix
     * @return the values
     */
    private Stream<T> getValuesWithPrefix(String prefix) {
        Set<Node<T>> nodes = this.root.getAllValueNodesWithPrefix(prefix);
        if(nodes==null){
            return Stream.empty();
        }
        else return nodes.stream().flatMap(node -> node.getValues().stream());
    }

    @Override
    public Set<T> deleteAll(String key) {
        if(key==null){
            throw new IllegalArgumentException("Cannot call Trie#deleteAll with a null key");
        }
        Set<T> deleted = this.root.delete(key,null);
        return deleted!=null ? deleted:new HashSet<>();
    }

    @Override
    public T delete(String key, T val) {
        if(key==null||val==null){
            throw new IllegalArgumentException("Cannot call Trie#delete with a null key or value");
        }
        Set<T> deleted = this.root.delete(key, val);
        assert deleted != null;
        if (deleted.isEmpty()) {
            return null;
        }
        assert deleted.size() == 1;
        return deleted.iterator().next();
    }

    /**
     * Represents a trie node. Each node has a possible 62 children (or "edges") for A-Z, a-z and 0-9.
     * Each node represents a character, although that information is stored in the parent node, not the child.
     * Each node can be associated with a Set of values.
     * @param <T>
     */
    private final static class Node<T> {
        private final Edges edges;
        private Set<T> values;

        Node(){
            this.edges = new Edges();
            this.values = null;
        }

        /**
         * Gets the values at the node associated with the given key
         * @param key the key
         * @return the associated values
         */
        private Set<T> get(String key) {
            //we don't want to do anything while walking down the trie, so pass null for action
            Node<T> lastNodeInPath = walk(key, null);
            return lastNodeInPath == null ? null : lastNodeInPath.getValues();
        }

        /**
         * Gets the nodes that:
         *  A) represent a key beginning with the given prefix and
         *  B) have some associated value
         * @param prefix the prefix
         * @return the nodes
         */
        private Set<Node<T>> getAllValueNodesWithPrefix(String prefix) {
            Node<T> lastNodeInPrefix = walk(prefix, null);
            return lastNodeInPrefix == null ? null : lastNodeInPrefix.findAllNodesWithValues(new HashSet<>());
        }

        /**
         * Returns all nodes that have some value associated with its key, starting and including
         * this node as the root of the search
         *
         * @param results the set to put results in
         * @return the results as a Set
         */
        private Set<Node<T>> findAllNodesWithValues(Set<Node<T>> results) {
            Queue<Node<T>> toSearch = new ArrayDeque<>(30);
            toSearch.add(this);
            while (toSearch.peek() != null) {
                Node<T> current = toSearch.remove();
                if (current.getValues() != null) {
                    results.add(current);
                }
                for(Node<T> edge: current.edges){
                    if(edge!=null){
                        toSearch.add(edge);
                    }
                }
            }
            return results;
        }

        /**
         * Associates a node with a value (adding to its set of values)
         * @param key the key to associate the value to
         * @param value the value
         */
        private void put(String key, T value) {
            for(char c:key.toCharArray()){
                if(edges.charToIndex(c)==-1){
                    throw new IllegalArgumentException(key+" contains non-alphanumeric characters!");
                }
            }
            Node<T> lastNodeInPath = walk(key, (current, nextIndex) -> {
                if (current.edges.nodeAt(nextIndex) == null) {//need to add a new node
                    current.edges.addNodeAt(nextIndex, new Node<>());
                }
            });
            if(lastNodeInPath!=null){//can be null if invalid key
                lastNodeInPath.addValue(value);
            }
        }

        /**
         * Delete given value at key. If no value passed in, will delete all values
         * @param key key to delete value from
         * @param value value to delete
         */
        private Set<T> delete(String key, T value) {
            Runnable deleteNode = null;//this runnable can potentially be used to delete a node from the tree
            Node<T> currentNode = this;
            char[] keyChars = key.toCharArray();
            for (char keyChar : keyChars) {
                Node<T> nextNode = currentNode.edges.nodeAtChar(keyChar);
                if (nextNode == null) {
                    return new HashSet<>();//never got to the end
                }
                if (!nextNode.okayToDelete()) {//dangerous to delete next node so if we initialized the deleting runnable, make it null
                    deleteNode = null;
                } else if (deleteNode == null) {//can totally delete the next node,
                    //and there is no node higher than it being deleted (deleteNode is null)
                    Node<T> finalCurrentNode = currentNode;
                    deleteNode = () -> {
                        finalCurrentNode.edges.removeNodeAt(edges.charToIndex(keyChar));
                    };
                }
                currentNode = nextNode;
            }
            //we are at the final node
            return this.deleteValuesFrom(currentNode, value, deleteNode);
        }

        /**
         * Removes a given value from a node. If we can remove part of the trie, that is done
         * with the passed in Runnable
         * @param node the node to remove the value from
         * @param toDelete what to delete. If null, will delete all values
         * @param removeNode Runnable that will safely remove part of the trie. If null will not run
         * @return the values removed (empty set if no values removed)
         */
        private Set<T> deleteValuesFrom(Node<T> node, T toDelete, Runnable removeNode) {
            Set<T> removed;
            if(node.getValues()==null){
                removed = new HashSet<>();//didn't remove anything
            }
            else if (toDelete == null || Set.of(toDelete).equals(node.getValues())) {//remove all values
                removed = node.getValues();
                node.values = null;
                if(removeNode!=null){//there is some node we can delete
                    removeNode.run();//removed all values, so can break apart Trie if possible
                }
            } else {//remove one value
                removed = node.getValues().remove(toDelete) ? Set.of(toDelete) : new HashSet<>();
            }
            return removed;
        }

        /**
         * Checks if, in a vacuum, assuming we are deleting the node below it, deleting this node will
         * not break anything (ie it's only child is being deleted, and does not contain a value)
         */
        private boolean okayToDelete() {
            return (this.edges.amount() == 1 && (this.values == null || this.values.isEmpty()))//not the last node, and doesn't contain any values that should remain
                    || (this.edges.amount()==0);//this is the last node, by definition ok to delete
        }

        /**
         * Walks through the Trie with a given path defined by the key
         *
         * @param key what path to take
         * @param action what to do, (currentNode, nextNode)->{}
         * @return the last node in the path if exists, otherwise null
         */
        private Node<T> walk(String key, BiConsumer<Node<T>, Integer> action) {
            Node<T> currentNode = this;
            for (char keyChar : key.toCharArray()) {
                int nextIndex = edges.charToIndex(keyChar);
                if(nextIndex==-1){
                    return null;
                }
                if (action != null) {
                    action.accept(currentNode, nextIndex);
                }
                Node<T> nextNode = currentNode.edges.nodeAt(nextIndex);
                if (nextNode == null) {
                    return null;
                }
                currentNode = nextNode;
            }
            return currentNode;
        }

        /**
         * Adds a value to this node
         * @param newValue the value to add
         */
        private void addValue(T newValue) {
            if (this.values == null) {
                this.values = new HashSet<>();
            }
            this.values.add(newValue);
        }

        private Set<T> getValues() {
            return this.values;
        }

        /**
         * Contains the edges (branches) of this node.
         * Contains logic for efficiently storing & retrieving the amount of edges a node has
         */
        private final class Edges implements Iterable<Node<T>>{
            private Node<T>[] edges;
            private int numEdges;

            private Edges(){
                this.clear();//initialize variables to empty
            }

            /**
             * Gets the node in the trie found at the edge with the given character
             * @param c the character
             * @return the node, or null if not there
             */
            Node<T> nodeAtChar(char c){
                int targetIndex = charToIndex(c);
                return targetIndex!=-1 ? this.edges[targetIndex]:null;
            }

            /**
             * Gets the node at the given index in the array
             * @param index the index
             * @return the node, or null if not there
             */
            Node<T> nodeAt(int index){
                return edges[index];
            }

            /**
             * Adds an edge to this one, increasing the node count if relevant
             * @param index the index to add it at
             * @param newNode the node to add
             */
            void addNodeAt(int index, Node<T> newNode){
                Node<T> currentNode = this.edges[index];
                this.edges[index]=newNode;
                if(currentNode==null){
                    this.numEdges++;
                }
            }

            /**
             * Removes an edge from this one, decreasing the  node amount if relevant
             * @param index the index to remove from
             */
            void removeNodeAt(int index){
                this.edges[index]=null;
                this.numEdges--;
            }

            @SuppressWarnings("unchecked")
            void clear(){
                int ALPHABET_SIZE = 62;
                this.edges=(Node<T>[]) new Node[ALPHABET_SIZE];
                this.numEdges=0;
            }

            /**
             * Returns the amount of edges this node has. O(1) operation
             * @return the amount of edges
             */
            int amount(){
                return numEdges;
            }

            @Override
            public Iterator<Node<T>> iterator() {
                return Arrays.asList(edges).iterator();
            }

            /**
             * Assigns each character in A-Z or a-z to an index ranging from 0-25 for A-Z and 26-51 for a-z
             * and 52-61 for 0-9
             *
             * @param c the character
             * @return the array index
             */
            private int charToIndex(char c) {
                //65-90 = A-Z
                //97-122 = a-z
                if(isLetter(c)){
                    return Character.isUpperCase(c) ? c - 65 : c - 71;
                }else if(isNumber(c)){
                    return c+4;
                }else return -1;
            }

            private boolean inRange(char c, int from, int to) {
                return from <= c && c <= to;
            }

            private boolean isLetter(char c){
                return inRange(c, 65, 90) || inRange(c, 97, 122);
            }

            private boolean isNumber(char c){
                return inRange(c,48,57);
            }
        }
    }

}
