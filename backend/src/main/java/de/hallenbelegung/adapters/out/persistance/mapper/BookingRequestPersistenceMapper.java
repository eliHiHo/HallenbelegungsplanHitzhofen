package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingRequest;
import de.hallenbelegung.application.domain.model.BookingRequest;

public class BookingRequestPersistenceMapper {

    private final HallPersistenceMapper hallMapper;
    private final UserPersistenceMapper userMapper;

    public BookingRequestPersistenceMapper(HallPersistenceMapper hallMapper,
                                           UserPersistenceMapper userMapper) {
        this.hallMapper = hallMapper;
        this.userMapper = userMapper;
    }

    public BookingRequest toDomain(DBBookingRequest entity) {
        if (entity == null) {
            return null;
        }

        return new BookingRequest(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getStatus(),
                entity.getRejectionReason(),
                hallMapper.toDomain(entity.getHall()),
                userMapper.toDomain(entity.getRequestedBy()),
                userMapper.toDomain(entity.getProcessedBy()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getProcessedAt()
        );
    }

    public DBBookingRequest toEntity(BookingRequest domain) {
        if (domain == null) {
            return null;
        }

        DBBookingRequest entity = new DBBookingRequest();
        updateEntity(entity, domain);
        return entity;
    }

    public void updateEntity(DBBookingRequest entity, BookingRequest domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setStartAt(domain.getStartAt());
        entity.setEndAt(domain.getEndAt());
        entity.setStatus(domain.getStatus());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setHall(hallMapper.toEntity(domain.getHall()));
        entity.setRequestedBy(userMapper.toEntity(domain.getRequestedBy()));
        entity.setProcessedBy(userMapper.toEntity(domain.getProcessedBy()));
        entity.setProcessedAt(domain.getProcessedAt());
    }
}