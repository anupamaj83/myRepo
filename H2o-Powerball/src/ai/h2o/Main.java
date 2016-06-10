package ai.h2o;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedList;
import java.util.List;


public class Main {

    public static void main(String[] args) {
        // Start time of the program
        long startTime = System.currentTimeMillis();
        int winningSeed_count = 0;
        Set<Long> winningSeeds = new HashSet<Long>();

        // Get processors
        int maxProcessors = Runtime.getRuntime().availableProcessors();
        System.out.print("Total no. of processors on machine  "+maxProcessors);

        List<PowerBallTask> taskList = new ArrayList<>(maxProcessors);
        for(int i =0;i<maxProcessors;i++) {
            PowerBallTask task = new PowerBallTask(i, startTime);
            taskList.add(task);
            task.start();

        }

        for(PowerBallTask taskObj:taskList) {
            try {
                taskObj.join();
                winningSeed_count = winningSeed_count+ taskObj.getWinningSeeds().keySet().size();

                for (Long aLong : taskObj.getWinningSeeds().keySet()) {

                    /* Test the winning seeds before reporting */
                    if (taskObj.verifyWinningSeed(aLong)){
                        System.out.print("\nVerified again before adding to winning seed list for seed: " + aLong );
                        winningSeeds.add(aLong);
                    }


                }



            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        System.out.print("\nTotal no. of Winning Seeds found in the given interval of time : "+ winningSeed_count);
        if (winningSeed_count > 0) {
            System.out.print("\nList of Winning Seeds found:");
            for (Long winningSeed : winningSeeds) {
                System.out.print("\n"+winningSeed);
            }
        }




    }
}
