package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.util.NoSuchElementException;

public class MinHeapImpl<T extends Comparable<T>> extends MinHeap<T> {

    @SuppressWarnings("unchecked")
    public MinHeapImpl(){
        super.elements= (T[]) new Comparable[3];
    }
    @Override
    public void reHeapify(T element) {
        int elementIndex = this.getArrayIndex(element);
        if(elementIndex==-1){
            throw new NoSuchElementException("Element not in heap!");
        }
        /*
        I could just call upHeap() and downHeap() because they check for all these things
        anyways, but that's very unclear.
         */
        boolean shouldBeGoingUp=elementIndex!=1 && isGreater(elementIndex/2, elementIndex);
        if(shouldBeGoingUp){
            super.upHeap(elementIndex);
        }else{
            super.downHeap(elementIndex);//wont move if not necessary, no worries :)
        }
    }

    @Override
    protected int getArrayIndex(T element) {
        /*
         * A heap doesn't really have ordering with regards to its children, and in specific
         * the heap in our scenario can have elements change on the fly. Thus I have no guarantees
         * for any sort of search optimization - best ot just search through the array O(n)
         */
        for(int i=1; i<=super.count; i++){
            assert elements[i] != null; //shouldnt be possible because complete tree
            if(element.compareTo(elements[i])==0){
                return i;
            }
        }
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doubleArraySize() {
        T[] newArray = (T[])new Comparable[super.elements.length*2];
        System.arraycopy(super.elements, 0, newArray, 0, super.elements.length);
        super.elements=newArray;
    }
}
