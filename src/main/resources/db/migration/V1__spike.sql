CREATE FUNCTION spike(numbers INT [])
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

CREATE TABLE foo (
  number INT  NOT NULL,
  string TEXT NOT NULL
);

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
