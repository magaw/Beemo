/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pokersquares.trainers;

import java.util.*;
import pokersquares.algorithms.Simulator;
import pokersquares.config.Settings;
import static pokersquares.config.Settings.Training.policyMax;
import static pokersquares.config.Settings.Training.policyMin;
import pokersquares.config.SettingsReader;
import pokersquares.environment.Board;
import pokersquares.environment.Card;

/**
 *
 * @author newuser
 */
public class ValueReinforcement implements Trainer {
    
    @Override
    public void runSession(long millis){
        
        System.err.print("\nTraining\n");
        
        long tStart = System.currentTimeMillis();
        
        //VALUES to be adjusted
        List <double[]> values = new ArrayList();
        
        values.add(Settings.Evaluations.colHands); 
        values.add(Settings.Evaluations.rowHands);
        values.add(Settings.Evaluations.highCardPolicy);
        values.add(Settings.Evaluations.pairPolicy);
        values.add(Settings.Evaluations.twoPairPolicy);
        values.add(Settings.Evaluations.threeOfAKindPolicy);
        values.add(Settings.Evaluations.straightPolicy);
        values.add(Settings.Evaluations.flushPolicy);
        values.add(Settings.Evaluations.fullHousePolicy);
        values.add(Settings.Evaluations.fourOfAKindPolicy);
        
        if (Settings.Training.randomize) randomize(values);
        
        Settings.Evaluations.debug();
        
        int i = 0, j = 0;
        int valuesToTrain = values.size() - 1;
        boolean systemChanged = false;
        //WHILE there is time left, continue training
        while ((System.currentTimeMillis() - tStart) < millis) {
            
            //TRAIN Value
            //if value is successfully trained, the system has changed
            if (i < 2) {
                //systemChanged = trainHandCombinations(values, i);
            } else {
                systemChanged = trainValuesIncrementally(values, i, j);
            }
            
            System.out.println("\nValues To Train: " + valuesToTrain);
            
            if (j == values.get(i).length-1) {
                if (i == values.size()-1) {
                    i = 0;
                    j = 0;
                    
                } else {
                    ++i;
                    j = 0;
                }
                --valuesToTrain;
                
            } else {
                ++j;
            }
            
            if (systemChanged) valuesToTrain = values.size() - 2;
            
            //if (!systemChanged && (valuesToTrain == 0)) redistribute(values);
            if (!systemChanged && (valuesToTrain == 0)) break;
        }
        
        SettingsReader.writeSettings(Settings.Training.settingsFileOut);
        
    }
    
    public static void redistribute(List <double[]> values) {
        //redistribute all values so they are evenly spaced
        List <Double> uniqueValues = new ArrayList <Double> ();
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        
        //List all Values
        for (int i = 0; i < values.size(); ++i) {
            
            double[] value = values.get(i);
            for (int j = 0; j < value.length; ++j) {
                double val = value[j];
                if (val > max) max = val;
                if (val < min) min = val;
                if (!uniqueValues.contains(val) && (i > 2))
                    uniqueValues.add(val);
            }
        }
        
        //MAP Values
        Collections.sort(uniqueValues);
        Map <Double, Double> distributedValues = new HashMap <Double, Double> ();
        double dValue = 2.0 / ((double) uniqueValues.size() - 1);
        double nValue = Settings.Training.policyMin;
        
        for (int i = 0; i <  uniqueValues.size(); ++i) {
            distributedValues.put(uniqueValues.get(i), (Double) nValue);
            nValue += dValue;
        }
        
        System.err.println(uniqueValues.size() + " " + dValue + " " + nValue);
        System.err.println(distributedValues.toString());
        
        //REDISTRIBUTE Values
        for (Double uv : distributedValues.keySet()){
            for (int i = 0; i < values.size(); ++i) {
                double[] value = values.get(i);
                for (int j = 0; j < value.length; ++j) {
                    if ((value[j] == uv) && (i > 2)){
                        value[j] = distributedValues.get(uv);
                        
                    }
                    
                    
                }
            }
        }
        
    }
    
    public static void randomize(List <double[]> values) {
        //randomize initial values 
        Random r = new Random();
        
        for (int j = 0; j < values.size(); ++j) {
            double[] value = values.get(j);
            if (j > 1) for (int i = 0; i < value.length; ++i) value[i] = r.nextDouble();
        }
    }
    
    public static boolean trainHandCombinations(List values, int i) {
        //for each hand ID possible in the array, toggle them and compare performance
        boolean systemChanged = false; 
        
        double baseScore,deltaScore;
        
        //INSTANTIATE list to 
        double[] ha = ((double[])values.get(i));
        
        //COMPARE PERFORMANCE for each hand
        for (int id = 0; id < ha.length; ++id) {
            
            //establish baseline
            baseScore = scoreGames(1000);
            
            System.out.println("\nTraining Hand Combinations:" + " " + i + " " + id);
            System.out.println("Base Score: " + baseScore);
            System.out.println(Arrays.toString(ha));
            
            int og;
            
            //TOGGLE hand id
            if (ha[id] == 1) {
                ha[id] = 0;
                og = 1;
            } else {
                ha[id] = 1;
                og = 0;
            }
            
            System.out.println(Arrays.toString(ha));
            
            deltaScore = scoreGames(1000);
            
            System.out.println("Delta Score: " + deltaScore);
            
            //if PERFORMANCE DECREASES or remains the same
            //RESET
            if (deltaScore < baseScore) ha[id] = og;
            else systemChanged = true;
            
        }
            
        
        return systemChanged;
    }
    
    public static boolean trainValuesIncrementally(List<double[]> values, int i, int j) {
        //adjust the specified value in a positive or negative direction until a max score is reached
        double[]  va = values.get(i); //value array
        boolean systemChanged = false;
        int sign = -1;
        double 
                baseScore, 
                newScore,
                og,
                scale = va[j];
        
        
        
        boolean train = true;
        
        boolean verbose = true;

        while (train) {
            baseScore = scoreGames(1000);
            if(verbose) System.out.println("\nTraining Values Incrementally:"  + " " + i + " " + j);
            if(verbose) System.out.println("Base Score: " + baseScore);
            //STORE original value
            og = va[j];
            
            //ADJUST value 
            if(verbose) System.out.print(va[j]);
            va[j] = va[j] + (sign*scale);
            if (va[j] < policyMin) va[j] = 0.0;
            else if (va[j] > policyMax) va[j] = 1.0;
            if(verbose) System.out.print("-->" + va[j] + ": ");
            //SCORE PERFORMANCE
            newScore = scoreGames(1000);
            if(verbose) System.out.println("New Score: " + newScore);
            
            //if PERFORMANCE DECREASES
            if (newScore < baseScore) {
                //RESET value
                va[j] = og;
                
                //INCREMENT value adjustors
                if (scale > 0.0001) scale = scale / 2.0;
                else if (sign == -1) {
                    sign = 1;
                    scale = 1.0 - va[j];
                }
                else train = false;
            } else if (newScore == baseScore) {
                //if performance does not change 
                if (scale > 0.0001) scale = scale / 2.0;
                if (sign == -1) {
                    sign = 1;
                    scale = 1.0 - va[j];
                } else {
                    train = false;
                }
                //RESET value
                //va[j] = og;
                
            } else {
                System.out.println(og + "-->" + va[j] + ": Δ" + (newScore - baseScore));
                //PERFORMANCE INCREASES
                //RECORD
                systemChanged = true;
                SettingsReader.writeSettings(Settings.Training.settingsFileOut);
            } 
        }
        
        return systemChanged;
    }
    
    public static double scoreGames(int numGames) {
        int numSimulations = numGames;
        //RESET patterns, so as not to retain old, bad evaluations
        pokersquares.evaluations.PatternPolicy.patternEvaluations = new java.util.HashMap();
        //SIMULATE Games
        return Simulator.simulate(new Board(), numSimulations, 10000, 1) / (double)(numGames+1);
    }
    
}
