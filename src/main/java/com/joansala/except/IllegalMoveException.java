package com.joansala.except;

/**
 * A move notation or identifier is not valid.
 */
public class IllegalMoveException extends GameEngineException {
    public IllegalMoveException(String message) {
        super(message);
    }
}
