window.SDcard = {
  createDir: function (pathname, dir, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "create directory", [pathname, dir]);
  },
  stats: function (filename, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "stats", [filename]);
  },
  createFile: function (pathname, file, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "create file", [pathname, file]);
  },
  exists: function (pathName, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "exists", [pathName]);
  },
  formatUri: function (pathName, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "format uri", [pathName]);
  },
  getPath: function (root, file, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "getpath", [root, filename]);
  },
  getStorageAccessPermission: function (uuid, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "storage permission", [uuid]);
  },
  listStorages: function (onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "list volumes", []);
  },
  listDir: function (src, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "list directory", [src]);
  },

  copy: function (root, src, dest, sub, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "copy", [root, src, dest, sub]);
  },
  move: function (root, src, dest, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "move", [srcPathname, destPathname]);
  },
  openDoc: function (onSuccess, onFail, mimeType) {
    cordova.exec(onSuccess, onFail, "SDcard", "open document", mimeType ? [mimeType] : []);
  },
  rename: function (root, file, newFile, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "rename", [root, file, newFile]);
  },
  write: function (root, file, content, onSuccess, onFail) {
    if (file) {
      cordova.exec(onSuccess, onFail, "SDcard", "write", [root, file, content]);
    } else {
      cordova.exec(onSuccess, onFail, "SDcard", "write", [root, content]);
    }
  },
  open: function (uuid, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "open", [uuid]);
  },
  list: function (onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "list", []);
  },
  syncFile: function (root, src, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "syncFile", [root, src]);
  },
  delete: function (root, file, onSuccess, onFail) {
    cordova.exec(onSuccess, onFail, "SDcard", "delete", [pathname]);
  },

};