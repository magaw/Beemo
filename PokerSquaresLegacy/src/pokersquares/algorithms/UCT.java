package pokersquares.algorithms;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import pokersquares.config.Settings;
import static pokersquares.config.Settings.Algorithms.UCT;
import static pokersquares.config.Settings.Algorithms.debugUCT;
import static pokersquares.config.Settings.Algorithms.simSampleSize;
import pokersquares.environment.*;
import pokersquares.evaluations.PatternPolicy;
import pokersquares.evaluations.PositionRank;

public class UCT extends Algorithm{
    static double epsilon = 0.000000001; //Some Small Number
    int totalSimulations = 0;
    double maxScore = Double.NEGATIVE_INFINITY;
    
    private class PosVal {
        Integer[] xy;
        double totalScore = 0;
        double numSim = 0;
    }
    
    @Override
    public int[] internalSearch(final Card card, final Board board, long millisRemaining) {
        if (debugUCT) System.out.println("\nUCT");
        
        long tBuffer = millisRemaining - 1000 + System.currentTimeMillis(); //Some amount of millis to make sure we dont exceed alotted millis
        
        Integer[] bestPos = {2, 2};
        Double bestScore = Double.NEGATIVE_INFINITY;
        
        Integer[][] positions = new Integer[board.getOpenPos().size()][];
        positions = board.getOpenPos().toArray(positions); //COMMENT to use symmetry optimization
        
        //INITIALIZE posValues
        HashMap <Integer, PosVal> posValues = new HashMap <Integer, PosVal> ();
        int maxSim = simSampleSize/Settings.Evaluations.numThreads * positions.length;
        for (Integer[] p : positions) {
            int ph = p[0]*5 + p[1];
            PosVal pv = new PosVal();
            pv.xy = p;
            posValues.put(ph,pv);
        }
        
        //WHILE there is time remaining, run simulations
        totalSimulations = 0;
        while(System.currentTimeMillis() < tBuffer) {
            int gamesPer = 160;
            //GET next pos
            Integer[] pos = bestUCTPos(positions, posValues);
            
            //PLAY Card
            Board b = new Board(board);
            
            b.playCard(card, new int[]{pos[0], pos[1]});
            
            //SIMULATE Games
            double score = Simulator.simulate(b, gamesPer, 1, totalSimulations);
            //b.debug();
            //System.out.println(score);
            
            //RECORD Score
            int posHash = pos[0] * 5 + pos[1];
            PosVal pv = posValues.get(posHash);
            
            pv.totalScore += score;
            pv.numSim += gamesPer;
            
            totalSimulations += gamesPer;
            
            if (totalSimulations >= maxSim) break;
            
        }
        
        if (debugUCT) System.out.println("Total Simulations " + totalSimulations * 16);
        
        //CHOOSE Best Pos
        for (Integer ph : posValues.keySet()) {
            PosVal pv = posValues.get(ph);
            if (debugUCT) System.out.println("{ " + pv.xy[0] + " ," + pv.xy[1] + " } " + pv.numSim + " " + (pv.totalScore/pv.numSim));
            double score = pv.totalScore/pv.numSim;
            if (score > bestScore) {
                bestScore = score;
                bestPos = pv.xy;
            }
        }
        
        return new int[] {bestPos[0], bestPos[1]};
    }
    
    public Integer[] bestUCTPos(Integer[][] positions, HashMap posValues) {
        Integer[] bestPos = {0,0};
        double bestUCT = Double.NEGATIVE_INFINITY;
        
        for(Integer[] pos : positions){
            int ph = pos[0] * 5 + pos[1];
            PosVal pv = (PosVal) posValues.get(ph);
            
            double posUCT = getUCT( pv, 1, UCT);
            
            if (posUCT > bestUCT) {
                bestUCT = posUCT;
                bestPos = pos;
            }
            
        }
        
        return bestPos;    
    }
    
    private double getUCT(PosVal pv, double A, double B) {
        //return the UCT value
        
        double score = (pv.totalScore / (pv.numSim+epsilon));
        maxScore = (score > maxScore) ? score : maxScore;
        
        double uctValue = 
            //average simulation value of a node scaled to the continuous range {0,1}
            score * A / (maxScore + epsilon)
                
            //uct term
            + B * Math.sqrt( Math.abs(Math.log(totalSimulations+epsilon)) / (pv.numSim+epsilon));
        //System.out.println(score * A / (maxScore + epsilon) +"\t" + pv.numSim+ " \t"  + B * Math.sqrt( Math.abs(Math.log(totalSimulations+epsilon)) / (pv.numSim+epsilon)));
        return uctValue;
    }
}