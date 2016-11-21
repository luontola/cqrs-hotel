CREATE OR REPLACE FUNCTION save_events(_stream_id        UUID,
                                       _expected_version INT,
                                       _events_data      JSONB [],
                                       _events_metadata  JSONB [])
  RETURNS INT AS $$
DECLARE
  _current_version INT;
  _version         INT := _expected_version;
  _event           RECORD;
BEGIN

  SELECT version
  INTO _current_version
  FROM event
  WHERE event.stream_id = _stream_id
  ORDER BY version DESC
  LIMIT 1;

  IF _current_version != _expected_version
  THEN
    RAISE EXCEPTION 'optimistic locking failure, current version is %', _current_version;
  END IF;

  FOR _event IN SELECT *
                FROM unnest(_events_data, _events_metadata) AS t(data, metadata)
  LOOP
    _version := _version + 1;
    INSERT INTO event (stream_id, version, data, metadata)
    VALUES (_stream_id, _version, _event.data, _event.metadata);
  END LOOP;

  RETURN _version;

END;
$$ LANGUAGE plpgsql;
