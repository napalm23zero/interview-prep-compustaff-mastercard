-- Seed data mirrors the account table in CHALLENGE.md.
-- NULL in daily_limit / max_transaction means UNLIMITED, not zero.

INSERT INTO documents (id, document_type, document_id) VALUES
  (1, 'PASSPORT',               'PP-1001-AAA'),
  (2, 'PASSPORT',               'PP-1002-AAA'),
  (3, 'SOCIAL_SECURITY_NUMBER', 'SSN-1003-111'),
  (4, 'PASSPORT',               'PP-1004-AAA'),
  (5, 'PASSPORT',               'PP-1005-AAA'),
  (6, 'SOCIAL_SECURITY_NUMBER', 'SSN-1006-111'),
  (7, 'PASSPORT',               'PP-1007-AAA'),
  (8, 'PASSPORT',               'PP-1008-AAA'),
  (9, 'SOCIAL_SECURITY_NUMBER', 'SSN-1009-111'),
  (10, 'PASSPORT',              'PP-1010-AAA'),
  (11, 'PASSPORT',              'PP-1011-AAA');

INSERT INTO accounts (id, holder_name, status, currency, balance, held_amount, daily_limit, daily_spent, min_transaction, max_transaction) VALUES
  (1001, 'Luke Skywalker',    'ACTIVE',    'USD',       5000.00,    0.00, 10000.00,   0.00, 1.00, 2500.00),
  (1002, 'Leia Organa',       'ACTIVE',    'USD',        100.00,    0.00, 10000.00,   0.00, 1.00, 2500.00),
  (1003, 'Han Solo',          'SUSPENDED', 'USD',       5000.00,    0.00, 10000.00,   0.00, 1.00, 2500.00),
  (1004, 'Anakin Skywalker',  'ACTIVE',    'USD',       5000.00,    0.00, 10000.00,   0.00, 1.00, 2500.00),
  (1005, 'Obi-Wan Kenobi',    'ACTIVE',    'EUR',       5000.00,    0.00, 10000.00,   0.00, 1.00, 2500.00),
  (1006, 'Lando Calrissian',  'ACTIVE',    'USD',       5000.00,    0.00,  1000.00, 900.00, 1.00, 2500.00),
  (1007, 'Boba Fett',         'ACTIVE',    'USD',       5000.00, 4900.00, 10000.00,   0.00, 1.00, 2500.00),
  (1008, 'Mace Windu',        'ACTIVE',    'USD',         50.00,    0.00,  1000.00, 990.00, 1.00, 2500.00),
  (1009, 'Sheev Palpatine',   'CLOSED',    'EUR',         10.00,    0.00,   100.00,  99.00, 1.00, 2500.00),
  (1010, 'Jyn Erso',          'SUSPENDED', 'USD',       5000.00,    0.00, 10000.00,   0.00, 1.00, 2500.00),
  (1011, 'Ahsoka Tano',       'ACTIVE',    'USD',    1000000.00,    0.00,     NULL,   0.00, 1.00,    NULL);

INSERT INTO kyc (id, account_id, first_name, last_name, birth_date, document_id, verified) VALUES
  (1,  1001, 'Luke',   'Skywalker',   DATE '1988-03-12', 1,  TRUE),
  (2,  1002, 'Leia',   'Organa',      DATE '1991-07-04', 2,  TRUE),
  (3,  1003, 'Han',    'Solo',        DATE '1985-11-23', 3,  TRUE),
  (4,  1004, 'Anakin', 'Skywalker',   DATE '1994-01-09', 4,  FALSE),
  (5,  1005, 'Obi-Wan','Kenobi',      DATE '1990-05-30', 5,  TRUE),
  (6,  1006, 'Lando',  'Calrissian',  DATE '1982-09-17', 6,  TRUE),
  (7,  1007, 'Boba',   'Fett',        DATE '1996-12-02', 7,  TRUE),
  (8,  1008, 'Mace',   'Windu',       DATE '1979-06-25', 8,  FALSE),
  (9,  1009, 'Sheev',  'Palpatine',   DATE '1987-02-14', 9,  FALSE),
  (10, 1010, 'Jyn',    'Erso',        DATE '1993-08-08', 10, FALSE),
  (11, 1011, 'Ahsoka', 'Tano',        DATE '1986-04-19', 11, TRUE);

-- Seeded ids were assigned by hand; restart the identity sequences above them so
-- the next generated id does not collide with a row that already exists.
ALTER TABLE documents ALTER COLUMN id RESTART WITH 100;
ALTER TABLE accounts  ALTER COLUMN id RESTART WITH 2000;
ALTER TABLE kyc       ALTER COLUMN id RESTART WITH 100;
