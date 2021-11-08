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

import java.util.Set;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.StateMachine;
import com.joansala.engine.base.BaseBoard;
import static com.joansala.game.general.GeneralGame.*;


/**
 * General Game Playing board.
 */
public class GeneralBoard extends BaseBoard<MachineState> {

    /**
     * Creates a new board for the start position.
     */
    @Inject
    public GeneralBoard(StateMachine machine) {
        this(machine.getInitialState());
    }


    /**
     * Creates a new board instance.
     *
     * @param machine       State machine
     * @param position      Position state
     * @param turn          Player to move
     */
    public GeneralBoard(MachineState position) {
        super(clone(position), SOUTH);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MachineState position() {
        return clone(position);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int toMove(String notation) {
        return Integer.parseInt(notation);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toCoordinates(int move) {
        return String.valueOf(move);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public GeneralBoard toBoard(String notation) {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toDiagram() {
        return "undefined";
    }


    /**
     * Clones a machine state.
     */
    private static MachineState clone(MachineState state) {
        return new MachineState(Set.copyOf(state.getContents()));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return position.getContents().stream()
            .map(key -> key.toString()).sorted()
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
