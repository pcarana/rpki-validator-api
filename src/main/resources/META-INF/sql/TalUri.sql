#getByTalId
select tau_id, tal_id, tau_value, tau_loaded_cer, tau_loaded
  from tal_uri
 where tal_id = ?
 order by tau_id;