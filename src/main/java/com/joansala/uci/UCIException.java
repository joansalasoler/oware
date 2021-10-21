package com.joansala.uci;

import com.joansala.except.GameEngineException;


/**
 * UCI protocol exception.
 */
public class UCIException extends GameEngineException {
    public UCIException(String message) {
        super(message);
    }
}
