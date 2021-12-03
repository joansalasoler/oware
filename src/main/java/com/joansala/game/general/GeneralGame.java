package com.joansala.game.general;

/*
 * Aalina engine.
 * Copyright (c) 2021 Joan Sala Soler <contact@joansala.com>
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

import java.util.Arrays;
import java.util.AbstractList;
import java.util.List;
import com.google.inject.Inject;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import com.joansala.engine.Board;
import com.joansala.engine.base.BaseGame;


/**
 * General Game Playing game.
 */
public class GeneralGame extends BaseGame {

    /** Maximum number of plies this object can store */
    public static final int MAX_CAPACITY = Integer.MAX_VALUE;

    /** Capacity increases at least this value each time */
    private static final int CAPACITY_INCREMENT = 128;

    /** A move that moves nothing */
    private static final Move NOOP_MOVE = new Move(GdlPool.getConstant("noop"));

    /** Cursor for an endgame state */
    private static final int ENDGAME_CURSOR = -2;

    /** Maximum obtainable goal score */
    private static final int MAX_GOAL = 100;

    /** State machine for the current game rules */
    private final StateMachine machine;

    /** Player to move first */
    private final Player south = new Player();

    /** Player to move second */
    private final Player north = new Player();

    /** Moves generator */
    private GeneralGenerator movegen;

    /** Start position and turn */
    private GeneralBoard board;

    /** Game states history */
    private MachineState[] states;

    /** Move generation cursors */
    private int[] cursors;

    /** Hash code history */
    private long[] hashes;

    /** Current game state */
    private MachineState state;

    /** Player to move */
    private Player player;

    /** Player to move opponent */
    private Player rival;

    /** If moves were generated for this ply */
    private boolean generated;

    /** Move generation cursor */
    private int cursor;


    /**
     * Instantiate a new game on the start state.
     */
    @Inject
    public GeneralGame(StateMachine machine) {
        this(machine, DEFAULT_CAPACITY);
    }


    /**
     * Instantiate a new game on the start state.
     *
     * @param capacity      Initial capacity
     */
    public GeneralGame(StateMachine machine, int capacity) {
        super(capacity);
        initialize(machine);
        this.machine = machine;
        movegen = new GeneralGenerator(machine);
        states = new MachineState[capacity];
        hashes = new long[capacity];
        cursors = new int[capacity];
        setBoard(new GeneralBoard(machine));
    }


    /**
     * Initialize this game properties.
     */
    private void initialize(StateMachine machine) {
        List<Role> roles = machine.getRoles();

        south.turn = SOUTH;
        north.turn = NORTH;

        if (roles.size() == 1) {
            south.role = roles.get(0);
            north.role = roles.get(0);
            south.action = new MoveAction();
            north.action = new MoveAction();
        } else {
            south.role = roles.get(0);
            north.role = roles.get(1);
            south.action = new MoveAction(SOUTH);
            north.action = new MoveAction(NORTH);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int turn() {
        return player.turn;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Board getBoard() {
        return board;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setBoard(Board board) {
        setBoard((GeneralBoard) board);
    }


    /**
     * {@see #setBoard(Board)}
     */
    public void setBoard(GeneralBoard board) {
        this.index = -1;
        this.board = board;
        this.move = NULL_MOVE;
        this.state = board.position();

        setTurn(board.turn());
        this.generated = false;
        this.cursor = computeCursor();
        this.hash = computeHash();
    }


    /**
     * Sets the current player to move.
     *
     * @param turn      Turn identifier
     */
    private void setTurn(int turn) {
        if (turn == SOUTH) {
            player = south;
            rival = north;
        } else {
            player = north;
            rival = south;
        }
    }


    /**
     * Toggles the player to move.
     */
    private void switchTurn() {
        Player player = this.player;
        this.player = rival;
        this.rival = player;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public GeneralBoard toBoard() {
        return new GeneralBoard(state);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEnded() {
        return cursor == ENDGAME_CURSOR;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int outcome() {
        if (south.role == north.role) {
            final int goal = goal(south);
            if (goal == MAX_GOAL) return MAX_SCORE;
            return goal;
        } else {
            final int sgoal = goal(south);
            final int ngoal = goal(north);
            if (sgoal < ngoal) return -MAX_SCORE;
            if (sgoal > ngoal) return MAX_SCORE;
        }

        return DRAW_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int score() {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getCursor() {
        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void resetCursor() {
        if (cursor != ENDGAME_CURSOR) {
            cursor = 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void makeMove(int move) {
        pushState();
        state = computeState(move);
        cursor = computeCursor();
        hash = computeHash();
        this.generated = false;
        this.move = move;
        switchTurn();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unmakeMove() {
        popState(index);
        switchTurn();
        index--;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unmakeMoves(int length) {
        if (length > 0) {
            index -= length;
            setTurn((length & 1) == 0 ? turn() : -turn());
            popState(1 + index);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int nextMove() {
        if (cursor < 0) {
            return NULL_MOVE;
        }

        generateMoves(1 + index);
        final int move = cursor;
        final int next = 1 + cursor;
        final int length = movegen.getLength(1 + index);
        cursor = (next == length) ? NULL_MOVE : next;

        return move;
    }


    /**
     * Store game state on the history.
     */
    private void pushState() {
        index++;
        states[index] = state;
        moves[index] = move;
        hashes[index] = hash;
        cursors[index] = cursor;
    }


    /**
     * Restore game state from the history.
     */
    private void popState(int index) {
        state = states[index];
        move = moves[index];
        hash = hashes[index];
        cursor = cursors[index];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected long computeHash() {
        return state.hashCode();
    }


    /**
     * Set the cursor according to the game state.
     */
    private int computeCursor() {
        return machine.isTerminal(state) ? ENDGAME_CURSOR : 0;
    }



    /**
     * State resulting from making a move on the current state.
     */
    private MachineState computeState(int move) {
        try {
            generateMoves(index);
            player.action.set(movegen.getMove(index, move));
            return machine.getNextState(state, player.action);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Generates legal moves for the current slot.
     *
     * @param slot      Current ply
     */
    private void generateMoves(int slot) {
        if (generated == false) {
            movegen.generate(slot, state, player.role);
            generated = true;
        }
    }


    /**
     * Current goal score for the given player.
     *
     * @param player    A player
     * @return          Goal score value
     */
    private int goal(Player player) {
        try {
            return machine.getGoal(state, player.role);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureCapacity(int size) {
        if (size > this.capacity) {
            size = Math.max(size, capacity + CAPACITY_INCREMENT);
            size = Math.min(MAX_CAPACITY, size);

            moves = Arrays.copyOf(moves, size);
            cursors = Arrays.copyOf(cursors, size);
            hashes = Arrays.copyOf(hashes, size);
            states = Arrays.copyOf(states, size << 4);
            capacity = size;

            System.gc();
        }
    }


    /**
     * Encapsulates the information of a player.
     */
    private static class Player {
        MoveAction action;
        Role role;
        int turn;
    }


    /**
     * A list of moves by one or two players that move sequentially.
     */
    private final class MoveAction extends AbstractList<Move> {
        private final int size;
        private final int index;
        private Move move;

        /** A move action in a one-player game */
        private MoveAction() {
            this.index = 0;
            this.size = 1;
        }

        /** A move action in a two-player game */
        private MoveAction(int turn) {
            this.index = (turn == SOUTH) ? 0 : 1;
            this.size = 2;
        }


        /** Sets the move to perform */
        public void set(Move move) {
            this.move = move;
        }


        /** {@inheritDoc} */
        @Override public Move get(int index) {
            return index == this.index ?
                move : NOOP_MOVE;
        }


        /** {@inheritDoc} */
        @Override public int size() {
            return size;
        }
    }
}
