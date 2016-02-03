import React from 'react';
import Dropzone from 'react-dropzone';
import TagImageEdit from '../TagEdit/formcomponents/TagImageEdit.react';

export default class SponsorEdit extends React.Component {

  constructor(props) {
    super(props);
  }

  removeImage() {
    this.props.onImageUpdated(undefined);
  }

  fileUploaded(e) {
    this.props.onImageUpdated(JSON.parse(e.target.response));
  }

  onDrop(files) {
    console.log('got files', files);
    var file = files[0];

    var oReq = new XMLHttpRequest();
    oReq.addEventListener("load", this.fileUploaded.bind(this));
    oReq.open("POST", "/support/uploadLogo/" + file.name);
    oReq.send(file);
  }

  render () {

    if (!this.props.logo) {
      return (
        <Dropzone onDrop={this.onDrop.bind(this)} multiple={false} >
          <div>Drop a logo here, or click to select file.</div>
        </Dropzone>
      );
    }

    const imageAsset = this.props.logo.assets[0];

    return (
      <div className="tag-edit__field">
        <img src={imageAsset.imageUrl} className="tag-edit__field__image"/>
        <div className="tag-edit__image__info">
          <div>Width: {imageAsset.width}px</div>
          <div>Height: {imageAsset.height}px</div>
          <div className="tag-edit__image__remove" onClick={this.removeImage.bind(this)}>
            <i className="i-cross-red" />Remove image
          </div>
        </div>
      </div>
    );
  }
}