# Write/modify on external storage

Fork [this plguin](https://github.com/deadlyjack/code-editor/tree/master/plugins/cordova-plugin-sdcard) for using this plugin only

Using this plugin, cordova apps can check for external storages and write/modify files.

## Installation


## Usage

```js
window.SDcard = {
  open: function (uuid, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "open", [uuid]);
  },
  openDoc: function (onSuccess, onFail, mimeType) {
    cordova.exec(onSuccess, onFail, "SDcard", "open document", mimeType ? [mimeType] : []);
  },
  list: function (onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "list", []);
  },
  write: function (root, file, content, onSuccess, onFail) {
    if (file) {
      cordova.exec(onSuccess, onFail, "SDcard", "write", [root, file, content]);
    } else {
      cordova.exec(onSuccess, onFail, "SDcard", "write", [root, content]);
    }
  },
  rename: function (root, file, newFile, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "rename", [root, file, newFile]);
  },
  delete: function (root, file, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "delete", [root, file]);
  },
  mkdir: function (root, parent, dir, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "mkdir", [root, parent, dir]);
  },
  touch: function (root, parent, file, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "touch", [root, parent, file]);
  },
  move: function (root, src, dest, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "move", [root, src, dest]);
  },
  copy: function (root, src, dest, sub, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "copy", [root, src, dest, sub]);
  },
  getPath: function (root, file, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "getpath", [root, file]);
  }
};

```