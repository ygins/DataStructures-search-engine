package project.impl;

import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * stage 4
 * I've been learning about how unit tests work throughout this project, so I've decided to try something
 * new here, rather than use my other files I have
 */
public class MinHeapImplTest {
    private MinHeap<Integer> integerMinHeap;
    private MinHeap<HeapElement> objectMinHeap;

    @BeforeEach
    void setup() {
        this.integerMinHeap = new MinHeapImpl<>();
        this.objectMinHeap = new MinHeapImpl<>();
    }

    //adds a bunch of numbers in random order, asserts we get each one out in order
    //this isn't really a test for my code, as I did not implement insert and remove, really
    //just a test for sanity - did I break anything
    @Test
    void heapWorksSanityCheck() {
        List<Integer> shuffledElements = new ArrayList<>();
        List<Integer> unShuffledElements = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            shuffledElements.add(i);
            unShuffledElements.add(i);
        }
        Collections.shuffle(shuffledElements);
        for (int item : shuffledElements) {
            this.integerMinHeap.insert(item);
        }
        for (int orderedElement : unShuffledElements) {
            assertEquals(orderedElement, integerMinHeap.remove());
        }
        assertThrows(NoSuchElementException.class, () -> integerMinHeap.remove());
    }

    /**
     * Tests if we can reheapify an element downwards
     *
     * @param indexToMoveDown the index of the item in the element list we will down (corresponds to size)
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    void reheapifyElementsMoveDown(int indexToMoveDown) {
        var elementsInHeap = generateHeapElementsOfVariousSizes();
        var elementToMoveDown = elementsInHeap.get(indexToMoveDown);
        elementToMoveDown.setValue("I am much longer than all of the other values, or so they tell me...");
        objectMinHeap.reHeapify(elementToMoveDown);
        assertIsLastElementInHeap(elementToMoveDown, elementsInHeap.size());
    }

    /**
     * Tests if we can reheapify an element upwards
     *
     * @param indexToMoveUp the index of the element we are moving up (corresponds to size)
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9})
    void reheapifySmallerElementsMoveUp(int indexToMoveUp) {
        var elementsInHeap = generateHeapElementsOfVariousSizes();
        var elementToMoveUp = elementsInHeap.get(indexToMoveUp);
        elementToMoveUp.setValue("");
        objectMinHeap.reHeapify(elementToMoveUp);
        assertEquals(elementToMoveUp, objectMinHeap.remove());
    }

    /**
     * Tests if reheapify moves the element halfway in the heap (not all the way to the top nor bottom)
     */
    @Test
    void reheapifyCanMoveHalfway() {
        var elementsInHeap = generateHeapElementsOfVariousSizes();
        var elementToMoveUp = elementsInHeap.get(4);
        elementToMoveUp.setValue("5".repeat(5));
        objectMinHeap.reHeapify(elementToMoveUp);
        assertNotEquals(elementToMoveUp, objectMinHeap.remove());
    }

    /**
     * Generates [1,22,333,4444,55555,666666....10101010101010101010101010101010101010]
     *
     * @return the list of elements
     */
    private List<HeapElement> generateHeapElementsOfVariousSizes() {
        List<HeapElement> heapElements = new ArrayList<>();
        for (int size = 1; size <= 10; size++) {
            heapElements.add(new HeapElement(String.valueOf(size).repeat(size)));
        }
        heapElements.forEach(objectMinHeap::insert);
        return heapElements;
    }

    @Test
    void reheapifyThrowsNoSuchElementExceptionIfDoesNotExist() {
        this.integerMinHeap.insert(5);
        this.integerMinHeap.insert(3);
        assertDoesNotThrow(() -> this.integerMinHeap.reHeapify(3));
        assertThrows(NoSuchElementException.class, () -> this.integerMinHeap.reHeapify(1));
    }

    @Test
    void reheapifyThrowsNoSuchElementExceptionForNullInput(){
        assertThrows(NoSuchElementException.class, ()->this.integerMinHeap.reHeapify(null));
    }

    /**
     * Checks if the element is the last one in the heap. removes all elements
     *
     * @param e        the element to check for
     * @param heapSize the expected size of the heap
     */
    private void assertIsLastElementInHeap(HeapElement e, int heapSize) {
        while (heapSize > 1) {
            this.objectMinHeap.remove();
            heapSize--;
        }
        assertEquals(e, objectMinHeap.remove());
    }


    /**
     * A simple simulation of a heap element that has a dynamically changeable value
     */
    private static final class HeapElement implements Comparable<HeapElement> {
        private String value;

        private HeapElement(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String newValue) {
            this.value = newValue;
        }

        @Override
        public int compareTo(HeapElement o) {
            return Integer.compare(value.length(), o.value.length());
        }
    }
}
