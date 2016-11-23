DROP FUNCTION save_events( UUID, INTEGER, JSONB [], JSONB [] );

CREATE OR REPLACE FUNCTION save_events(_stream_id        UUID,
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

  SELECT version
  INTO _base_version
  FROM event
  WHERE event.stream_id = _stream_id
  ORDER BY version DESC
  LIMIT 1;

  IF _base_version IS NULL
  THEN
    _base_version := 0;
  END IF;

  IF _base_version != _expected_version
  THEN
    RAISE EXCEPTION 'optimistic locking failure, current version is %', _base_version;
  END IF;

  _version := _base_version;
  FOR _event IN SELECT *
                FROM unnest(_events_data, _events_metadata) AS t(data, metadata)
  LOOP
    _version := _version + 1;
    INSERT INTO event (stream_id, version, data, metadata)
    VALUES (_stream_id, _version, _event.data, _event.metadata);
  END LOOP;

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
