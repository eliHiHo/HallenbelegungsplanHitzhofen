package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingSeriesRequest;
import de.hallenbelegung.application.domain.model.BookingSeriesRequest;

public class BookingSeriesRequestPersistenceMapper {

    private final HallPersistenceMapper hallMapper;
    private final UserPersistenceMapper userMapper;

    public BookingSeriesRequestPersistenceMapper(HallPersistenceMapper hallMapper,
                                                 UserPersistenceMapper userMapper) {
        this.hallMapper = hallMapper;
        this.userMapper = userMapper;
    }

    public BookingSeriesRequest toDomain(DBBookingSeriesRequest entity) {
        if (entity == null) {
            return null;
        }

        return new BookingSeriesRequest(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getWeekday(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStartDate(),
                entity.getEndDate(),
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

    public DBBookingSeriesRequest toEntity(BookingSeriesRequest domain) {
        if (domain == null) {
            return null;
        }

        DBBookingSeriesRequest entity = new DBBookingSeriesRequest();
        updateEntity(entity, domain);
        return entity;
    }

    public void updateEntity(DBBookingSeriesRequest entity, BookingSeriesRequest domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setWeekday(domain.getWeekday());
        entity.setStartTime(domain.getStartTime());
        entity.setEndTime(domain.getEndTime());
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setStatus(domain.getStatus());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setHall(hallMapper.toEntity(domain.getHall()));
        entity.setRequestedBy(userMapper.toEntity(domain.getRequestedBy()));
        entity.setProcessedBy(userMapper.toEntity(domain.getProcessedBy()));
        entity.setProcessedAt(domain.getProcessedAt());
    }
}