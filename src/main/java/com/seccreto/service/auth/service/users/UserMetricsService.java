package com.seccreto.service.auth.service.users;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

/**
 * Serviço de métricas customizadas para monitoramento de negócio.
 * 
 * Características de implementação sênior:
 * - Métricas de negócio específicas
 * - Contadores e timers customizados
 * - Monitoramento de performance
 */
@Service
public class UserMetricsService {

    private final Counter userCreatedCounter;
    private final Counter userUpdatedCounter;
    private final Counter userDeletedCounter;
    private final Counter userSuspendedCounter;
    private final Counter userActivatedCounter;
    private final Timer userOperationTimer;

    public UserMetricsService(MeterRegistry meterRegistry) {
        this.userCreatedCounter = Counter.builder("users.created")
                .description("Total number of users created")
                .register(meterRegistry);
        
        this.userUpdatedCounter = Counter.builder("users.updated")
                .description("Total number of users updated")
                .register(meterRegistry);
        
        this.userDeletedCounter = Counter.builder("users.deleted")
                .description("Total number of users deleted")
                .register(meterRegistry);
        
        this.userSuspendedCounter = Counter.builder("users.suspended")
                .description("Total number of users suspended")
                .register(meterRegistry);

        this.userActivatedCounter = Counter.builder("users.activated")
                .description("Total number of users activated")
                .register(meterRegistry);

        this.userOperationTimer = Timer.builder("users.operation.duration")
                .description("Duration of users operations")
                .register(meterRegistry);
    }

    public void incrementUserCreated() {
        userCreatedCounter.increment();
    }

    public void incrementUserUpdated() {
        userUpdatedCounter.increment();
    }

    public void incrementUserDeleted() {
        userDeletedCounter.increment();
    }

    public void incrementUserSuspended() {
        userSuspendedCounter.increment();
    }

    public void incrementUserActivated() {
        userActivatedCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void recordTimer(Timer.Sample sample) {
        sample.stop(userOperationTimer);
    }
}
