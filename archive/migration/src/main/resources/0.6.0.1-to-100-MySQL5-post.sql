# 0.6.0.1-to-0.6.1-MySQL5-post.sql
# remove old data
alter table item drop foreign key FK317B137014CFFB;
alter table item drop index FK317B137014CFFB;
alter table item drop column parentid;

alter table stamp drop column isactive;
alter table item drop index idx_itemisactive;
alter table item drop column isactive;

# update server version
update server_properties SET propertyvalue='100' WHERE propertyname='cosmo.schemaVersion';