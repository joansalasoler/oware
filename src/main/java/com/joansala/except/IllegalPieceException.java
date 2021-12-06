package com.joansala.except;

/**
 * A piece notation or identifier is not valid.
 */
public class IllegalPieceException extends GameEngineException {
    private static final long serialVersionUID = 1L;

    public IllegalPieceException(String message) {
        super(message);
    }
}
