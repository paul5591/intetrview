package ru.lgnpvl.aop_attempt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventThrottlerTest {

    class Starter implements Runnable {

        private EventThrottler eventThrottler;

        public Starter(EventThrottler eventThrottler) {
            this.eventThrottler = eventThrottler;
        }

        @Override
        public void run() {
            eventThrottler.shouldAcceptEvent("sensor-temp-01", 3);
        }
    }

    @Test
    public void testBasicEventThrottling() throws InterruptedException {
        EventThrottler throttler = new EventThrottler(3, Duration.ofSeconds(1));

        Thread thread1 = new Thread(new Starter(throttler));
        Thread thread2 = new Thread(new Starter(throttler));
        thread1.start();
        Thread.sleep(800);
        thread2.start();
        Thread.sleep(1000);

        String deviceId = "sensor-temp-01";

        assertTrue(throttler.shouldAcceptEvent(deviceId, 1));
        assertTrue(throttler.shouldAcceptEvent(deviceId, 2));

        // 4-е событие должно быть отклонено
        assertFalse(throttler.shouldAcceptEvent(deviceId, 1));

        Thread.sleep(1100);
        assertTrue(throttler.shouldAcceptEvent(deviceId, 1));
    }

}