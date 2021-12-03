package com.joansala.engine;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.joansala.mock.*;


/**
 *
 */
@DisplayName("Engine interface contract")
public interface EngineContract {

    /**
     * Instantiate a new engine object.
     */
    Engine newInstance();


    @Test()
    @DisplayName("game state is the same after search")
    default void ComputeBestMoveKeepsGameState() {
        Engine engine = newInstance();
        MockGame game = new MockGame();

        for (int move : new int[] { 0, 1, 3,  2,  0, 1, 4, 1, 1 }) {
            game.makeMove(move);
        }

        int[] moves = game.moves();
        engine.computeBestMove(game);
        assertArrayEquals(moves, game.moves());
    }
}
