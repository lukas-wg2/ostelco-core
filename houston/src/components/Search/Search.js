import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import { subscriberActions } from '../../actions/subscriber.actions';
import SearchForm from './SearchForm';
import SearchResults from './SearchResults';
import AlertMessage from './Alert';

class Search extends React.Component {

  onSubmit = (text) => {
    this.props.getSubscriberAndBundlesByEmail(text);
  }

  render() {
    const hasResults = this.props.profile.name || false;
    return (
      <div className="container">
        <AlertMessage />
        <SearchForm onSubmit={this.onSubmit} />
        <br />
        {
          hasResults && (
            <SearchResults />
          )
        }
      </div>
    );
  }
}

Search.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
  profile: PropTypes.object,
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  const { subscriber } = state;
  return {
    loggedIn,
    profile: subscriber
  };
};

const mapDispatchToProps = {
  getSubscriberAndBundlesByEmail: subscriberActions.getSubscriberAndBundlesByEmail
};
export default connect(mapStateToProps, mapDispatchToProps)(Search);