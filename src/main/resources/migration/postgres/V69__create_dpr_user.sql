CREATE ROLE ${dpr_user} WITH LOGIN PASSWORD '${dpr_password}';

GRANT rds_superuser TO ${dpr_user};
GRANT rds_replication TO ${dpr_user};
