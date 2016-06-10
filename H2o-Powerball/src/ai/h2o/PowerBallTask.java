/**
 * Copyright
 * Used org.apache.commons.math3.random library for MersenneTwister random number generation
 * ,but ended up not using Java's Random class as I could not get the winning combination from MersenneTwister
 * while testing.
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
 *Key improvements and known issues:
 *
 * - Currently the powerball random number generation is not very efficient.
 *   I have to figure out ways to make it more efficient to find the max number of winning seeds.
 * - Have to tune the heap space usage.  Running it for 2hrs time interval resulted in OOM.
 *   Made some changes to help come out of this situation . I have to look at this in depth to fine tune the object memory usage.
 *   # Made some changes to limit elements getting into dataList. This significantly helped the memory usage
 *     Not seeing the OOM for now. But definitely need some more thought work
 *   # I'm thinking of limiting the  size of dataList (the one that stores the prng numbers generated)
 *   Once it reaches the limit, then do  performCompare() and restart the powerball prng from the seed where it was left earlier.
 *   This would help with the memory usage. This is not yet implemented
 *
 *
 *
 */
package ai.h2o;


import java.util.*;
import java.util.concurrent.atomic.AtomicLong;



/**
 * Created by anupama.janakiram on 6/7/16.
 */
public class PowerBallTask extends Thread {
    int taskId;
    long starttime;
    static AtomicLong seed;
    final long MAXTIME = 10 * 60 * 1000; // 10 MINUTE testing
    int winning_powerball = 10;
    int total_pb_number = 0;
    Integer[] winning_whiteballs = new Integer[]{4, 8, 19, 27, 34};
    Integer[] ex_whiteballs = new Integer[]{19, 8, 4, 27, 34};
    List<PowerBallData> dataList = new LinkedList<>();
    Map<Long, Integer> winningSeeds = new HashMap<>();

    static {
        seed = new AtomicLong();
        seed.set(29965454150L);

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

        System.out.println(" ** Stop Task #" + taskId);

    }

    /**
     * Logic to get Powerball sequence
     */

    private List<PowerBallData> generatePowerballOutcome() {
        Long iseed = seed.getAndIncrement();
        return generatePowerballOutcomeforSeed(iseed);
    }


    public List<PowerBallData> generatePowerballOutcomeforSeed(long iseed) {
        //MersenneTwister rng = new MersenneTwister(iseed);
        Random rng = new Random(iseed);
        int count = 0;
        while (count < 10) {
            int WB1 = rng.nextInt(69) + 1;
            int WB2 = rng.nextInt(69) + 1;
            int WB3 = rng.nextInt(69) + 1;
            int WB4 = rng.nextInt(69) + 1;
            int WB5 = rng.nextInt(69) + 1;
            int PB = rng.nextInt(26) + 1;
            Set<Integer> collisionCheck = new HashSet<Integer>(Arrays.asList(new Integer[]{WB1, WB2, WB3, WB4, WB5}));

            /* check if PB is winning PB, only then check for collision check and proceed */

                if (collisionCheck.size() == 5) {
                    // We are using the [Rejection sampling](https://en.wikipedia.org/wiki/Rejection_sampling) strategy here: if
                    // the numbers are all distinct then we return the sample generated. Otherwise we just draw again (and again,
                    // and again, ...) until the correct combo is found.
                    PowerBallData pbData = new PowerBallData(iseed, PB, collisionCheck.toArray(new Integer[5]));
                    if (PB == this.winning_powerball){
                    dataList.add(pbData);
                    }
                    total_pb_number++;
                    //return pbData; //"{" + iseed + ": " + WB1 + ", " + WB2 + ", " + WB3 + ", " + WB4 + ", " + WB5 + "; " + PB + "}";

                }
            count++;

        }
        return dataList;
    }

    public Boolean verifyWinningSeed(Long iseed) {
        List<PowerBallData> pDataList = generatePowerballOutcomeforSeed(iseed);
        for (PowerBallData powerBallData : pDataList) {

            if (powerBallData != null && (powerBallData.powerball == winning_powerball && isJackpot(powerBallData.whiteballs, winning_whiteballs))){
                return Boolean.TRUE;
            }

        }

        return Boolean.FALSE;
    }

    public void performAction() {

        System.out.println(" Running task " + taskId);
        try {
            Thread.sleep(2);
            List<PowerBallData> result = this.generatePowerballOutcome();

            /* to display the powerball Data generated for each seed
            for (PowerBallData powerBallData : result) {
                System.out.print("result : " + powerBallData.toString());
            }*/



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* isJackpot() method is to compare the whiteballs generated matches the
    winning whiteball sequence or not.
    It uses Binary Search algorithm
     */
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

    /* evaluateSets() checks for powerball and whiteballs balls generated if it matches the winning combination.
      If yes, puts te seed to winningSeeds map.
     */
    public void evaluateSets() {
        for(PowerBallData data:dataList) {
            if (winning_powerball == data.powerball){
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


    /* getter method for winningSeeds */
    public Map<Long,Integer> getWinningSeeds(){
        return winningSeeds;
    }

    public void performCompare(List<PowerBallData> dataList ) {


        System.out.println(" Comparing PowerBallData objects under task with the winning combination" + taskId);
        try {
            Thread.sleep(2);
            evaluateSets();
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
                    if (i < whiteballs.length - 1) {
                        ret.append(",");
                    }
                }
            }
            ret.append(";").append(powerball).append("}");

            return ret.toString();
        }
    }
}