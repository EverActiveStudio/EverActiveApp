INSERT INTO users (id, email, name, password, role)
VALUES (1, 'test@everactive.pl', 'Test User', '{bcrypt}$2a$10$jp21f02i/pDdP81AGT3gku3bYiZU.ClMAoSruQY0rN1lPbcYiTCVa', 'User')
ON CONFLICT (id) DO NOTHING
;
