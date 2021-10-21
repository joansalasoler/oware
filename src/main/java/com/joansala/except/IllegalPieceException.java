package com.joansala.except;

/**
 * A piece notation or identifier is not valid.
 */
public class IllegalPieceException extends GameEngineException {
    public IllegalPieceException(String message) {
        super(message);
    }
}
