package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBBooking;
import de.hallenbelegung.application.domain.model.Booking;

public class BookingPersistenceMapper {

    private final HallPersistenceMapper hallMapper;
    private final UserPersistenceMapper userMapper;
    private final BookingSeriesPersistenceMapper seriesMapper;

    public BookingPersistenceMapper(HallPersistenceMapper hallMapper,
                                    UserPersistenceMapper userMapper,
                                    BookingSeriesPersistenceMapper seriesMapper) {
        this.hallMapper = hallMapper;
        this.userMapper = userMapper;
        this.seriesMapper = seriesMapper;
    }

    public Booking toDomain(DBBooking entity) {
        if (entity == null) {
            return null;
        }

        return new Booking(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getStatus(),
                entity.getParticipantCount(),
                Boolean.TRUE.equals(entity.getConducted()),
                entity.getFeedbackComment(),
                hallMapper.toDomain(entity.getHall()),
                userMapper.toDomain(entity.getResponsibleUser()),
                seriesMapper.toDomain(entity.getBookingSeries()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                userMapper.toDomain(entity.getCreatedBy()),
                userMapper.toDomain(entity.getUpdatedBy()),
                userMapper.toDomain(entity.getCancelledBy()),
                entity.getCancelledAt(),
                entity.getCancelReason()
        );
    }

    public DBBooking toEntity(Booking domain) {
        if (domain == null) {
            return null;
        }

        DBBooking entity = new DBBooking();
        updateEntity(entity, domain);
        return entity;
    }

    public void updateEntity(DBBooking entity, Booking domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setStartAt(domain.getStartAt());
        entity.setEndAt(domain.getEndAt());
        entity.setStatus(domain.getStatus());
        entity.setParticipantCount(domain.getParticipantCount());
        entity.setConducted(domain.isConducted());
        entity.setFeedbackComment(domain.getFeedbackComment());
        entity.setCancelReason(domain.getCancelReason());
        entity.setHall(hallMapper.toEntity(domain.getHall()));
        entity.setResponsibleUser(userMapper.toEntity(domain.getResponsibleUser()));
        entity.setBookingSeries(seriesMapper.toEntity(domain.getBookingSeries()));
        entity.setCreatedBy(userMapper.toEntity(domain.getCreatedBy()));
        entity.setUpdatedBy(userMapper.toEntity(domain.getUpdatedBy()));
        entity.setCancelledBy(userMapper.toEntity(domain.getCancelledBy()));
        entity.setCancelledAt(domain.getCancelledAt());
    }
}