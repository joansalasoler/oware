package com.joansala.uci;

import com.joansala.except.GameEngineException;


/**
 * UCI protocol exception.
 */
public class UCIException extends GameEngineException {
    private static final long serialVersionUID = 1L;

    public UCIException(String message) {
        super(message);
    }
}
