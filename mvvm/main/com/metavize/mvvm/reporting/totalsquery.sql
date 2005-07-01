select sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes), count(*)
from pl_stats
where
