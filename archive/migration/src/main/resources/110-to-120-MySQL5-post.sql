# 110-to-120-MySQL5-post.sql
# remove old data
drop table calendar_stamp;

# update server version
update server_properties SET propertyvalue='120' WHERE propertyname='cosmo.schemaVersion';