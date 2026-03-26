package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBBookingSeries;
import de.hallenbelegung.application.domain.model.BookingSeries;

public class BookingSeriesPersistenceMapper {

    private final HallPersistenceMapper hallMapper;
    private final UserPersistenceMapper userMapper;

    public BookingSeriesPersistenceMapper(HallPersistenceMapper hallMapper,
                                          UserPersistenceMapper userMapper) {
        this.hallMapper = hallMapper;
        this.userMapper = userMapper;
    }

    public BookingSeries toDomain(DBBookingSeries entity) {
        if (entity == null) {
            return null;
        }

        return new BookingSeries(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getWeekday(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus(),
                hallMapper.toDomain(entity.getHall()),
                userMapper.toDomain(entity.getResponsibleUser()),
                userMapper.toDomain(entity.getCreatedBy()),
                userMapper.toDomain(entity.getUpdatedBy()),
                userMapper.toDomain(entity.getCancelledBy()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCancelledAt(),
                entity.getCancelReason()
        );
    }

    public DBBookingSeries toEntity(BookingSeries domain) {
        if (domain == null) {
            return null;
        }

        DBBookingSeries entity = new DBBookingSeries();
        updateEntity(entity, domain);
        return entity;
    }

    public void updateEntity(DBBookingSeries entity, BookingSeries domain) {
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
        entity.setHall(hallMapper.toEntity(domain.getHall()));
        entity.setResponsibleUser(userMapper.toEntity(domain.getResponsibleUser()));
        entity.setCreatedBy(userMapper.toEntity(domain.getCreatedBy()));
        entity.setUpdatedBy(userMapper.toEntity(domain.getUpdatedBy()));
        entity.setCancelledBy(userMapper.toEntity(domain.getCancelledBy()));
        entity.setCancelReason(domain.getCancelReason());
        entity.setCancelledAt(domain.getCancelledAt());
    }
}