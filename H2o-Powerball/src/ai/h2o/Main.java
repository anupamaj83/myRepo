package ai.h2o;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class Main {

    public static void main(String[] args) {
        // Start time of the program
        long startTime = System.currentTimeMillis();

        // Get processors
        int maxProcessors = Runtime.getRuntime().availableProcessors();
        System.out.print("Total no. of processors on machine  "+maxProcessors);
        /*PowerBallTask task = new PowerBallTask(1,startTime);
        String result = task.generatePowerballOutcome(System.currentTimeMillis());
        System.out.print("result from seed 29965454158L "+result);*/
        maxProcessors=1;
        List<PowerBallTask> taskList = new ArrayList<>(maxProcessors);
        for(int i =0;i<maxProcessors;i++) {
            PowerBallTask task = new PowerBallTask(i, startTime);
            taskList.add(task);
            task.start();

        }	// write your code here

        for(PowerBallTask taskObj:taskList) {
            try {
                taskObj.join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }


    }
}
