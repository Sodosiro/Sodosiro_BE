-- PostgreSQL 최초 기동 시 1회 실행 (docker-entrypoint-initdb.d)
-- pgvector 확장 활성화 — spot_embedding 의 vector(1536) 컬럼에 필요
CREATE EXTENSION IF NOT EXISTS vector;
