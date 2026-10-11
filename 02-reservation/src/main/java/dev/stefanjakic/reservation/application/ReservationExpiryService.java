package dev.stefanjakic.reservation.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReservationExpiryService {

    private final ReservationRepository repository;

    public ReservationExpiryService(
        ReservationRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int expire(Instant now) {
        return repository.expireHolds(now);
    }
}
