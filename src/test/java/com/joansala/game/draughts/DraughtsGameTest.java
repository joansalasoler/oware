package com.joansala.game.draughts;

import org.junit.jupiter.api.*;
import com.joansala.engine.Game;
import com.joansala.engine.GameContract;
import com.joansala.game.draughts.DraughtsGame;


@DisplayName("Draughts game")
public class DraughtsGameTest implements GameContract {

    /**
     * {@inheritDoc}
     */
    @Override
    public Game newInstance() {
        return new DraughtsGame();
    }
}
