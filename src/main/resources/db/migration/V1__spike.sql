CREATE FUNCTION spike(numbers INT [])
  RETURNS INT8 AS $$
DECLARE
  s INT8 := 0;
  x INT;
BEGIN
  FOREACH x IN ARRAY $1
  LOOP
    s := s + x;
  END LOOP;
  RETURN s;
END;
$$ LANGUAGE plpgsql;
