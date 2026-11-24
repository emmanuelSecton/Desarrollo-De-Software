-- V1: baseline. Habilita las extensiones que usan los changes siguientes.
-- btree_gist: exclusion constraint de solapamiento de turnos (C-10).
-- citext: emails y busquedas sin distinguir mayusculas.
-- pgcrypto: gen_random_uuid() y utilidades criptograficas.
-- Las tres son extensiones "trusted" desde PostgreSQL 13: no requieren superusuario.
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
