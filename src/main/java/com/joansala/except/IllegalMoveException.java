package com.joansala.except;

/**
 * A move notation or identifier is not valid.
 */
public class IllegalMoveException extends GameEngineException {
    private static final long serialVersionUID = 1L;

    public IllegalMoveException(String message) {
        super(message);
    }
}
