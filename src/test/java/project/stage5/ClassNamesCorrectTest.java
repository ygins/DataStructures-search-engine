package project.stage5;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static project.util.Assertions.project;

public class ClassNamesCorrectTest {

    @Test
    void classNames() {
        assertEquals(project("stage5.Document"), Document.class.getName());
        assertEquals(project("stage5.DocumentStore"), DocumentStore.class.getName());
    }
}
