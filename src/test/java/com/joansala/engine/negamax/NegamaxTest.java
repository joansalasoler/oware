package com.joansala.engine.negamax;

import org.junit.jupiter.api.*;
import com.joansala.engine.Engine;
import com.joansala.engine.EngineContract;


@DisplayName("Negamax engine")
public class NegamaxTest implements EngineContract {

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine newInstance() {
        return new Negamax();
    }
}
