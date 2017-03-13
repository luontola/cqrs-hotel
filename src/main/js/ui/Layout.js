// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import {Link} from "./Link";
import history from "../history";

// layout based on http://purecss.io/layouts/side-menu/

const MenuLink = ({to, title, divided = false}) => {
  let className = "pure-menu-item";
  if (divided) {
    className += " menu-item-divided";
  }
  if (history.location.pathname === to) {
    className += " pure-menu-selected";
  }
  return (
    <li className={className}>
      <Link to={to} className="pure-menu-link">{title}</Link>
    </li>
  );
};

class Layout extends React.Component {

  constructor(props) {
    super(props);
    this.state = {active: false};
    this.toggleMenu = () => {
      this.setState({active: !this.state.active});
    };
    this.maybeActive = (className) => {
      if (this.state.active) {
        if (className) {
          return className + ' active';
        } else {
          return 'active';
        }
      } else {
        return className;
      }
    };
  }

  render() {
    return (<div id="layout" className={this.maybeActive()}>

      {/* Menu toggle */}
      <a href="#menu" id="menuLink" className={this.maybeActive("menu-link")} onClick={this.toggleMenu}>
        {/* Hamburger icon */}
        <span/>
      </a>

      <div id="menu" className={this.maybeActive()}>
        <div className="pure-menu">
          <Link to="/" className="pure-menu-heading">CQRS Hotel</Link>

          <ul className="pure-menu-list">
            <MenuLink to="/" title="Home"/>
            <MenuLink to="/admin" title="Admin" divided/>
            <MenuLink to="/services" title="Services"/>
            <MenuLink to="/contact" title="Contact"/>
          </ul>
        </div>
      </div>

      <div id="main">
        <div className="header">
          <h1>CQRS Hotel</h1>
          <h2>Example application about CQRS and Event Sourcing</h2>
        </div>

        <div className="content">
          {this.props.children}
        </div>
      </div>
    </div>);
  }
}

export {Layout};
