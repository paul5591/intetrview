package ru.lgnpvl.aop_attempt.controller;
/*
Реализуйте throttler для ограничения частоты событий от IoT устройств.
Датчики отправляют события, но нужно контролировать их частоту для экономии bandwidth.

Требования:
- Использование sliding window алгоритма
- Потокобезопасность - события приходят из разных потоков
- Очистка устаревших записей
*/

import org.springframework.util.CollectionUtils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

class EventThrottler {
    private final int maxEventsByDevice;
    private final Duration windowDuration;
    private final Map<String, Deque<Instant>> events;

    public EventThrottler(int maxEventsByDevice, Duration windowDuration) {
        this.maxEventsByDevice = maxEventsByDevice;
        this.windowDuration = windowDuration;
        events = new ConcurrentHashMap<>();
    }

    /**
     * Проверяет, можно ли принять eventCount событий от устройства.
     * Каждое устройство имеет свой независимый лимит.
     *
     * @param deviceId идентификатор устройства
     * @param eventCount количество событий для добавления
     * @return true если события приняты, false если превышен лимит
     */
    public boolean shouldAcceptEvent(String deviceId, int eventCount) {
        if (eventCount > maxEventsByDevice) return false;

        // Используем AtomicBoolean, чтобы вытащить результат из лямбды
        AtomicBoolean accepted = new AtomicBoolean(false);

        events.compute(deviceId, (id, queue) -> {
            if (queue == null) queue = new ConcurrentLinkedDeque<>();

            Instant windowStart = Instant.now().minus(windowDuration);

            // 1. Чистим старье
            while (!queue.isEmpty() && queue.peekFirst().isBefore(windowStart)) {
                queue.pollFirst();
            }

            // 2. Проверяем лимит ПЕРЕД добавлением
            if (queue.size() + eventCount <= maxEventsByDevice) {
                Instant now = Instant.now();
                for (int i = 0; i < eventCount; i++) {
                    queue.addLast(now);
                }
                accepted.set(true);
            }

            return queue;
        });

        return accepted.get();
    }
}