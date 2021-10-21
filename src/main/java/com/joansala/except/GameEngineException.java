package com.joansala.except;

/**
 * Superclass of all the game related runtime exceptions.
 */
public class GameEngineException extends RuntimeException {
    public GameEngineException(String message) {
        super(message);
    }
}
