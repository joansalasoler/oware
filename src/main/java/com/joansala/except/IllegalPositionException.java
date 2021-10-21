package com.joansala.except;

/**
 * A position notation or encoding is not valid.
 */
public class IllegalPositionException extends GameEngineException {
    public IllegalPositionException(String message) {
        super(message);
    }
}
