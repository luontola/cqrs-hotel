CREATE TABLE event (
  stream_id UUID  NOT NULL,
  version   INT4  NOT NULL CONSTRAINT positive_version CHECK (version > 0),
  data      JSONB NOT NULL,
  metadata  JSONB NOT NULL,
  PRIMARY KEY (stream_id, version)
);

CREATE INDEX event_idx
  ON event USING BTREE (stream_id, version, data, metadata);

CREATE TABLE stream (
  stream_id UUID NOT NULL,
  version   INT4 NOT NULL,
  PRIMARY KEY (stream_id)
);

CREATE TABLE event_sequence (
  position  INT8 NOT NULL,
  stream_id UUID NOT NULL,
  version   INT4 NOT NULL,
  PRIMARY KEY (position),
  FOREIGN KEY (stream_id, version) REFERENCES event (stream_id, version)
);

CREATE INDEX event_sequence_idx
  ON event_sequence USING BTREE (position, stream_id, version);
