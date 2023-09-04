package project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage5.impl.DocumentStoreImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static project.util.Assertions.*;

public class ClassMetaDataTest {

    @Test
    void stage3ClassNames() {
        assertEquals(project("stage5.impl.DocumentImpl"), DocumentImpl.class.getName());
        assertEquals(project("stage5.impl.DocumentStoreImpl"), DocumentStoreImpl.class.getName());
    }

    @Test
    void classesOnlyImplementProjectSpecifications() {
        assertOnlyImplements(DocumentImpl.class, Document.class);
        assertOnlyImplements(DocumentStoreImpl.class, DocumentStore.class);
    }
}
