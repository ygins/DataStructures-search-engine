package project.util;

import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class Assertions {
    public static void assertImplements(Class<?> clazz, Class<?> interfase){
        assertTrue(implementz(clazz, interfase));
    }

    public static void assertOnlyImplements(Class<?> clazz, Class<?> interfase){
        assertImplements(clazz, interfase);
        assertEquals(1, clazz.getInterfaces().length);
    }

    public static void assertDoesNotImplement(Class<?> clazz, Class<?> interfase){
        assertFalse(implementz(clazz,interfase));
    }

    private static boolean implementz(Class<?> clazz, Class<?> interfase){
        return Arrays.asList(clazz.getInterfaces()).contains(interfase);
    }

    public static void assertDoesNotImplementAnything(Class<?> clazz){
        assertEquals(0,clazz.getInterfaces().length);
        assertEquals(Object.class,clazz.getSuperclass());
    }

    public static void assertExtends(Class<?> clazz, Class<?> parent){
        assertEquals(clazz.getSuperclass(), parent);
    }

    public static void assertThrowsIllegalArg(Executable runnable){
        assertThrows(IllegalArgumentException.class,runnable);
    }

    public static <T> void assertEmpty(Collection<T> collection){
        assertEquals(0, collection.size());
    }

    public static String project(String str){
        return "edu.yu.cs.com1320.project."+str;
    }
}
