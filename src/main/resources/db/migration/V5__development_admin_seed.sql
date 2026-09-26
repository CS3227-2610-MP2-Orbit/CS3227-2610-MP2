-- Development/demo account only. Do not use this credential in production.
INSERT INTO users (
    user_id,
    username,
    password_hash,
    role,
    active,
    created_at,
    updated_at
)
SELECT
    '00000000-0000-0000-0000-000000000001'::UUID,
    'admin',
    'pbkdf2_sha256$210000$zAXjYA1Gol/U+lxnvFKovQ$e5MGhLdLY1vglhNcqic+oONnkId0IAWqr7QNgkoDGhk',
    'VENUE_ADMINISTRATOR',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);
