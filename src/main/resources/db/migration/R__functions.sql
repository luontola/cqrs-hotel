CREATE OR REPLACE FUNCTION spike(numbers INT [])
  RETURNS INT8 AS $$
DECLARE
  s INT8 := 0;
  x INT;
BEGIN
  FOREACH x IN ARRAY $1
  LOOP
    RAISE NOTICE 'number = %', x;
    s := s + x;
  END LOOP;
  RETURN s;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION spike2(numbers INT [], strings TEXT [])
  RETURNS INT8 AS $$
DECLARE
  sum  INT8 := 0;
  pair RECORD;
BEGIN
  FOR pair IN SELECT *
              FROM unnest($1, $2) AS t(number, string)
  LOOP
    RAISE NOTICE 'number = %, string = %', pair.number, pair.string;
    sum := sum + pair.number;
    INSERT INTO foo (number, string) VALUES (pair.number, pair.string);
  END LOOP;
  RETURN sum;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION save_events(stream_id        UUID,
                                       expected_version INT,
                                       data             JSONB [],
                                       metadata         JSONB [])
  RETURNS INT AS $$
DECLARE
  v INT := 0;
  e RECORD;
BEGIN
  RAISE NOTICE 'stream_id = %, expected_version = %', stream_id, expected_version;
  FOR e IN SELECT *
           FROM unnest($3, $4) AS t(data, metadata)
  LOOP
    v := v + 1;
    RAISE NOTICE 'version = %, data = %, metadata = %', v, e.data, e.metadata;
    INSERT INTO event (stream_id, version, data, metadata) VALUES (stream_id, v, e.data, e.metadata);
  END LOOP;
  RETURN v;
END;
$$ LANGUAGE plpgsql;
