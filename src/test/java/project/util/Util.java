package project.util;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class Util {
    public static void repeat(int times, IntConsumer action) {
        for (int i = 0; i < times; i++) {
            action.accept(i);
        }
    }

    public static <T> void iterate(T[] arr, BiConsumer<Integer, T> action) {
        for (int i = 0; i < arr.length; i++) {
            action.accept(i, arr[i]);
        }
    }

    public static int count(String word, String text) {
        String[] words = text.split(" ");
        return (int) Arrays.stream(words).filter(it -> it.equals(text)).count();
    }

    public static int summation(int amount) {
        int sum = 0;
        while (amount > 0) {
            sum += amount;
            amount--;
        }
        return sum;
    }
}
