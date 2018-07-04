#getByTalId
select taf_id, tal_id, taf_file_type, taf_status, taf_message, taf_location
  from tal_file
 where tal_id = ?
 order by taf_id;