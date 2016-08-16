import React from 'react';
import SponsorLogo from './SponsorLogo.react';
import {PAID_HOSTEDCONTENT_TYPE} from '../../constants/paidContentTagTypes';

export default class SponsorEdit extends React.Component {

  constructor(props) {
    super(props);
  }

  updateName(e) {
    this.props.updateSponsorship(Object.assign({}, this.props.sponsorship, {
      sponsorName: e.target.value
    }));
  }

  updateLogo(image) {
    this.props.updateSponsorship(Object.assign({}, this.props.sponsorship, {
      sponsorLogo: image
    }));
  }

  updateLink(e) {
    this.props.updateSponsorship(Object.assign({}, this.props.sponsorship, {
      sponsorLink: e.target.value
    }));
  }

  updateAboutLink(e) {
    this.props.updateSponsorship(Object.assign({}, this.props.sponsorship, {
      aboutLink: e.target.value
    }));
  }

  render () {

    if (!this.props.sponsorship) {
      return false;
    }

    const logoWidth = this.props.paidContentTagType === PAID_HOSTEDCONTENT_TYPE.value ? false : 140;
    const logoHeight = this.props.paidContentTagType === PAID_HOSTEDCONTENT_TYPE.value ? false : 90;

    return (
      <div className="tag-edit__input-group">
        <label className="tag-edit__input-group__header">Sponsor</label>
        <div className="tag-edit__field">
          <label className="tag-edit__label">Name</label>
          <input type="text" className="tag-edit__input" value={this.props.sponsorship.sponsorName || ""} onChange={this.updateName.bind(this)}/>
        </div>

        <div className="tag-edit__field">
          <label className="tag-edit__label">Logo</label>
          <SponsorLogo logo={this.props.sponsorship.sponsorLogo} onImageUpdated={this.updateLogo.bind(this)} requiredWidth={logoWidth} requiredHeight={logoHeight}/>
        </div>

        <div className="tag-edit__field">
          <label className="tag-edit__label">Link</label>
          <input type="text" className="tag-edit__input" value={this.props.sponsorship.sponsorLink || ""} onChange={this.updateLink.bind(this)}/>
        </div>

        <div className="tag-edit__field">
          <label className="tag-edit__label">About this content Link</label>
          <input type="text" className="tag-edit__input" value={this.props.sponsorship.aboutLink || ""} onChange={this.updateAboutLink.bind(this)}/>
        </div>
      </div>
    );

  }
}
