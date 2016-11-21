CREATE TABLE event (
  stream_id UUID  NOT NULL,
  version   INT   NOT NULL CONSTRAINT positive_version CHECK (version > 0),
  data      JSONB NOT NULL,
  metadata  JSONB NOT NULL,
  PRIMARY KEY (stream_id, version)
);

CREATE INDEX event_idx
  ON event USING BTREE (stream_id, version, data, metadata);

CREATE TABLE stream (
  stream_id UUID NOT NULL,
  version   INT  NOT NULL,
  PRIMARY KEY (stream_id)
);
