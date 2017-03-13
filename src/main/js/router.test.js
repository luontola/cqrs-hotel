// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import {describe, it} from "mocha";
import {expect} from "chai";
import router, {matchURI} from "./router";

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

  describe("#resolve", () => {

    it("find the first matching route", () => {
      const routes = [
        {path: '/one', action: () => 'page one'},
        {path: '/two', action: () => 'page two'},
        {path: '/three', action: () => 'page three'},
      ];
      const context = {pathname: '/two'};

      return router.resolve(routes, context)
        .then(page => expect(page).to.eq('page two'));
    });

    it("gives an error if no route matches", () => {
      const routes = [
        {path: '/one', action: () => 'page one'},
        {path: '/two', action: () => 'page two'},
        {path: '/three', action: () => 'page three'},
      ];
      const context = {pathname: '/garbage'};

      return router.resolve(routes, context)
        .then(page => {
          expect.fail(page, null, "did not expect a match");
        }, error => {
          expect(error).to.be.instanceOf(Error);
          expect(error).to.have.property('message', 'Not found');
          expect(error).to.have.property('status', 404);
        });
    });

    it("informs the route about the current location/context", () => {
      const routes = [
        {path: '/foo', action: (context) => context},
      ];
      const context = {pathname: '/foo', bar: 123};

      return router.resolve(routes, context)
        .then(page => {
          expect(page).to.eql({pathname: '/foo', bar: 123, params: {}});
        });
    });

    it("informs the route about path variables", () => {
      const routes = [
        {path: '/foo/:bar', action: (context) => context},
      ];
      const context = {pathname: '/foo/123'};

      return router.resolve(routes, context)
        .then(page => {
          expect(page).to.eql({pathname: '/foo/123', params: {bar: '123'}});
        });
    });

    it("returns error page if context has an error", () => {
      const routes = [
        {path: '/normal', action: () => 'normal page'},
        {path: '/error', action: () => 'error page'},
      ];
      const context = {pathname: '/normal', error: {}};

      return router.resolve(routes, context)
        .then(page => {
          expect(page).to.eql('error page');
        });
    });

    it("supports asynchronous routes", () => {
      const routes = [
        {
          path: '/foo',
          async action() {
            const data = await Promise.resolve('foo');
            return 'async page ' + data;
          }
        },
      ];
      const context = {pathname: '/foo'};

      return router.resolve(routes, context)
        .then(page => {
          expect(page).to.eql('async page foo');
        });
    })
  });
});
