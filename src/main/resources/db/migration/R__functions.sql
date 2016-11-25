DROP FUNCTION IF EXISTS save_events( UUID, INT4, JSONB [], JSONB [] );

CREATE FUNCTION save_events(_stream_id        UUID,
                            _expected_version INT4,
                            _events_data      JSONB [],
                            _events_metadata  JSONB [])
  RETURNS INT8 AS $$
DECLARE
  _base_version  INT4;
  _version       INT4;
  _base_position INT8;
  _position      INT8;
  _event         RECORD;
BEGIN

  -- initialize stream

  SELECT version
  INTO _base_version
  FROM stream
  WHERE stream_id = _stream_id
  FOR UPDATE;

  IF _base_version IS NULL
  THEN
    INSERT INTO stream (stream_id, version)
    VALUES (_stream_id, 0)
    ON CONFLICT DO NOTHING;

    SELECT version
    INTO _base_version
    FROM stream
    WHERE stream_id = _stream_id
    FOR UPDATE;
  END IF;

  IF _base_version != _expected_version
  THEN
    RAISE EXCEPTION 'optimistic locking failure, current version is %', _base_version;
  END IF;

  -- append events to stream

  _version := _base_version;
  FOR _event IN SELECT *
                FROM unnest(_events_data, _events_metadata) AS t(data, metadata)
  LOOP
    _version := _version + 1;
    INSERT INTO event (stream_id, version, data, metadata)
    VALUES (_stream_id, _version, _event.data, _event.metadata);
  END LOOP;

  UPDATE stream
  SET version = _version
  WHERE stream_id = _stream_id;

  -- set the global order of the events

  LOCK TABLE event_sequence IN EXCLUSIVE MODE;

  SELECT count(*)
  INTO _base_position
  FROM event_sequence;

  _position := _base_position;
  FOR v IN _base_version + 1 .. _version LOOP
    _position := _position + 1;
    INSERT INTO event_sequence (position, stream_id, version)
    VALUES (_position, _stream_id, v);
  END LOOP;

  RETURN _position;

END;
$$ LANGUAGE plpgsql;
