-- 扩大数据源 database_name 列(JDBC URL 可超过 64 字符)
ALTER TABLE temporedata_guard_datasource MODIFY COLUMN database_name VARCHAR(512);
