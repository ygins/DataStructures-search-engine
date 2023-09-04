package project.impl;

import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static project.util.Assertions.*;

public class ClassMetaDataTest {

    @Test
    void classNames() {
        assertEquals(project("impl.StackImpl"), StackImpl.class.getName());
        assertEquals(project("impl.MinHeapImpl"),MinHeapImpl.class.getName());
    }

    @Test
    void classesOnlyImplementProjectSpecifications(){
        assertOnlyImplements(StackImpl.class, Stack.class);
        assertOnlyImplements(TrieImpl.class, Trie.class);
        assertExtends(MinHeapImpl.class, MinHeap.class);
    }
}
