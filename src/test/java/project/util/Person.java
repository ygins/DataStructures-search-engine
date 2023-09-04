package project.util;

import java.util.Random;

public record Person(String name, int age) {

    @Override
    public int hashCode() {
        return age;
    }


    public static class Generator {
        private final Random random;
        private final String[] NAMES = {"Reuven", "Shimon", "Levi", "Yehuda", "Yissachar", "Zevulun", "Dan", "Naftali", "Gad", "Asher", "Yosef", "Binyamin", "Avraham", "Yitzchak", "Yaakov", "Moshe", "Aaron", "David", "Sarah", "Rivka", "Rachel", "Leah"};
        private final String[] ADJECTIVES = {"Cheerful", "Morose", "Upbeat", "Down-to-earth", "Serious", "Optimistic", "Nihilistic", "Angry", "Intelligent", "Sees the glass half-full"};

        public Generator(Random random) {
            this.random = random;
        }

        public Generator() {
            this(new Random());
        }

        public Person randomPerson(int age) {
            return new Person(randomElementFrom(NAMES, this.random), age);
        }

        public String randomName() {
            return randomElementFrom(NAMES, this.random);
        }

        public String randomAdjective() {
            return randomElementFrom(ADJECTIVES, this.random);
        }

        private static <T> T randomElementFrom(T[] arr, Random random) {
            return arr[random.nextInt(arr.length)];
        }

    }
}
