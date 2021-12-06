package com.joansala.game.chess;

import org.junit.jupiter.api.*;
import com.joansala.engine.Game;
import com.joansala.engine.GameContract;
import com.joansala.game.chess.ChessGame;


@DisplayName("Chess game")
public class ChessGameTest implements GameContract {

    /**
     * {@inheritDoc}
     */
    @Override
    public Game newInstance() {
        return new ChessGame();
    }
}
