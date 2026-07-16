package com.notifsvc.notification;

import com.notifsvc.channel.ChannelType;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** One bounded thread pool per channel type, so a slow/overloaded channel can't starve the others. */
@Component
public class ChannelWorkerPools {

    private final Map<ChannelType, ExecutorService> pools = new EnumMap<>(ChannelType.class);

    public ChannelWorkerPools(@Value("${notifsvc.dispatch.pool-size-per-channel:4}") int poolSizePerChannel) {
        for (ChannelType channelType : ChannelType.values()) {
            pools.put(channelType, Executors.newFixedThreadPool(poolSizePerChannel, dispatchThreadFactory(channelType)));
        }
    }

    public void submit(ChannelType channelType, Runnable task) {
        pools.get(channelType).submit(task);
    }

    private static java.util.concurrent.ThreadFactory dispatchThreadFactory(ChannelType channelType) {
        return runnable -> {
            Thread thread = new Thread(runnable, "dispatch-" + channelType.name().toLowerCase() + "-" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        };
    }

    @PreDestroy
    public void shutdown() {
        pools.values().forEach(pool -> {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }
}
