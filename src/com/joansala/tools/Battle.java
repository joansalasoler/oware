package com.joansala.tools;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

import com.joansala.engine.*;
import com.joansala.oware.*;


/**
 * Runs a Round-Robin tournament between oware engines.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public final class Battle {
    
    /** Engines participating in the tournament */
    private static Negamax[] engines = null;
    
    /** Engines participating in the tournament */
    private static OwareCache[] caches = null;
    
    /** Opening books for each engine */
    private static OwareRoots[] roots = null;
    
    /** Endgames used for each engine */
    private static OwareLeaves[] leaves = null;
    
    /** Names of the engines */
    private static String[] names = null;
    
    /** Sum of scores for each engine */
    private static float[] scores = null;
    
    /** Count of won matches for each engine */
    private static int[] wins = null;
    
    /** Count of lost matches for each engine */
    private static int[] losses = null;
    
    /** Output sepparator */
    private static String separator = "";
    
    /** Default book path */
    private static String bookPath = null;
    
    /** Default engames path */
    private static String leavesPath = null;
    
    /** Default time per move */
    private static long movetime = Negamax.DEFAULT_TIME;
    
    /** Default maximum depth */
    private static int depth = Negamax.MAX_DEPTH;
    
    /** Default hash size */
    private static int hashsize = 32;
    
    /** Maximum number of seeds for endgames */
    private static int numSeeds = OwareLeaves.MAX_SEEDS;
    
    /** Default number of matches */
    private static int matches = 1;
    
    
    /**
     * This class cannot be instantiated.
     */
    private Battle() { }
    
    
    /**
     * Shows an usage notice on the standard output
     */
    private static void showUsage(Exception e) {
        System.out.format(
            "Exception:%n%n" +
            "  %s%n%n" +
            "Usage:%n%n" +
            "  Battle [parameters] [properties file]%n%n" +
            "Valid parameters are:%n%n" +
            "  -matches   <int>%n" +
            "  -depth     <byte>    (plies)%n" +
            "  -movetime  <long>    (milliseconds)%n" +
            "  -hashsize  <int>     (MB)%n" +
            "  -num-seeds <byte>    (MB)%n" +
            "  -leaves    <string>  (file path)%n" +
            "  -book      <string>  (file path)%n",
            e.getMessage()
        );
    }
    
    
    /**
     * Plays a match between two engines and returns the result of the
     * played match.
     *
     * @param south     south player identifier
     * @param north     north player identifier
     * @return          match result
     */
    private static int disputeMatch(int south, int north) {
        OwareGame game = new OwareGame();
        
        engines[south].newMatch();
        engines[north].newMatch();
        
        if (roots[south] != null)
            roots[south].newMatch();
        
        if (roots[north] != null)
            roots[north].newMatch();
        
        while (game.hasEnded() == false) {
            int turn = game.turn();
            int index = (turn == Game.SOUTH) ? south : north;
            int move = Game.NULL_MOVE;
            
            try {
                if (roots[index] != null)
                    move = roots[index].pickBestMove(game);
            } catch (Exception e) {
                System.err.println("Error: Cannot pick book move");
            }
            
            if (move == Game.NULL_MOVE)
                move = engines[index].computeBestMove(game);
            
            game.ensureCapacity(2 + game.length());
            game.makeMove(move);
        }
        
        game.endMatch();
        
        return game.winner();
    }
    
    
    /**
     * Runs a tournament maching engines against each other in a
     * Round-Robin fashion.
     *
     * @param robin     Round-Robin tournament
     */
    private static void runTournament(int numMatches) {
        RoundRobin robin = new RoundRobin(engines.length);
        
        System.out.format("Running tournament%n%s%n", separator);
        
        for (int match = 0; match < numMatches; match++) {
            for (int round = 0; round < robin.numRounds(); round++) {
                int n = 1 + round + match * robin.numRounds();
                System.out.format("Round %3d.", n);
                
                for (int table = 0; table < robin.numTables(); table++) {
                    Integer[] pairing = robin.pairing(round, table);
                    Integer south = pairing[0], north = pairing[1];
                    
                    // Dispute the match if not a bye
                    
                    int result = Game.DRAW;
                    
                    if (south != null && north != null) {
                        result = disputeMatch(south, north);
                    } else {
                        result = (south == null) ?
                            Game.NORTH : Game.SOUTH;
                    }
                    
                    // Count the obtained result
                    
                    switch (result) {
                        case Game.SOUTH:
                            scores[south] += 1.0F;
                            losses[north] += 1;
                            wins[south] += 1;
                            break;
                            
                        case Game.NORTH:
                            scores[north] += 1.0F;
                            losses[south] += 1;
                            wins[north] += 1;
                            break;
                            
                        default:
                            scores[south] += 0.5F;
                            scores[north] += 0.5F;
                    }
                    
                    // Show the disputed match result
                    
                    System.out.format(
                        " %s-%s (%s)",
                        (south == null) ? "bye" :
                            String.format("%3d", 1 + south),
                        (north == null) ? "bye" :
                            String.format("%-3d", 1 + north),
                        (result == Game.SOUTH) ? "S" :
                            (result == Game.NORTH) ? "N" : "D"
                    );
                }
                
                System.out.println();
            }
            
            robin.invert();
        }
    }
    
    
    /**
     * Outputs the engines and their obtained scores, sorted acording
     * to the score from highest to lowest.
     */
    private static void printClassification() {
        // Sort engines by obtained score
        
        Integer[] classification = new Integer[names.length];
        
        for (int i = 0; i < names.length; i++)
            classification[i] = i;
        
        Arrays.sort(classification, new Comparator<Integer>() {
            public int compare(Integer i1, Integer i2) {                        
                return Float.compare(scores[i2], scores[i1]);
            }
        });
        
        // Show tournament classification
        
        System.out.format("Classification%n%s%n", separator);
        
        for (int n = 0; n < names.length; n++) {
            int i = classification[n];
            System.out.format(
                "%3d. %3d: %-30s W: %2d   L: %2d %8.1f points%n",
                n + 1, i + 1, names[i], wins[i], losses[i], scores[i]);
        }
    }
    
    
    /**
     * Outputs engine settings.
     */
    private static void printSettings() {
        System.out.format("Engine settings%n%s%n", separator);
        
        for (int i = 0; i < names.length; i++) {
            long capacity = caches[i].size();
            int maxdepth = engines[i].getDepth() + 1;
            long settime = engines[i].getMoveTime();
            String bookName = roots[i] == null ?
                "-" : roots[i].getField("Name");
            String leavesName = leaves[i] == null ?
                "-" : leaves[i].getField("Name");
            
            System.out.format(
                "%2d. Engine name:        %40s%n" +
                "    Openings book:      %40s%n" +
                "    Endgames book:      %40s%n" +
                "    Hash table size:    %,40d bytes%n" +
                "    Time per move:      %,40d ms%n" +
                "    Maximum depth:      %,40d plies%n%n",
                i + 1, names[i], bookName, leavesName,
                capacity, settime, maxdepth
            );
        }
    }
    
    
    /**
     * Initializes tournament objects.
     *
     * @param size  number of engines
     */
    private static void initObjects(int size) {
        engines = new Negamax[size];
        caches = new OwareCache[size];
        roots = new OwareRoots[size];
        leaves = new OwareLeaves[size];
        scores = new float[size];
        wins = new int[size];
        losses = new int[size];
    }
    
    
    /**
     * Initializes a new engine object.
     *
     * @param i         engine index
     * @param depth     maximum search depth
     * @param mtime     milliseconds per move
     * @param hsize     hash size in megabytes
     */
    private static void initEngine(int i, int depth, long mtime, int hsize) {
        caches[i] = new OwareCache(hsize << 20);
        engines[i] = new Negamax();
        
        engines[i].setInfinity(OwareGame.MAX_SCORE);
        engines[i].setMoveTime(mtime);
        engines[i].setDepth(depth);
        
        if (leaves[i] != null)
            engines[i].setLeaves(leaves[i]);
        
        engines[i].setCache(caches[i]);
    }
    
    
    /**
     * Initializes a book object.
     *
     * @param i      engine index
     * @param path   book path
     */
    private static void initRoots(int i, String path) {
        if (path == null)
            return;
        
        try {
            roots[i] = new OwareRoots(new File(path));
        } catch (Exception e) {
            showUsage(e);
            System.exit(1);
        }
    }
    
    
    /**
     * Initializes an endgame database object.
     *
     * @param i      engine index
     * @param path   book path
     */
    private static void initLeaves(int i, String path, int numSeeds) {
        if (path == null)
            return;
        
        try {
            leaves[i] = new OwareLeaves(new File(path), numSeeds);
        } catch (Exception e) {
            showUsage(e);
            System.exit(1);
        }
    }
    
    
    /**
     * Inititalitzes default objects
     */
    private static void initDefaultEngines() {
        names = new String[2];
        names[0] = "Matt";
        names[1] = "Elektra";
        
        initObjects(2);
        
        for (int i = 0; i < 2; i++) {
            initRoots(i, bookPath);
            initLeaves(i, leavesPath, numSeeds);
            initEngine(i, depth, movetime, hashsize);
        }
    }
    
    
    /**
     * Inititalitzes objects from a propierties file configuration
     *
     * @param propsPath     properties file path
     */
    private static void initFromProperties(String propsPath) throws IOException {
        Properties properties = new Properties();
        FileInputStream file = null;
        String value = null;
        
        try {
            file = new FileInputStream(propsPath);
            properties.load(file);
            
            // Obtain configured engines
            
            value = properties.getProperty("battle.engines");
            
            if (value == null) {
                throw new Exception("No engine names specified");
            } else {
                names = value.split(",\\s*");
                initObjects(names.length);
            }
            
            // Overwrite defaults with properties file values
            
            value = properties.getProperty("battle.matches");
            matches = (value == null) ?
                matches : Integer.parseInt(value);
            
            value = properties.getProperty("battle.depth");
            depth = (value == null) ?
                depth : Byte.parseByte(value);
            
            value = properties.getProperty("battle.movetime");
            movetime = (value == null) ?
                movetime : Long.parseLong(value);
            
            value = properties.getProperty("battle.hashsize");
            hashsize = (value == null) ?
                hashsize : Integer.parseInt(value);
            
            value = properties.getProperty("battle.numseeds");
            numSeeds = (value == null) ?
                numSeeds : Byte.parseByte(value);
            
            value = properties.getProperty("battle.book");
            bookPath = (value == null) ?
                bookPath : value;
            
            value = properties.getProperty("battle.leaves");
            leavesPath = (value == null) ?
                leavesPath : value;
            
            // Initialize the engines
            
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                String engineBookPath = bookPath;
                String engineLeavesPath = leavesPath;
                long engineMovetime = movetime;
                int engineDepth = depth;
                int engineNumSeeds = numSeeds;
                int engineHashsize = hashsize;
                
                value = properties.getProperty(name + ".depth");
                engineDepth = (value == null) ?
                    depth : Byte.parseByte(value);
                
                value = properties.getProperty(name + ".movetime");
                engineMovetime = (value == null) ?
                    movetime : Long.parseLong(value);
                
                value = properties.getProperty(name + ".hashsize");
                engineHashsize = (value == null) ?
                    hashsize : Integer.parseInt(value);
                
                value = properties.getProperty(name + ".numseeds");
                engineNumSeeds = (value == null) ?
                    numSeeds : Byte.parseByte(value);
                
                value = properties.getProperty(name + ".book");
                engineBookPath = (value == null) ?
                    bookPath : value;
                
                value = properties.getProperty(name + ".leaves");
                engineLeavesPath = (value == null) ?
                    leavesPath : value;
                
                initRoots(i, engineBookPath);
                initLeaves(i, engineLeavesPath, engineNumSeeds);
                initEngine(i, engineDepth, engineMovetime, engineHashsize);
            }
        } catch (Exception e) {
            showUsage(e);
            System.exit(1);
        } finally {
            if (file != null)
                file.close();
        }
    }
    
    
    /**
     * Runs a battle between two or more engines.
     *
     * @param argv  Command line arguments.
     */
    public static void main(String[] argv) throws IOException {
        // Output separator
        
        separator = new String(new char[76]).replace("\0", "=");
        
        // Default configuration
        
        String propsPath = null;
        
        // Overwrite defaults from command line arguments
        
        try {
            int i = 0;
            
            for (i = 0; i < argv.length - 1; i++) {
                if ("-depth".equals(argv[i])) {
                    depth = Byte.parseByte(argv[++i]);
                } else if ("-movetime".equals(argv[i])) {
                    movetime = Long.parseLong(argv[++i]);
                } else if ("-hashsize".equals(argv[i])) {
                    hashsize = Integer.parseInt(argv[++i]);
                } else if ("-matches".equals(argv[i])) {
                    matches = Integer.parseInt(argv[++i]);
                } else if ("-num-seeds".equals(argv[i])) {
                    numSeeds = Byte.parseByte(argv[++i]);
                } else if ("-leaves".equals(argv[i])) {
                    leavesPath = argv[++i];
                } else if ("-book".equals(argv[i])) {
                    bookPath = argv[++i];
                } else {
                    throw new IllegalArgumentException();
                }
            }
            
            if (i < argv.length)
                propsPath = argv[i];
        } catch (Exception e) {
            showUsage(e);
            System.exit(1);
        }
        
        // Initialize the contending engines
        
        if (propsPath == null) {
            initDefaultEngines();
        } else {
            initFromProperties(propsPath);
        }
        
        // Run the battle between engines
        
        printSettings();
        runTournament(matches);
        printClassification();
    }
    
}

