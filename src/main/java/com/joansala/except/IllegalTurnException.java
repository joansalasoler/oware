package com.joansala.except;

/**
 * A turn notation or identifier is not valid.
 */
public class IllegalTurnException extends GameEngineException {
    private static final long serialVersionUID = 1L;

    public IllegalTurnException(String message) {
        super(message);
    }
}
