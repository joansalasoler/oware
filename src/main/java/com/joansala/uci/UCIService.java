package com.joansala.uci;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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

import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.function.Consumer;
import com.google.inject.Inject;

import com.joansala.engine.*;


/**
 * Implements a communication protocol service for game engines.
 * The protocol implemented resembles that of the Universal Chess
 * Interface protocol with only minimal adaptations. Therefore, this
 * class provides a service that reads UCI commands from the standard
 * input and writes replies to the standard output.</p>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class UCIService {

    /** Thread where the computations are performed */
    private Brain brain = null;

    /** Performs move computations for a game */
    private Engine engine = null;

    /** Game where the computations are performed */
    private Game game = null;

    /** Contains the current start position and turn */
    private Board board = null;

    /** Contains the initial board of the game */
    private Board rootBoard = null;

    /** Opening book */
    private Roots<Game> roots = null;

    /** Engine's transposition table */
    private Cache<Game> cache = null;

    /** Engine's endgames database */
    private Leaves<Game> leaves = null;

    /** Last info shown for the current computation */
    private String lastInfo = null;

    /** Contains the performed moves for the next computation */
    private int[] moves = null;

    /** Minimum capacity in MB for the hash table */
    private int minHashSize = 1;

    /** Maximum capacity in MB for the hash table */
    private int maxHashSize = 1;

    /** Current capacity in MB for the hash table */
    private int currentHashSize = minHashSize;

    /** Requested hash table capacity for subsequent computations */
    private int requestedHashSize = minHashSize;

    /** Contempt factor for the engine */
    private int contempt = Game.DRAW_SCORE;

    /** Infinity score for the engine */
    private int infinity = Integer.MAX_VALUE;

    /** If debug mode is enabled the engine sends additional infos */
    private boolean debug = false;

    /** If set to true the engine will have a high preference for draws */
    private volatile boolean drawSearch = false;

    /** If set to true the engine will use its own book */
    private volatile boolean ownBook = true;

    /** If set the engine will use its endgames database */
    private volatile boolean useLeaves = true;

    /** If set to true the time for next computation will be infinite */
    private volatile boolean infinite = false;


    /**
     * Instantiates a new UCI service for the given game. The provided
     * engine will be used to perform move computations. The provided
     * board is the initial position for the game.
     *
     * @param game      A game object
     * @param engine    An engine object
     */
    @Inject public UCIService(Game game, Engine engine) {
        this.board = null;
        this.game = game;
        this.engine = engine;
        this.rootBoard = game.rootBoard();

        if (engine instanceof HasCache) {
            cache = ((HasCache) engine).getCache();
        }

        if (engine instanceof HasLeaves) {
            leaves = ((HasLeaves) engine).getLeaves();
        }
    }


    /**
     * Sets the contempt factor of the engine.
     *
     * @param score     Score for drawn positions
     */
    public synchronized void setContempt(int score) {
        this.contempt = score;
    }


    /**
     * Sets the infinity score of the engine.
     *
     * @param score     Maximum possible score
     */
    public synchronized void setInfinity(int score) {
        this.infinity = score;
    }


    /**
     * Sets the openings book database to use.
     *
     * @param roots     A roots object or {@code null} to disable
     *                  the use of an opening book
     */
    @Inject(optional=true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized void setRoots(Roots roots) {
        this.roots = roots;
    }


    /**
     * Asks this service to start receiving commands. Commands are parsed
     * from the standard input until a quit command is received. Replies
     * are written to the standard output.
     */
    public synchronized void start() {
        Scanner scanner = new Scanner(System.in);

        // Brain initialization

        this.brain = new Brain();
        this.brain.start();

        // Remember current hash size and available memory

        if (cache != null) {
            long freeMemory = Runtime.getRuntime().freeMemory();
            long currentMemory = cache.size();

            this.maxHashSize = (int) ((currentMemory + freeMemory) >> 20);
            this.currentHashSize = (int) (currentMemory >> 20);
            this.requestedHashSize = currentHashSize;
        }

        // Start parsing received commands

        while (scanner.hasNext()) {
            String command = scanner.next();
            String params = scanner.nextLine();

            // Show the received command in debug mode

            if (debug == true) {
                printString("Command: " + command);

                if (!params.equals(""))
                    printString("Options:" + params);
            }

            // Perform the requested command

            if ("debug".equals(command)) {
                switchDebugMode(params);
            } else if ("go".equals(command)) {
                startThinking(params);
            } else if ("isready".equals(command)) {
                answerPing();
            } else if ("ponderhit".equals(command)) {
                stopPondering();
            } else if ("position".equals(command)) {
                setPosition(params);
            } else if ("quit".equals(command)) {
                quitEngine();
                break;
            } else if ("setoption".equals(command)) {
                setOption(params);
            } else if ("stop".equals(command)) {
                stopThinking();
            } else if ("uci".equals(command)) {
                identifyEngine();
            } else if ("ucinewgame".equals(command)) {
                startNewGame();
            } else {
                showError("Unknown command");
            }
        }

        this.brain.interrupt();
        scanner.close();
    }


    /**
     * The search thread where the computations are performed.
     */
    private class Brain extends Thread {

        private volatile boolean thinking = false;


        /**
         * The main bucle for the brain.
         */
        @Override public void run() {
            final Consumer<Report> consumer = createSearchConsumer();
            engine.attachConsumer(consumer);

            while (true) {
                synchronized (this) {
                    try {
                        lastInfo = null;
                        thinking = false;
                        this.wait();
                        findBestMove();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            engine.detachConsumer(consumer);
        }


        /**
         * Returns if the brain is currently computing a move.
         *
         * @return {@code true} if there is a move computation in progress
         */
        public boolean isThinking() {
            return thinking;
        }


        /**
         * Instructs the brain to start a new move calculation
         */
        public void startThinking() {
            synchronized (this) {
                thinking = true;
                this.notify();
            }
        }


        /**
         * Instructs the brain to abort any move calculations
         */
        public void stopThinking() {
            while (isThinking()) {
                engine.abortComputation();
                synchBrain();
            }
        }


        /**
         * Performs all the necessary computations to find a best move
         * for the current game.
         */
        private void findBestMove() {
            int bestMove = Game.NULL_MOVE;

            // If the game has ended return a null move

            if (game.hasEnded()) {
                output("bestmove 0000");
                return;
            }

            // Use the book to find a move

            if (ownBook && !infinite) {
                bestMove = getBookMove(game);
            }

            // Enable or disable the endgames database

            if (engine instanceof HasLeaves) {
                HasLeaves e = (HasLeaves) engine;
                e.setLeaves(useLeaves ? leaves : null);
            }

            // Use the engine to compute a move

            if (bestMove == Game.NULL_MOVE) {
                bestMove = engine.computeBestMove(game);
            } else {
                output("info string A book move was chosen");
            }

            // Show the computed best and ponder moves

            StringBuilder response = new StringBuilder();

            response.append("bestmove ");
            response.append(rootBoard.toAlgebraic(bestMove));

            performMove(game, bestMove);
            int ponderMove = getPonderMove(game);

            if (ponderMove != Game.NULL_MOVE) {
                response.append(" ponder ");
                response.append(rootBoard.toAlgebraic(ponderMove));
            }

            output(response.toString());
        }


        /**
         * Prints information for an engine's search.
         *
         * @param report    Search report
         * @return          Information string
         */
        private String formatReport(Report report) {
            StringBuilder response = new StringBuilder();

            int flag = report.getFlag();
            int score = report.getScore();
            int depth = report.getDepth();
            int[] variation = report.getVariation();

            response.append("info");

            if (depth > 0) {
                response.append(" depth ");
                response.append(depth);
            }

            response.append(" score cp ");
            response.append(score);

            if (flag == Flag.LOWER) {
                response.append(" lowerbound");
            }

            if (flag == Flag.UPPER) {
                response.append(" upperbound");
            }

            if (variation.length > 0) {
                response.append(" pv ");
                response.append(rootBoard.toAlgebraic(variation));
            }

            return response.toString();
        }


        /**
         * A consumer that prints search information for the current state.
         *
         * @return      New search consumer instance
         */
        private Consumer<Report> createSearchConsumer() {
            return (report) -> {
                if (report.getFlag() != Flag.EMPTY) {
                    String info = formatReport(report);

                    if (!info.equals(lastInfo)) {
                        lastInfo = info;
                        output(info);
                    }
                }
            };
        }


        /**
         * Returns a move from the openings book.
         *
         * @param game  A game object
         * @return      A move or {@code Game.NULL_MOVE}
         */
        private int getBookMove(Game game) {
            int move = Game.NULL_MOVE;

            if (roots instanceof Roots == false) {
                return move;
            }

            try {
                move = roots.pickBestMove(game);
            } catch (Exception e) {
                showError("Cannot select book move");
                showError(e.getMessage());
            }

            return move;
        }


        /**
         * Returns a move for pondering.
         *
         * @param game  A game object
         * @return      A move or {@code Game.NULL_MOVE}
         */
        private int getPonderMove(Game game) {
            int move = engine.getPonderMove(game);

            if (move == Game.NULL_MOVE && ownBook) {
                move = getBookMove(game);
            }

            return move;
        }
    }


    /**
     * Performs a move on the internal board. This method asserts that
     * the move is legal and ensures the game has enough capacity to
     * store it.
     *
     * @param game  Game object where a move must be performed
     * @param move  Move to perform on the game
     * @throws IllegalArgumentException  if the move cannot be
     *      performed on the provided game object
     */
    private void performMove(Game game, int move) {
        if (game.isLegal(move)) {
            game.ensureCapacity(1 + game.length());
            game.makeMove(move);
        } else {
            throw new IllegalArgumentException(
                "The provided move is not legal");
        }
    }


    /**
     * Waits for the brain to be ready for at most one second. The
     * brain is ready when it's thinking state is false.
     */
    private void synchBrain() {
        for (int i = 0; brain.isThinking() && i < 50; i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                break;
            }
        }
    }


    /**
     * Prints the engine name and author on the standard output
     */
    private void identifyEngine() {
        Package pack = game.getClass().getPackage();
        String title = pack.getImplementationTitle();
        String version = pack.getImplementationVersion();
        String author = pack.getImplementationVendor();

        // Show version information and author

        output("id name " + (title == null ? "Unknown Engine" : title) +
            " " + (version == null ? "" : version));
        output("id author " + (author == null ? "Unknown Author" : author));

        // Hash table size is set based on the available memory

        if (cache != null) {
            output("option name Hash type spin default " + currentHashSize +
                " min " + minHashSize + " max " + maxHashSize);
        }

        if (leaves != null) {
            output("option name UseLeaves type check default true");
        }

        // Show own book option only when the book is present

        if (roots != null) {
            output("option name OwnBook type check default true");
        }

        // Ponder mode is enabled by default

        output("option name Ponder type check default true");

        // Sets the engine to prefer draw scores

        output("option name DrawSearch type check default false");

        // The engine is ready

        output("uciok");
    }


    /**
     * Prints 'readyok' on the standard output
     */
    private void answerPing() {
        output("readyok");
    }


    /**
     * Switch the debug mode on and off
     */
    private void switchDebugMode(String params) {
        Scanner scanner = new Scanner(params);

        if (!scanner.hasNext())
            debug = !debug;

        while (scanner.hasNext()) {
            String token = scanner.next();

            if (token.equals("on")) {
                debug = true;
            } else if (token.equals("off")) {
                debug = false;
            }
        }

        scanner.close();
    }


    /**
     * Tells the engine to start a new game
     */
    private void startNewGame() {
        synchBrain();

        if (brain.isThinking()) {
            showError("A calculation is in progress");
            return;
        }

        if (roots != null)
            roots.newMatch();

        engine.newMatch();
    }


    /**
     * Stops the search thread
     */
    private void quitEngine() {
        brain.stopThinking();
        brain.interrupt();
    }


    /**
     * Parses the 'go' command parameters setting the relevant engine
     * propieties and starts a new move computation.
     *
     * @param params  A string containing options for the command
     */
    private void startThinking(String params) {
        synchBrain();

        if (brain.isThinking()) {
            showError("Already calculating a move");
            return;
        }

        // Set default parameters

        int contempt = this.contempt;
        int depth = Integer.MAX_VALUE;
        long movetime = Engine.DEFAULT_MOVETIME;

        infinite = false;

        // Obtain search params

        Scanner scanner = new Scanner(params);

        while (scanner.hasNext()) {
            String token = scanner.next();

            if (token.equals("depth")) {
                if (scanner.hasNextByte())
                    depth = scanner.nextByte();
            } else if (token.equals("infinite")) {
                infinite = true;
            } else if (token.equals("movetime")) {
                if (scanner.hasNextLong())
                    movetime = scanner.nextLong();
            } else if (token.equals("ponder")) {
                infinite = true;
            }
        }

        scanner.close();

        // Ensure parameters for infinite search

        if (infinite) {
            depth = Integer.MAX_VALUE;
            movetime = 60 * 60 * 1000;
        }

        // Set the board position

        game.setStart(board == null ? rootBoard : board);

        // Perform the moves on the board

        try {
            if (moves != null) {
                for (int move : moves)
                    performMove(game, move);
            }
        } catch (Exception e) {
            showError(e.getMessage());
            return;
        }

        // Set the comptempt factor

        infinity = game.infinity();
        contempt = game.contempt();

        if (drawSearch == true) {
            contempt = infinity;
        }

        // Set engine parameters

        engine.setDepth(depth - 1);
        engine.setMoveTime(movetime);
        engine.setContempt(contempt);
        engine.setInfinity(infinity);

        // Resize the hash table if requested

        if (cache != null) {
            if (currentHashSize != requestedHashSize) {
                cache.resize(requestedHashSize << 20);
                currentHashSize = requestedHashSize;
            }
        }

        // Start calculating a move

        brain.startThinking();
    }


    /**
     * Parses the 'stop' command parameters setting the relevant engine
     * propieties.
     */
    private void stopThinking() {
        brain.stopThinking();
    }


    /**
     * Parses the 'ponderhit' command parameters setting the relevant
     * engine propieties.
     */
    private void stopPondering() {}


    /**
     * Parses the 'setoption' command parameters setting the relevant engine
     * propieties.
     *
     * @param params  A string containing options for the command
     */
    private void setOption(String params) {
        String name = null;
        String value = null;

        // Obtain option name and value

        Scanner scanner = new Scanner(params);
        Pattern stop = Pattern.compile("name|value");

        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.equals("name")) {
                name = consumeString(scanner, stop);
            } else if (token.equals("value")) {
                value = consumeString(scanner, stop);
            }
        }

        scanner.close();

        // No option name specified

        if (name == null) {
            showError("No option name specified");
            return;
        }

        // Set use own openings book parameter

        if (name.equals("OwnBook")) {
            if (value.equals("true")) {
                ownBook = true;
            } else if (value.equals("false")) {
                ownBook = false;
            } else {
                showError("Invalid value for option OwnBook");
            }

            return;
        }

        // Use endgame tablebases

        if (name.equals("UseLeaves")) {
            if (value.equals("true")) {
                useLeaves = true;
            } else if (value.equals("false")) {
                useLeaves = false;
            } else {
                showError("Invalid value for option UseLeaves");
            }

            return;
        }

        // Set draw search parameter

        if (name.equals("DrawSearch")) {
            if (value.equals("true")) {
                drawSearch = true;
            } else if (value.equals("false")) {
                drawSearch = false;
            } else {
                showError("Invalid value for option DrawSearch");
            }

            return;
        }

        // Set hash size parameter

        if (name.equals("Hash") && cache != null) {
            try {
                int size = Integer.parseInt(value);

                if (size >= minHashSize && size <= maxHashSize) {
                    requestedHashSize = size;
                } else {
                    showError("Hash size out of range");
                }
            } catch (NumberFormatException e) {
                showError("Hash size is not a number");
            }

            return;
        }

        // No valid option requested

        showError("Cannot set requested option");
    }


    /**
     * Parses the 'position' command parameters setting the relevant engine
     * propieties.
     *
     * @param params  A string containing options for the command
     */
    private void setPosition(String params) {
        String boardNotation = null;
        String movesNotation = null;

        // Obtain the position command parameters

        Scanner scanner = new Scanner(params);
        Pattern stop = Pattern.compile("startpos|fen|moves");

        while (scanner.hasNext()) {
            String token = scanner.next();

            if (token.equals("fen")) {
                if (scanner.hasNext())
                    boardNotation = scanner.next();
            } else if (token.equals("moves")) {
                movesNotation = consumeString(scanner, stop);
            }
        }

        scanner.close();

        // Set default values

        board = null;
        moves = null;

        // Set the new board if we received a notation for it

        if (boardNotation != null) {
            try {
                board = rootBoard.toBoard(boardNotation);
            } catch (Exception e) {
                showError(e.getMessage());
                return;
            }
        }

        // Set the performed moves if we received a notation for them

        if (movesNotation != null) {
            try {
                moves = rootBoard.toMoves(movesNotation);
            } catch (Exception e) {
                showError(e.getMessage());
                return;
            }
        }
    }


    /**
     * Prints an error string if debug mode is enabled.
     *
     * @param s     message
     */
    private void showError(String s) {
        if (debug == true)
            printString("Error: " + s);
    }


    /**
     * Prints an information string
     *
     * @param s     message
     */
    private static void printString(String s) {
        output("info string " + s);
    }


    /**
     * Prints an engine message to the standart output.
     *
     * @param message   engine message
     */
    private static void output(String message) {
        synchronized (System.out) {
            System.out.println(message);
        }
    }


    /**
     * Builds a {@code String} from the {@code Scanner} concatenating
     * each token until it founds a token that matches the stop pattern
     * or the end of input is reached.
     *
     * @param scanner  The scanner from which to consume tokens
     * @param stop     The stop pattern
     */
    private static String consumeString(Scanner scanner, Pattern stop) {
        StringBuilder sb = new StringBuilder();

        while (scanner.hasNext() && !scanner.hasNext(stop)) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(scanner.next());
        }

        return sb.toString();
    }
}
