package com.joansala.engine.uct;

import org.junit.jupiter.api.*;
import com.joansala.engine.Engine;
import com.joansala.engine.EngineContract;


@DisplayName("UCT engine")
public class UCTTest implements EngineContract {

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine newInstance() {
        return new UCT();
    }
}
