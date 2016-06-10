/**
 * Copyright
 * Used org.apache.commons.math3.random library for MersenneTwister random number generation
 *
 * Design:
 *
 * a. AtomicLong type is being used for seed to generate MersenneTwister rng across concurrent threads
 * to make sure that we maximize the seed range within 10min interval and seed is not repeated across threads until
 * it reaches the MAX limit
 *
 * b. Multi threading is used to speed up the task of generating powerball combinations. No. of threads spawned
 * is based on the system's available Processors. That way it is not hard-coded to some magic number
 *
 * c. comparing if a powerball combination matches the winning combination:
 * - once the task of generating powerball combination within 10min interval is carried out in a
 * multi-threaded environment, compare logic kicks-in in the same multi-threaded environment to make it faster.
 * - Compare logic uses binary tree search method to check if the whiteball numbers are there in winning combination.
 * - Chose binary tree search over sorting the array of whiteballs and then compare it with winning combination because
 * sorting of each of whiteball array would be expensive O(nlogn) as compared to binarysearch
 *
 *
 */
package ai.h2o;

import java.lang.Object;
import java.util.*;
import java.lang.Number;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.random.BitsStreamGenerator;
import org.apache.commons.math3.random.MersenneTwister;


/**
 * Created by anupama.janakiram on 6/7/16.
 */
public class PowerBallTask extends Thread {
    int taskId;
    long starttime;
    static AtomicLong seed;
    final long MAXTIME = 2 * 60* 1000; // 10 MINUTE testing
    int winning_powerball = 10;
    //ArrayList<Integer> winning_whiteballs = new ArrayList<Integer>(Arrays.asList(4, 8, 19, 27, 34));
    Integer[] winning_whiteballs = new Integer[]{4, 8, 19, 27, 34};
    Integer[] ex_whiteballs = new Integer[]{19, 8, 4, 27, 34};
    List<PowerBallData> dataList = new LinkedList<>();
    Map<Long, Integer> winningSeeds = new HashMap<>();

    static {
        seed = new AtomicLong();
        
    }

    public PowerBallTask(int _taskId, long _starttime) {
        taskId = _taskId;
        starttime = _starttime;

    }

    public void run() {
        System.out.println(" ** Start Task #" + taskId);
        long loop = MAXTIME - (System.currentTimeMillis() - starttime);
        while (loop > 0) {
            System.out.println(" ** Task #" + taskId + ":" +loop);
            performAction();
            loop = MAXTIME - (System.currentTimeMillis() - starttime);

        }

        performCompare(dataList);
        //test_evaluateSets();

        System.out.println(" ** Stop Task #" + taskId);

    }

    /**
     * Logic to get Powerball sequence
     */

    private PowerBallData generatePowerballOutcome() {
        Long iseed = seed.getAndIncrement();
        return generatePowerballOutcomeforSeed(iseed);
    }

    /**
     * Logic to get Powerball sequence
     */

    public PowerBallData generatePowerballOutcomeforSeed(long iseed) {
        //MersenneTwister rng = new MersenneTwister(iseed);
        Random rng = new Random(iseed);
        while (true) {
            int WB1 = rng.nextInt(69) + 1;
            int WB2 = rng.nextInt(69) + 1;
            int WB3 = rng.nextInt(69) + 1;
            int WB4 = rng.nextInt(69) + 1;
            int WB5 = rng.nextInt(69) + 1;
            int PB = rng.nextInt(26) + 1;
            Set<Integer> collisionCheck = new HashSet<Integer>(Arrays.asList(new Integer[]{WB1, WB2, WB3, WB4, WB5}));
            if (collisionCheck.size() == 5) {
                // We are using the [Rejection sampling](https://en.wikipedia.org/wiki/Rejection_sampling) strategy here: if
                // the numbers are all distinct then we return the sample generated. Otherwise we just draw again (and again,
                // and again, ...) until the correct combo is found.
                PowerBallData pbData = new PowerBallData(iseed, PB, collisionCheck.toArray(new Integer[5]));
                dataList.add(pbData);
                return pbData; //"{" + iseed + ": " + WB1 + ", " + WB2 + ", " + WB3 + ", " + WB4 + ", " + WB5 + "; " + PB + "}";

            }
        }
    }

    public Boolean verifyWinningSeed(Long iseed) {
        PowerBallData pData = generatePowerballOutcomeforSeed(iseed);

        return pData != null && (pData.powerball == winning_powerball && isJackpot(pData.whiteballs, winning_whiteballs));
    }

    public void performAction() {

        System.out.println(" Running task " + taskId);
        try {
            Thread.sleep(2);
            //AtomicLong seed=System.currentTimeMillis() + (long)System.identityHashCode(this);
            PowerBallData result = this.generatePowerballOutcome();
            //this.generatePowerballOutcome();
            System.out.print("result : " + result.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isJackpot(Integer[] input, Integer[] refArray) {

        for(int i=0;i<input.length;i++) {
            int key = input[i];
            int min = 0, max = input.length, k = (min + max) / 2;
            boolean match = false;
           // System.out.println("1. Iteration "+i + ": Key "+key);
            while (min < max) {
                if (key == refArray[k]) {
                    //System.out.println("Match found for "+key);
                    match = true;
                    break;
                } else if (key > refArray[k]) {
                    min = k + 1;
                    k = (min + max) / 2;
                } else if (key < refArray[k]) {
                    max = k;
                    k = (min + max) / 2;
                }
            }
            if(!match) {
                return false;
            }
        }

        return true;
    }


    public void evaluateSets() {
        for(PowerBallData data:dataList) {
            if (data.powerball == 10){
                if(isJackpot(data.whiteballs,this.winning_whiteballs )) {
                    System.out.println(" Found Jackpot ! "+data.iseed);
                    if(winningSeeds.containsKey(data.iseed)) {
                        Integer currentCount = winningSeeds.get(data.iseed);
                        currentCount++;
                        winningSeeds.put(data.iseed, currentCount);
                    } else {
                        winningSeeds.put(data.iseed, 1);
                    }
                }
            }
        }
    }

    public void test_evaluateSets(Integer[] input, Integer[] refArray) {

            if(isJackpot(input,refArray )) {
                System.out.println(" Found Jackpot! ");

            }

    }

    public Map<Long,Integer> getWinningSeeds(){
        return winningSeeds;
    }

    public void performCompare(List<PowerBallData> dataList ) {


        System.out.println(" Comparing PowerBallData objects under task with the winning combination" + taskId);
        try {
            Thread.sleep(2);
            evaluateSets();
            /*Set<Long> seedKeySet = winningSeeds.keySet();
            for(Long seedVal:seedKeySet) {
                System.out.print("result : " + winningSeeds.get(seedKeySet));
            }*/
            System.out.print("result winningSeeds.size(): " + winningSeeds.size());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*PowerBallData is the data-structure to store the powerball combination along with the seed
      each object under individual thread is put into a dataList which is later used to compare
      with the winning combination
     */
    public class PowerBallData {

        Long iseed;
        Integer[] whiteballs;
        int powerball;

        public PowerBallData(Long iseed, int powerball, Integer[] whiteballs) {
            this.iseed = iseed;
            this.powerball = powerball;
            this.whiteballs = whiteballs;
        }

        public String toString() {
            StringBuilder ret= new StringBuilder();
            ret.append("{").append(iseed).append(":");
            if(whiteballs!=null) {
                for (int i = 0; i < whiteballs.length; i++) {
                    ret.append(whiteballs[i]);
                    if (i < whiteballs.length - 2) {
                        ret.append(",");
                    }
                }
            }
            ret.append(";").append(powerball).append("}");

            return ret.toString();
        }
    }
}