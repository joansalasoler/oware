package com.joansala.except;

/**
 * A position notation or encoding is not valid.
 */
public class IllegalPositionException extends GameEngineException {
    private static final long serialVersionUID = 1L;

    public IllegalPositionException(String message) {
        super(message);
    }
}
