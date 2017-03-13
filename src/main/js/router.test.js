// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import {describe, it} from "mocha";
import {expect} from "chai";
import {matchURI} from "./router";

describe("router", () => {
  describe("#matchURI", () => {
    it("match", () => {
      expect(matchURI('/foo', '/foo')).to.eql({});
    });
    it("mismatch", () => {
      expect(matchURI('/foo', '/bar')).to.eql(null);
    });
    it("path variable match", () => {
      expect(matchURI('/foo/:bar', '/foo/gazonk')).to.eql({bar: 'gazonk'});
    });
    it("numeric path variable match", () => {
      expect(matchURI('/foo/:id(\\d+)', '/foo/123')).to.eql({id: '123'});
    });
    it("numeric path variable mismatch", () => {
      expect(matchURI('/foo/:id(\\d+)', '/foo/bar')).to.eql(null);
    });
  });
});
