package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {

    StackNode top;
    private int size = 0;

    public StackImpl() {
        this.top = null;
    }

    @Override
    public void push(T element) {
        StackNode prevTop = this.top;
        StackNode newNode=new StackNode(element);
        newNode.setBelow(prevTop);
        this.top=newNode;
        this.size+=1;
    }

    @Override
    public T pop() {
        if(top==null){
            return null;
        }
        T element = this.peek();
        this.top=top.getBelow();
        this.size-=1;
        return element;
    }

    @Override
    public T peek() {
        return top == null ? null : top.getElement();
    }

    @Override
    public int size() {
        return this.size;
    }

    private final class StackNode{
        private final T element;
        private StackNode below;
        StackNode(T element, StackNode below){
            this.below=below;
            this.element=element;
        }
        StackNode(T element) {
           this(element, null);
        }
        private T getElement(){
            return this.element;
        }
        private StackNode getBelow(){
            return this.below;
        }
        private void setBelow(StackNode newNode){
            this.below=newNode;
        }
    }
}
