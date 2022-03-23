package com.joansala.test.game.oware;

import org.junit.jupiter.api.*;
import com.joansala.engine.Game;
import com.joansala.test.engine.GameContract;
import com.joansala.game.oware.OwareGame;


@DisplayName("Oware game")
public class OwareGameTest implements GameContract {

    /**
     * {@inheritDoc}
     */
    @Override
    public Game newInstance() {
        return new OwareGame();
    }
}
