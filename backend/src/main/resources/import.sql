-- Hallen
INSERT INTO halls (id, name, description, active, hall_type, created_at, updated_at)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'Gesamthalle',  'Die gesamte Halle', true, 'FULL',       now(), now()),
  ('00000000-0000-0000-0000-000000000002', 'Teilhalle A',  'Kleiner Hallenteil', true, 'PART_SMALL', now(), now()),
  ('00000000-0000-0000-0000-000000000003', 'Teilhalle B',  'Großer Hallenteil',  true, 'PART_LARGE', now(), now());

-- Admin-User  (Passwort: Admin1234)
INSERT INTO users (id, first_name, last_name, email, password_hash, role, active, created_at, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000010',
  'Admin', 'User', 'admin@hitzhofen.de',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'ADMIN', true, now(), now()
);

-- Vereinsvertreter  (Passwort: Verein1234)
INSERT INTO users (id, first_name, last_name, email, password_hash, role, active, created_at, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000011',
  'Max', 'Mustermann', 'max@verein.de',
  '$2a$10$TKh8H1.PfQ0A0bEzgGjb.e/YqhBqRTmM2XNJDIqEhpRcb0TjqvJlG',
  'CLUB_REPRESENTATIVE', true, now(), now()
);