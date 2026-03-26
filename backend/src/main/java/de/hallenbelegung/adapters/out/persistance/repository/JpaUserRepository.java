package de.hallenbelegung.adapters.out.persistance.repository;

import de.hallenbelegung.adapters.out.persistance.entity.DBUser;
import de.hallenbelegung.adapters.out.persistance.mapper.UserPersistenceMapper;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaUserRepository implements UserRepositoryPort {

    @Inject
    EntityManager em;

    private final UserPersistenceMapper mapper = new UserPersistenceMapper();

    public JpaUserRepository() {
    }

    @Override
    public User save(User user) {
        DBUser entity = mapper.toEntity(user);
        DBUser merged = em.merge(entity);
        return mapper.toDomain(merged);
    }

    @Override
    public Optional<User> findById(UUID userId) {
        DBUser entity = em.find(DBUser.class, userId);
        return Optional.ofNullable(mapper.toDomain(entity));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        List<DBUser> list = em.createQuery("SELECT u FROM DBUser u WHERE u.email = :email", DBUser.class)
                .setParameter("email", email)
                .getResultList();
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(mapper.toDomain(list.get(0)));
    }

    @Override
    public List<User> findAll() {
        List<DBUser> list = em.createQuery("SELECT u FROM DBUser u", DBUser.class).getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<User> findAllActive() {
        List<DBUser> list = em.createQuery("SELECT u FROM DBUser u WHERE u.active = true", DBUser.class).getResultList();
        return list.stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
