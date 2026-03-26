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

        // Note: domain BookingRequest does not contain processedBy/processedAt fields
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
                entity.getCreatedAt(),
                entity.getUpdatedAt()
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
        // domain getter names: getstartAt / getendAt
        entity.setStartAt(domain.getstartAt());
        entity.setEndAt(domain.getendAt());
        entity.setStatus(domain.getStatus());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setHall(hallMapper.toEntity(domain.getHall()));
        // domain exposes requesting user via requestedBy()
        entity.setRequestedBy(userMapper.toEntity(domain.requestedBy()));
        // processedBy/processedAt are not part of domain model and should be managed separately
    }
}