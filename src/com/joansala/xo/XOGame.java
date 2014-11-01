package com.joansala.xo;

/*
 * Copyright (C) 2014 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.joansala.engine.Engine;
import com.joansala.engine.Game;


/**
 * Implements a simple tic-tac-toe game logic using bitboards.
 *
 * <p>The game is represented with two integer values. The first one
 * stores the board cells where a cross or a nought has been placed.
 * This is represented as nine bits, where each bit set to one is a
 * full cell. The other integer value stores the cells where noughts
 * or crosses are placed, in the same way as the previously described
 * representation. Bits zero to nine represent crosses while bits ten
 * two eighteen represent noughts.</p>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class XOGame implements Game {
    
    /** Maximum possible score */
    public static final int MAX_SCORE = 10;
    
    /** Current game turn */
    private int turn;
    
    /** Current board position */
    private int position;
    
    /** Currently full board cells */
    private int board;
    
    /** Current move index */
    private int index;
    
    /** Current move generation state */
    private int state;
    
    /** Stores all the performed moves */
    private int[] moves;
    
    /** Stores the move generation states */
    private int[] states;
    
    /** Placeholder for generated legal moves */
    private int[] legal = new int[9];
    
    /** Legal moves that may be performed */
    private static final int[] VALID_MOVES = {
        4, 0, 2, 6, 8, 1, 3, 5, 7
    };
    
    /** Winning positions masks for noughts */
    private static final int[] NORTH_WINNERS = {
        0x00E00, 0x07000, 0x38000, 0x24800,
        0x12400, 0x09200, 0x0A800, 0x22200
    };
    
    /** Winning positions masks for crosses */
    private static final int[] SOUTH_WINNERS = {
        0x00007, 0x00038, 0x001C0, 0x00124,
        0x00092, 0x00049, 0x00054, 0x00111
    };
    
    
    /**
     * Create a new game in the default start position.
     */
    public XOGame() {
        this.moves = new int[9];
        this.states = new int[9];
        this.turn = Game.SOUTH;
        this.position = 0;
        this.state = 0;
        this.index = -1;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int length() {
        return 1 + index;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int[] moves() {
        if (index == -1)
            return null;
        
        int[] moves = new int[1 + index];
        System.arraycopy(this.moves, 0, moves, 0, 1 + index);
        
        return moves;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int turn() {
        return turn;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Integer position() {
        return position;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void setStart(Object position, int turn) {
        if (turn != Game.SOUTH && turn != Game.NORTH)
            throw new IllegalArgumentException(
                "Game turn is not a valid");
        
        int newPosition = (int) position;
        
        if ((newPosition & newPosition >> 9) != 0)
            throw new IllegalArgumentException(
                "Position representation is not valid");
        
        this.position = newPosition;
        this.board = (newPosition | newPosition >> 9);
        this.turn = turn;
        this.index = -1;
        this.state = 0;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void endMatch() {
        // Does nothing
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean hasEnded() {
        if ((board & 0x1FF) == 0x1FF)
            return true;
        
        return winner() != Game.DRAW;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int winner() {
        if (turn == Game.SOUTH) {
            for (int mask : NORTH_WINNERS) {
                if ((position & mask) == mask)
                    return Game.NORTH;
            }
        } else {
            for (int mask : SOUTH_WINNERS) {
                if ((position & mask) == mask)
                    return Game.SOUTH;
            }
        }
        
        return Game.DRAW;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int score() {
        return Game.DRAW_SCORE;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int outcome() {
        int winner = winner();
        
        if (winner == Game.SOUTH) {
            return MAX_SCORE;
        } else if (winner == Game.NORTH) {
            return -MAX_SCORE;
        }
        
        return Game.DRAW_SCORE;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public long hash() {
        return (long) position;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isLegal(int move) {
        return (move >= 0 && move < 9) &&
               ((board & (0x001 << move)) == 0);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void makeMove(int move) {
        position = (turn == Game.SOUTH) ?
            position | (0x001 << move) :
            position | (0x200 << move);
        board = (position | position >> 9);
        moves[++index] = move;
        states[index] = state;
        turn = -turn;
        state = 0;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void unmakeMove() {
        int move = moves[index];
        
        position = (turn == Game.NORTH) ?
            position & ~(0x001 << move) :
            position & ~(0x200 << move);
        board = (position | position >> 9);
        state = states[index];
        turn = -turn;
        index--;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int nextMove() {
        for (int i = state; i < 9; i++) {
            int move = VALID_MOVES[i];
            state++;
            
            if ((board & (0x001 << move)) == 0)
                return move;
        }
        
        return Game.NULL_MOVE;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int[] legalMoves() {
        int[] moves = null;
        int length = 0;
        
        for (int i = 0; i < 9; i++) {
            int move = VALID_MOVES[i];
            if ((board & (0x001 << move)) == 0)
                legal[length++] = move;
        }
        
        if (length > 0) {
            moves = new int[length];
            System.arraycopy(legal, 0, moves, 0, length);
        }
        
        return moves;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void ensureCapacity(int capacity) {
        // Does nothing
    }
    
    
}
