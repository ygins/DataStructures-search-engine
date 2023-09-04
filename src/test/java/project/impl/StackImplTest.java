package project.impl;

import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.impl.StackImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import project.util.Person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StackImplTest {
    private Stack<Person> people;

    @BeforeEach
    void setup() {
        this.people = new StackImpl<>();
    }

    //Begin testing push/pop
    @Test
    void popReturnsInPrevOrder() {
        testPushPopAmount(50);
    }

    @Test
    void stressTestPushPop() {
        for (int i = 0; i < 4; i++) {
            testPushPopAmount((int) Math.pow(10, i));
        }
    }

    @Test
    void pushPopInsertionDeletion() {//Not just testing one insert - what if we push,pop,then push again?
        for (int e = 0; e < 10; e++) {
            //push x amount, remove y<x amount, push z amount.
            //result = x-y amount + z
            Random r = new Random();
            var xPeople = this.populateStack(r.nextInt(50, 500));
            var amountToRemove = r.nextInt(r.nextInt(10, xPeople.size()));
            for (int i = 0; i < amountToRemove; i++) {
                this.people.pop();
            }
            var zPeople = this.populateStack(r.nextInt(50, 500));
            //Now to get our expected output. First zPeople (reversed) then xPeople with last bunch popped off
            Collections.reverse(zPeople);
            Collections.reverse(xPeople);
            List<Person> expected = new ArrayList<>(zPeople);
            for (int i = amountToRemove; i < xPeople.size(); i++) {
                expected.add(xPeople.get(i));
            }
            assertEquals(expected.size(), this.people.size());
            assertEqual(expected, this.people);
            assertEquals(0, this.people.size());
        }
    }

    @Test
    void popReturnsNullInitially() {
        assertNull(this.people.pop());
    }

    @Test
    void popReturnsNullIfEmptied() {
        this.populateStack(100);
        for (int i = 0; i < 100; i++) {
            this.people.pop();
        }
        assertNull(this.people.pop());
    }

    //test peek
    @Test
    void peekDoesNotAlterStack() {
        var people = this.populateStack(50);
        for (int i = 0; i < 50; i++) {
            this.people.peek();
        }
        Collections.reverse(people);
        assertEqual(people, this.people);
    }

    @Test
    void peekReturnsNextValue() {
        var people = this.populateStack(10000);
        var peekedPeople = new ArrayList<Person>();
        var helperStack = new StackImpl<Person>();
        for (int i = 0; i < people.size(); i++) {
            peekedPeople.add(this.people.peek());
            helperStack.push(this.people.pop());
        }
        Collections.reverse(peekedPeople);
        assertEqual(peekedPeople, helperStack);
    }

    @Test
    void peekReturnsNullInitially() {
        assertNull(this.people.peek());
    }

    @Test
    void peekReturnsNullWhenEmptied() {
        this.populateStack(1000);
        for (int i = 0; i < 1000; i++) {
            this.people.pop();
        }
        assertNull(this.people.peek());
    }

    //test size
    @Test
    void sizeIsZeroInitially() {
        assertEquals(0, this.people.size());
    }

    @Test
    void sizeIsZeroWhenEmptied() {
        this.populateStack(1000);
        for (int i = 0; i < 1000; i++) {
            this.people.pop();
        }
        assertEquals(0, this.people.size());
    }

    @Test
    void sizeGrowsWithStack() {
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            this.people = new StackImpl<>();
            int amount = r.nextInt(1, 1000);
            this.populateStack(amount);
            assertEquals(amount, this.people.size());
        }
    }

    private void testPushPopAmount(int amount) {
        var people = populateStack(amount);
        Collections.reverse(people);
        assertEqual(people, this.people);
    }


    private List<Person> populateStack(int amount) {
        Person.Generator generator = new Person.Generator();
        var peopleList = new ArrayList<Person>();
        for (int i = 0; i < amount; i++) {
            Person p = generator.randomPerson(i);
            peopleList.add(p);
            this.people.push(p);
        }
        return peopleList;
    }

    private void assertEqual(List<Person> expected, Stack<Person> actual) {
        for (Person person : expected) {
            assertEquals(person, actual.pop());
        }
    }
}
