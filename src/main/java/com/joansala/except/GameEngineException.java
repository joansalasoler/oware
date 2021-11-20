package com.joansala.except;

/**
 * Superclass of all the game related runtime exceptions.
 */
public class GameEngineException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GameEngineException(String message) {
        super(message);
    }
}
