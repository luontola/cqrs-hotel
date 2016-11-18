CREATE TABLE foo (
  number INT  NOT NULL,
  string TEXT NOT NULL
);

CREATE TABLE event (
  stream_id UUID  NOT NULL,
  version   INT   NOT NULL CONSTRAINT positive_version CHECK (version > 0),
  data      JSONB NOT NULL,
  metadata  JSONB NOT NULL,
  PRIMARY KEY (stream_id, version)
);
