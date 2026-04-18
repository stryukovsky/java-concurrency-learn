package org.example;

public class VolatileStuff {

    volatile int someInt = 10;
    volatile boolean a = false;

    public void run() {
        var readingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                if (a == true) {
                    assert someInt == 1000;
                } else {
                    System.out.println("Cannot reproduce, seems reader started before the writer");
                }
            }
        });

        var writingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                someInt = 1000;
                a = true;
            }
        });

        writingThread.start();
        readingThread.start();
        try {
            writingThread.join();
            readingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
