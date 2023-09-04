package project;

import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.Trie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static project.util.Assertions.project;

public class ClassNamesCorrectTest {

    @Test
    void classNames() {
        assertEquals(project("Stack"), Stack.class.getName());
        assertEquals(project("Trie"), Trie.class.getName());
        assertEquals(project("MinHeap"), MinHeap.class.getName());
    }
}
