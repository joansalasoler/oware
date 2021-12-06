package com.joansala.game.othello;

import org.junit.jupiter.api.*;
import com.joansala.engine.Game;
import com.joansala.engine.GameContract;
import com.joansala.game.othello.OthelloGame;


@DisplayName("Othello game")
public class OthelloGameTest implements GameContract {

    /**
     * {@inheritDoc}
     */
    @Override
    public Game newInstance() {
        return new OthelloGame();
    }
}
