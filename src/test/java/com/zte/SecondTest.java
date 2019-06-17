package com.zte;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestSuiteProfilerExtension.class)
class SecondTest {

    @Test
    void name() throws InterruptedException {
        System.out.println("one more! "+Thread.currentThread().getName());
        Thread.sleep(10000);
    }
}


