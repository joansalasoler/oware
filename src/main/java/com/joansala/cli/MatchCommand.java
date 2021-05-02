package com.joansala.cli;

/*
 * Aalina oware engine.
 * Copyright (C) 2021 Joan Sala Soler <contact@joansala.com>
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

import java.util.concurrent.Callable;
import com.google.inject.Inject;
import picocli.CommandLine.*;

import com.joansala.engine.Engine;
import com.joansala.uci.UCIClient;
import com.joansala.uci.UCIMatch;

/**
 * Executes the user interface to play against an engine.
 */
@Command(
  name = "match",
  version = "1.2.1",
  description = "Play a match against the engine",
  mixinStandardHelpOptions = true
)
public final class MatchCommand implements Callable<Integer> {

    /** Injected UCI match instance */
    private UCIMatch match;

    /** UCI client instance */
    private UCIClient client;

    @Option(
      names = "--depth",
      description = "Depth limit per move (plies)"
    )
    private int depth = Engine.DEFAULT_DEPTH;

    @Option(
      names = "--movetime",
      description = "Time limit per move (ms)"
    )
    private long moveTime = Engine.DEFAULT_MOVETIME;

    @Option(
      names = "--command",
      description = "Custom UCI engine command",
      converter = ProcessConverter.class,
      defaultValue = "<default>"
    )
    private Process service = null;


    /**
     * Creates a new service.
     */
    @Inject public MatchCommand(UCIMatch match) {
        this.client = match.getUCIClient();
        this.match = match;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        client.setService(service);
        match.setMoveTime(moveTime);
        match.setDepth(depth);
        match.start();

        return 0;
    }
}
