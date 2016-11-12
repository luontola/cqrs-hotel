// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";

/*
 Based on http://purecss.io/layouts/side-menu/
 */
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
          <a className="pure-menu-heading" href="#">CQRS Hotel</a>

          <ul className="pure-menu-list">
            <li className="pure-menu-item"><a href="#" className="pure-menu-link">Home</a></li>
            <li className="pure-menu-item"><a href="#" className="pure-menu-link">About</a></li>

            <li className="pure-menu-item menu-item-divided pure-menu-selected">
              <a href="#" className="pure-menu-link">Services</a>
            </li>

            <li className="pure-menu-item"><a href="#" className="pure-menu-link">Contact</a></li>
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
