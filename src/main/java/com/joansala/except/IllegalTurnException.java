package com.joansala.except;

/**
 * A turn notation or identifier is not valid.
 */
public class IllegalTurnException extends GameEngineException {
    public IllegalTurnException(String message) {
        super(message);
    }
}
