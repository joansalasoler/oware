package com.joansala.tools;

/*
 * Copyright (C) 2014 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;


/**
 * Implements a double-stack of a fixed size.
 *
 * <p>This stack is implemented with two arrays which contain links to
 * next and previous elements. Each element is identified by an unique
 * hash code that must be a value between zero and the size of the
 * stack. All operations are performed in O(1).</p>
 *
 * <p>A double-stack contains two piles of stacked values, the main stack
 * and a backtracking stack. Note that the stack is only warranted to work
 * correctly when the main stack and the backtracking stack contain
 * disjoint sets.</p>
 *
 */
public class DStack {
    
    /** Null hash code */
    private final int NULL_NODE = -1;
    
    /** Reserved node identifier */
    private final int ANY_NODE = -2;
    
    /** Links to previous nodes */
    private final int[] prev;
    
    /** Links to next nodes */
    private final int[] next;
    
    /** Pointer to the head of the main stack */
    private int head = NULL_NODE;
    
    /** Pointer to the head of the backtracking stack */
    private int back = NULL_NODE;
    
    
    /**
     * Instantiates a new double-stack object
     *
     * @param size  size of the stack
     */
    public DStack(int size) {
        prev = new int[size];
        next = new int[size];
        Arrays.fill(prev, NULL_NODE);
        Arrays.fill(next, NULL_NODE);
    }
    
    
    /**
     * Adds a node to the top of the main stack.
     *
     * @param node  unique identifier
     */
    public void push(int node) {
        if (head != NULL_NODE)
            next[head] = node;
        prev[node] = head;
        next[node] = ANY_NODE;
        head = node;
    }
    
    
    /**
     * Promotes an already stacked node to the top of the main stack.
     *
     * @param node  unique identifier
     */
    public void raise(int node) {
        int n = next[node];
        int p = prev[node];
        
        prev[n] = p;
        next[p] = n;
        
        next[head] = node;
        prev[node] = head;
        next[node] = ANY_NODE;
        head = node;
    }
    
    
    /**
     * Removes a node from the top of the main stack.
     */
    public void pop() {
        next[head] = NULL_NODE;
        head = prev[head];
    }
    
    
    /**
     * Returns the unique identifier of the node currently on the
     * top of the main stack.
     *
     * @return  node identifier
     */
    public int peek() {
        return head;
    }
    
    
    /**
     * Checks if the main stack contains any nodes.
     *
     * @return  {@code true} if the main stack is empty
     */
    public boolean empty() {
        return head == NULL_NODE;
    }
    
    
    /**
     * Adds a node to the top of the backtracking stack.
     *
     * @param node  unique identifier
     */
    public void pushBack(int node) {
        prev[node] = back;
        back = node;
    }
    
    
    /**
     * Removes a node from the top of the backtracking stack.
     */
    public void popBack() {
        back = prev[back];
    }
    
    
    /**
     * Returns the unique identifier of the node currently on the
     * top of the backtracking stack.
     *
     * @return  node identifier
     */
    public int peekBack() {
        return back;
    }
    
    
    /**
     * Checks if the backtracking stack contains any nodes.
     *
     * @return  {@code true} if the main stack is empty
     */
    public boolean emptyBack() {
        return back == NULL_NODE;
    }
    
    
    /**
     * Returns {@code true} if either the main stack or the backtracking
     * stack contain the specified node.
     *
     * @param node  unique identifier
     * @return      {@code true} if the node is stacked
     */
    public boolean contains(int node) {
        return next[node] != NULL_NODE;
    }
    
}

