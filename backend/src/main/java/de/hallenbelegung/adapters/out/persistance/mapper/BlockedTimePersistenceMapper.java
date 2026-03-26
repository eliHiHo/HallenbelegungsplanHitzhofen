package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBBlockedTime;
import de.hallenbelegung.application.domain.model.BlockedTime;

public class BlockedTimePersistenceMapper {

    private final HallPersistenceMapper hallMapper;
    private final UserPersistenceMapper userMapper;

    public BlockedTimePersistenceMapper(HallPersistenceMapper hallMapper,
                                        UserPersistenceMapper userMapper) {
        this.hallMapper = hallMapper;
        this.userMapper = userMapper;
    }

    public BlockedTime toDomain(DBBlockedTime entity) {
        if (entity == null) {
            return null;
        }

        return new BlockedTime(
                entity.getId(),
                entity.getReason(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getType(),
                hallMapper.toDomain(entity.getHall()),
                userMapper.toDomain(entity.getCreatedBy()),
                userMapper.toDomain(entity.getUpdatedBy()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DBBlockedTime toEntity(BlockedTime domain) {
        if (domain == null) {
            return null;
        }

        DBBlockedTime entity = new DBBlockedTime();
        updateEntity(entity, domain);
        return entity;
    }

    public void updateEntity(DBBlockedTime entity, BlockedTime domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setId(domain.getId());
        entity.setReason(domain.getReason());
        entity.setStartAt(domain.getStartAt());
        entity.setEndAt(domain.getEndAt());
        entity.setType(domain.getType());
        entity.setHall(hallMapper.toEntity(domain.getHall()));
        entity.setCreatedBy(userMapper.toEntity(domain.getCreatedBy()));
        entity.setUpdatedBy(userMapper.toEntity(domain.getUpdatedBy()));
    }
}