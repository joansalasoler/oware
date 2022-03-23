package com.joansala.test.engine;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.joansala.engine.Game;


/**
 *
 */
@DisplayName("Game interface contract")
public interface GameContract {

    /**
     * Instantiate a new game object.
     */
    Game newInstance();


    @Test()
    @DisplayName("moves can be iterated in random order")
    default void SameMoveReturnedAfterSetCursor() {
        List<Move> nodes = new LinkedList<>();

        Game game = newInstance();
        int cursor = game.getCursor();
        int cmove = Game.NULL_MOVE;

        while ((cmove = game.nextMove()) != Game.NULL_MOVE) {
            nodes.add(new Move(cmove, cursor));
            cursor = game.getCursor();
        }

        Collections.shuffle(nodes, new Random(1));

        for (Move node : nodes) {
            game.setCursor(node.cursor);
            assertEquals(node.move, game.nextMove());
        }
    }


    /**
     * Encapsulates information about a move.
     */
    public class Move {
        public int move;
        public int cursor;

        public Move(int move, int cursor) {
            this.move = move;
            this.cursor = cursor;
        }
    }
}
