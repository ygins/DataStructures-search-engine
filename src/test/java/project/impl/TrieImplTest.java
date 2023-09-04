package project.impl;

import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static java.util.Comparator.naturalOrder;
import static org.junit.jupiter.api.Assertions.*;
import static project.util.Assertions.assertThrowsIllegalArg;
import static project.util.Util.iterate;
import static project.util.Util.repeat;

public class TrieImplTest {
    private static String[] seashells;
    private Trie<Integer> trie;
    private static Comparator<Integer> integerComparator;

    @BeforeAll
    static void beforeAll() {
        seashells = "she sells seashells by the seashore".split(" ");
        assert seashells.length == 6;
        integerComparator = naturalOrder();
    }

    @BeforeEach
    void beforeEach() {
        this.trie = new TrieImpl<>();
    }

    //Intuitive basic testing
    @Test
    void basicPutGet() {
        insertSeashells();
        repeat(seashells.length,
                i -> assertEquals(i, getSingleValue(seashells[i])));
    }

    @Test
    void basicDelete() {
        insertSeashells();
        trie.deleteAll("she");
        for (int i = 1; i < seashells.length; i++) {
            assertEquals(i, getSingleValue(seashells[i]));
        }
    }

    //test put:
    //1. Next node is null
    @Test
    void putInsertNewNodes() {
        insertSeashells();
        trie.put("abracadabra", 6);//completely new word
        trie.put("scam", 4);//inserting right after first letter
        trie.put("shy", 3);//inserting last letter
        assertEquals(6, getSingleValue("abracadabra"));
        assertEquals(4, getSingleValue("scam"));
        assertEquals(3, getSingleValue("shy"));
    }

    //2. Next node is not null
    @Test
    void putAddingToExistingWords() {
        insertSeashells();
        insertSeashells(2);
        iterate(seashells, (index, string) -> assertEquals(Set.of(index, index + 2), getValues(string)));
    }

    @Test
    void throwsIfNullInput() {
        assertThrowsIllegalArg(() -> trie.put(null, 5));
        assertDoesNotThrow(() -> trie.put("Hello", null));
    }

    @Test
    void putThrowsForInvalidKey() {
        assertThrowsIllegalArg(() -> trie.put("-%47@", 4));
    }

    //test getAllSorted
    //1. It works
    @Test
    void testGetWorks() {
        this.basicDelete();
    }

    //2. Sorts in descending order
    @Test
    void getSortsDescendingOrder() {
        insertSeashells();
        insertSeashells(5);
        insertSeashells(2);
        iterate(seashells, (index, string) -> {
            List<Integer> expected = List.of(index + 5, index + 2, index);
            assertEquals(expected, trie.getAllSorted(string, reverseNaturalOrder()));
        });
    }

    //3. Is case sensitive
    @Test
    void getIsCaseSensitive() {
        insertSeashells();
        String[] capsSeashells = Arrays.stream(seashells).map(String::toUpperCase).toArray(String[]::new);
        iterate(capsSeashells, (index, string) -> trie.put(string, index + 10));
        //check getAllSorted
        iterate(seashells, (index, string) -> assertEquals(List.of(index), trie.getAllSorted(string, reverseNaturalOrder())));
        iterate(capsSeashells, (index, string) -> assertEquals(List.of(index + 10), trie.getAllSorted(string, reverseNaturalOrder())));
    }

    @Test
    void getAllSortedThrowsOnNullInput() {
        assertThrowsIllegalArg(() -> trie.getAllSorted(null, reverseNaturalOrder()));
        assertThrowsIllegalArg(() -> trie.getAllSorted("seashells", null));
    }

    @Test
    void getDoesNotReturnDuplicates() {
        insertSeashells();
        trie.put("she", 0);
        trie.put("she", 1);
        assertEquals(List.of(1, 0), trie.getAllSorted("she", reverseNaturalOrder()));
    }

    @Test
    void getDoesNotThrowIfInvalidKey() {
        assertDoesNotThrow(() -> trie.getAllSorted("!#$-->", reverseNaturalOrder()));
    }

    //test getAllWithPrefixSorted
    @Test
    void getPrefixWorks() {
        insertSeashells();
        Set<Integer> sPrefix = new HashSet<>(trie.getAllWithPrefixSorted("s", naturalOrder()));
        assertEquals(Set.of(0, 1, 2, seashells.length - 1), sPrefix);
        Set<Integer> seaPrefix = new HashSet<>(trie.getAllWithPrefixSorted("sea", naturalOrder()));
        assertEquals(Set.of(2, seashells.length - 1), seaPrefix);
    }

    @Test
    void getPrefixSortsDescending() {
        insertSeashells();
        List<Integer> sPrefix = trie.getAllWithPrefixSorted("s", reverseNaturalOrder());
        assertEquals(List.of(seashells.length - 1, 2, 1, 0), sPrefix);
        List<Integer> seaPrefix = trie.getAllWithPrefixSorted("sea", reverseNaturalOrder());
        assertEquals(List.of(seashells.length - 1, 2), seaPrefix);
    }

    @Test
    void getPrefixThrowsOnNullInput() {
        assertThrowsIllegalArg(() -> trie.getAllWithPrefixSorted("s", null));
        assertThrowsIllegalArg(() -> trie.getAllWithPrefixSorted(null, reverseNaturalOrder()));
    }

    @Test
    void getPrefixReturnsEmptySetIfNoValues() {
        assertEquals(0, trie.getAllWithPrefixSorted("Hello", reverseNaturalOrder()).size());
    }

    @Test
    void getPrefixDoesNotReturnDuplicates() {
        trie.put("apple", 10);
        trie.put("application", 10);
        trie.put("apple", 5);
        trie.put("application", 7);
        trie.put("banana", 1);
        assertEquals(List.of(10, 7, 5), trie.getAllWithPrefixSorted("appl", reverseNaturalOrder()));
    }

    @Test
    void returnsEmptySetForInvalidKey() {
        trie.getAllWithPrefixSorted("(*&^%$#", reverseNaturalOrder());
    }

    //test deleteAll
    @Test
    void deleteAllWorks() {
        insertSeashells();
        insertSeashells(2);
        Set<Integer> deleted = trie.deleteAll("she");
        assertEquals(Set.of(0, 2), deleted);
        assertTrue(trie.deleteAll("she").isEmpty());
    }

    @Test
    void deleteAllDoesNotAffectTrie() {
        //for each value in array, remove it and check if trie is altered
        for (int i = 0; i < seashells.length; i++) {
            insertSeashells();
            Set<Integer> deleted = trie.deleteAll(seashells[i]);
            assertEquals(Set.of(i), deleted);
            for (int e = 0; e < seashells.length; e++) {
                if (e != i) {
                    assertEquals(e, getSingleValue(seashells[e]));
                }
            }
            trie = new TrieImpl<>();//reset
        }
    }

    @Test
    void deleteAlLReturnsEmptySetIfNoValues() {
        insertSeashells();
        assertEquals(0, trie.deleteAll("Hello").size());
    }

    @Test
    void deleteAllIsCaseSensitive() {
        Runnable resetTrie = () -> {
            trie = new TrieImpl<>();
            insertSeashells();
            iterate(seashells, (index, string) -> trie.put(string.toUpperCase(), index));
        };
        resetTrie.run();
        iterate(seashells, (index, string) -> trie.deleteAll(string));
        iterate(seashells, (index, string) -> {
            assertEquals(0, trie.getAllSorted(string, reverseNaturalOrder()).size());
            assertEquals(List.of(index), trie.getAllSorted(string.toUpperCase(), reverseNaturalOrder()));
        });
    }

    @Test
    void deleteAllDoesNotThrowForInvalidKey() {
        assertDoesNotThrow(() -> trie.deleteAll(")(*&^%$#@"));
    }

    //delete
    @Test
    void deleteWorks() {
        insertSeashells();
        insertSeashells(2);
        int deleted = trie.delete("seashells", 4);
        assertEquals(4, deleted);
        assertEquals(2, getSingleValue("seashells"));
    }

    @Test
    void deleteReturnsNullIfKeyNotThere() {
        insertSeashells();
        assertNull(trie.delete("apples", 1));
    }

    @Test
    void deleteReturnsNullIfValueNotThere() {
        insertSeashells();
        assertNull(trie.delete("she", 10));
    }

    @Test
    void deleteDoesNotThrowForInvalidKey() {
        assertDoesNotThrow(() -> trie.delete("09876", 4));
    }

    //deleteAllWithPrefix
    @Test
    void deleteAllWithPrefixWorks() {
        insertSeashells();
        deleteAllWithPrefix("s");
        this.trie = new TrieImpl<>();
        insertSeashells();
        deleteAllWithPrefix("sea");
    }

    @Test
    void deleteAllWithPrefixCaseSensitive() {
        Runnable resetTrie = () -> {
            this.trie = new TrieImpl<>();
            insertSeashells();
            trie.put("she", 2);
            trie.put("She", 3);
        };
        resetTrie.run();
        assertEquals(indicesThatStartWith(seashells, "s"), trie.deleteAllWithPrefix("s"));
        resetTrie.run();
        assertEquals(Set.of(3), trie.deleteAllWithPrefix("S"));
    }

    @Test
    void deleteAllWithPrefixAlsoDeletesPrefix() {
        insertSeashells();
        trie.put("sea", 10);
        deleteAllWithPrefix("sea", 10);
        assertTrue(trie.getAllSorted("sea", naturalOrder()).isEmpty());
    }

    @Test
    void deleteAllWithPrefixWithNoValuesReturnsEmpty() {
        insertSeashells();
        assertEquals(0, trie.deleteAllWithPrefix("Hello").size());
    }

    @Test
    void deleteAlLWithPrefixThrowsOnNullInput() {
        assertThrowsIllegalArg(() -> trie.deleteAllWithPrefix(null));
    }

    @Test
    void deleteAllWithPrefixDoesNotThrowOnInvalidKey() {
        assertDoesNotThrow(() -> trie.deleteAllWithPrefix(")(*&^%$#@"));
    }

    /**
     * assuming each word is mapped to its index in the initial string,
     * calls deleteAll and checks if it worked, and if it altered the initial Trie
     **/
    private void deleteAllWithPrefix(String prefix, Integer alsoDeleted) {
        Set<Integer> valuesToDelete = indicesThatStartWith(seashells, prefix);
        if (alsoDeleted != null) {
            valuesToDelete.add(alsoDeleted);
        }
        Set<Integer> deleted = trie.deleteAllWithPrefix(prefix);
        assertEquals(valuesToDelete, deleted);
        iterate(seashells, (index, string) -> {
            if (valuesToDelete.contains(index)) {
                assertTrue(trie.getAllSorted(string, naturalOrder()).isEmpty());
            } else {
                assertEquals(index, getSingleValue(string));
            }
        });
    }

    private void deleteAllWithPrefix(String prefix) {
        deleteAllWithPrefix(prefix, null);
    }

    @Test
    void worksWithAllLettersAndNumbers() {
        List<Character> chars = new ArrayList<>();
        for (char c = '0'; c <= '9'; c++) {
            chars.add(c);
        }
        for (char c = 'a'; c <= 'z'; c++) {
            chars.add(c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            chars.add(c);
        }
        assertEquals(62, chars.size());
        for (char aChar : chars) {
            this.trie.put(aChar + "", (int) aChar);
        }
        for (char aChar : chars) {
            assertEquals(aChar, getSingleValue(aChar + ""));
        }
    }


    //There's no real good way to test if the key is completely removed with the current specifications.
    //here are some values to test using a debugger...
    @Test
    void deleteRemoves() {
        trie.put("she", 1);
        trie.put("shell", 2);
        trie.delete("shell", 2);
        trie = new TrieImpl<>();
        trie.put("sea", 3);
        trie.put("seashore", 5);
        trie.put("seashell", 6);
        trie.put("s", 2);
        trie.deleteAllWithPrefix("sea"); //should only have s left
    }

    private void insertSeashells() {
        this.insertSeashells(0);
    }

    private void insertSeashells(int modifier) {
        iterate(seashells, (index, string) -> trie.put(string, index + modifier));
    }

    private int getSingleValue(String key) {
        List<Integer> values = trie.getAllSorted(key, integerComparator);
        assertEquals(1, values.size());
        return values.get(0);
    }

    private Set<Integer> getValues(String key) {
        return new HashSet<>(trie.getAllSorted(key, naturalOrder()));
    }

    private Set<Integer> indicesThatStartWith(String[] arr, String prefix) {
        Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].startsWith(prefix)) {
                indices.add(i);
            }
        }
        return indices;
    }

    private Comparator<Integer> reverseNaturalOrder() {
        return Comparator.comparingInt(it -> it * -1);
    }
}
