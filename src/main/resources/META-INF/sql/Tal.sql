#getById
select tal_id, tal_last_sync, tal_public_key, tal_status, tal_name
  from tal
 where tal_id = ?;

#getAll
select tal_id, tal_last_sync, tal_public_key, tal_status, tal_name
  from tal
 order by tal_id;