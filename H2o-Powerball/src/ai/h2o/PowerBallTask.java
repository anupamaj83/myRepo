package ai.h2o;

import java.lang.Object;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.lang.Number;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Arrays;
import org.apache.commons.math3.random.BitsStreamGenerator;
import org.apache.commons.math3.random.MersenneTwister;


/**
 * Created by anupama.janakiram on 6/7/16.
 */
public class PowerBallTask extends Thread {
    int taskId;
    long starttime;
    static AtomicLong seed;
    final long MAXTIME = 10*60*1000; // 1 MINUTE testing
    static  {
        seed = new AtomicLong();
    }
    public PowerBallTask(int _taskId, long _starttime) {
        taskId = _taskId;
        starttime = _starttime;
    }
    public void run() {
        System.out.println(" ** Start Task #"+taskId);
        while((MAXTIME - (System.currentTimeMillis() - starttime))>0) {
            performAction();
        }
        System.out.println(" ** Stop Task #"+taskId);
    }

    /**
     * Logic to get Powerball sequence
     */

    public String generatePowerballOutcome() {
        MersenneTwister rng = new MersenneTwister(seed.getAndIncrement());
        while (true) {
            int WB1 = rng.nextInt(69) + 1;
            int WB2 = rng.nextInt(69) + 1;
            int WB3 = rng.nextInt(69) + 1;
            int WB4 = rng.nextInt(69) + 1;
            int WB5 = rng.nextInt(69) + 1;
            int PB  = rng.nextInt(26) + 1;
            Set<Integer> collisionCheck = new HashSet<Integer>(Arrays.asList(new Integer[]{WB1, WB2, WB3, WB4, WB5}));
            if (collisionCheck.size() == 5) {
                // We are using the [Rejection sampling](https://en.wikipedia.org/wiki/Rejection_sampling) strategy here: if
                // the numbers are all distinct then we return the sample generated. Otherwise we just draw again (and again,
                // and again, ...) until the correct combo is found.
                return "{" + seed + ": " + WB1 + ", " + WB2 + ", " + WB3 + ", " + WB4 + ", " + WB5 + "; " + PB + "}";
            }
        }
    }

    public void performAction() {
        System.out.println(" Running task "+taskId);
        try {
            Thread.sleep(2);
            //AtomicLong seed=System.currentTimeMillis() + (long)System.identityHashCode(this);
            String result = this.generatePowerballOutcome();
            System.out.print("result : "+result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
