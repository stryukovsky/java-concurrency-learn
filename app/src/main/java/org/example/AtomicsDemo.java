package org.example;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicsDemo {

    private AtomicInteger counter = new AtomicInteger(0);

    public void run() {
        var threads = new ArrayList<Thread>(3);
        for (int i = 0; i < 3; i++) {
            var t = new Thread(new Runnable() {

                @Override
                public void run() {
                    System.out.println(counter.getAndIncrement());
                }
            });
            t.run();
        }
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

    }
}
