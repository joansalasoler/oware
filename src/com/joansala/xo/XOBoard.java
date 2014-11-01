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

import com.joansala.engine.Board;
import com.joansala.engine.Game;


/**
 * Represents a valid position and turn in a tic-tac-toe game.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class XOBoard implements Board {
    
    
    /** Distribution of the board */
    private int position;
    
    /** Which player moves on the position */
    private int turn;
    
    
    /**
     * Instantiates a new board with the default position and turn
     */
    public XOBoard() {
        this.position = 0;
        this.turn = Game.SOUTH;
    }
    
    
    /**
     * Instantiates a new board with the specified position and turn.
     *
     * @param position  The position of the board
     * @param turn      The player that is to move
     *
     * @throws IllegalArgumentException  if the {@code postion} or
     *      {@code turn} parameters are not valid
     */
    public XOBoard(int position, int turn) {
        if (isValidTurn(turn) == false)
            throw new IllegalArgumentException(
                "Game turn is not a valid");
        
        if (isValidPosition(position) == false)
            throw new IllegalArgumentException(
                "Position representation is not valid");
        
        this.position = position;
        this.turn = turn;
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
    private static boolean isValidTurn(int turn) {
        return turn == Game.SOUTH || turn == Game.NORTH;
    }
    
    
    /**
     * {@inheritDoc}
     */
    private static boolean isValidPosition(int position) {
        return 0 == ((position & 0x1FF) & (position >> 9));
    }
    
    
    /**
     * {@inheritDoc}
     */
    public XOBoard toBoard(Game game) {
        if (!(game instanceof XOGame)) {
            throw new IllegalArgumentException(
                "Not a valid game object");
        }
        
        return new XOBoard((Integer) game.position(), game.turn());
    }
    
    
    /**
     * {@inheritDoc}
     */
    public XOBoard toBoard(String notation) {
        int position = 0;
        int turn = Game.SOUTH;
        
        for (int i = 0; i < notation.length(); i++) {
            char c = notation.charAt(i);
            
            if (c == 'X') {
                position |= (1 << i);
                turn = -turn;
            } else if (c == 'O') {
                position |= (1 << (i + 9));
                turn = -turn;
            }
        }
        
        return new XOBoard(position, turn);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int toMove(String notation) {
        if (notation.length() != 2) {
            throw new IllegalArgumentException(
                "Moves notation is not valid");
        }
        
        char row = notation.charAt(0);
        char col = notation.charAt(1);
        int move = row - 97 + 3 * (col - 49);
        
        if (move < 0 || move > 8) {
            throw new IllegalArgumentException(
                "Moves notation is not valid");
        }
        
        return move;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int[] toMoves(String notation) {
        String[] m = notation.split(" ");
        int[] moves = new int[m.length];
        
        for (int i = 0; i < m.length; i++)
            moves[i] = toMove(m[i]);
        
        return moves;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toAlgebraic(int move) {
        StringBuilder notation = new StringBuilder();
        
        if (move < 0 || move > 8) {
            throw new IllegalArgumentException(
                "Not a valid move representation");
        }
        
        notation.append((char) (97 + move % 3));
        notation.append((char) (49 + move / 3));
        
        return notation.toString();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toAlgebraic(int[] moves) {
        StringBuilder notation = new StringBuilder();
        
        for (int i = 0; i < moves.length; i++) {
            int move = moves[i];
            
            if (move < 0 || move > 8) {
                throw new IllegalArgumentException(
                    "Not a valid move representation");
            }
            
            notation.append((char) (97 + move % 3));
            notation.append((char) (49 + move / 3));
            
            if (1 + i < moves.length)
                notation.append(' ');
        }
        
        return notation.toString();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toNotation() {
        StringBuilder notation = new StringBuilder();
        
        for (int i = 0; i < 9; i++) {
            if (((position >> i) & 0x01) == 1) {
                notation.append('X');
            } else if (((position >> (9 + i)) & 0x01) == 1) {
                notation.append('O');
            } else {
                notation.append('-');
            }
        }
        
        return notation.toString();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String notation = toNotation();
        char[] chars = notation.replace('-', ' ').toCharArray();
        
        return String.format(
            " %c | %c | %c %n" +
            "---+---+---%n" +
            " %c | %c | %c %n" +
            "---+---+---%n" +
            " %c | %c | %c %n%n" +
            "%s move",
            chars[6], chars[7], chars[8],
            chars[3], chars[4], chars[5],
            chars[0], chars[1], chars[2],
            turn == Game.SOUTH ? "Crosses" : "Noughts"
        );
    }


}
