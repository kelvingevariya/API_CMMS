INSERT INTO currency (id, name, code)
VALUES (1, 'Euro', '€'),
       (2, 'Dollar', '$'),
       (3, 'Dirham', 'DH')
ON CONFLICT
    (id)
DO UPDATE SET name = excluded.name;
