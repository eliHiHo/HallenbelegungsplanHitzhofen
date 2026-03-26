INSERT INTO halls (id, active, created_at, description, hall_type, name, updated_at)
VALUES
  (
    '11111111-1111-1111-1111-111111111111',
    true,
    NOW(),
    'Kleine Halle / Halle A',
    'PART_SMALL',
    'Halle A',
    NOW()
  ),
  (
    '22222222-2222-2222-2222-222222222222',
    true,
    NOW(),
    'Große Halle / Halle B',
    'PART_LARGE',
    'Halle B',
    NOW()
  ),
  (
    '33333333-3333-3333-3333-333333333333',
    true,
    NOW(),
    'Gesamthalle',
    'FULL',
    'Gesamthalle',
    NOW()
  );