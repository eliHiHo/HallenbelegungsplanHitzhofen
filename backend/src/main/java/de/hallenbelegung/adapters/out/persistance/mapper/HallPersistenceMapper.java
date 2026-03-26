package de.hallenbelegung.adapters.out.persistance.mapper;

import de.hallenbelegung.adapters.out.persistance.entity.DBHall;
import de.hallenbelegung.application.domain.model.Hall;

public class HallPersistenceMapper {

    public Hall toDomain(DBHall entity) {
        if (entity == null) {
            return null;
        }

        return new Hall(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getHallType()
        );
    }

    public DBHall toEntity(Hall domain) {
        if (domain == null) {
            return null;
        }

        DBHall entity = new DBHall();
        updateEntity(entity, domain);
        return entity;
    }

    public void updateEntity(DBHall entity, Hall domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setActive(domain.isActive());
        entity.setHallType(domain.getHallType());
    }
}