package com.foxdebug.sdcard;

import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.URLConnection;

import android.database.Cursor;
import android.net.Uri;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;

import android.os.Build;
import android.os.ParcelUuid;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import android.content.ContentResolver;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import android.text.TextUtils;
import android.provider.DocumentsContract;
import android.util.Log;

import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;

import org.apache.cordova.BuildConfig;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FilenameUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SDcard extends CordovaPlugin {

  private CallbackContext cb;
  private int mode;
  private int REQUEST_CODE;
  private int DOCUMENT_TREE;
  private int ACCESS_INTENT;
  private int OPEN_DOCUMENT;
  private StorageManager storageManager;
  private Context context;
  private Activity activity;
  private ContentResolver contentResolver;
  private DocumentFile originalRootFile;
  private String rootPath;
  private final String SAPERATOR = "::";
  private final String TAG = "Cordova Plugin SDCard";

  public SDcard() {

  }

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.ACCESS_INTENT = 6000;
    this.DOCUMENT_TREE = 6001;
    this.OPEN_DOCUMENT = 6002;
    this.REQUEST_CODE = this.ACCESS_INTENT;
    this.context = cordova.getContext();
    this.activity = cordova.getActivity();
    this.storageManager = (StorageManager) this.activity.getSystemService(Context.STORAGE_SERVICE);
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

    this.cb = callbackContext;

    if ("open".equals(action)) {

      if(args.length() == 0){
        this.cb.error("SDCardName required");
        return false;
      }

      Intent intent = null;

      // VERSION.SDK_INT >= 0x00000018 && VERSION.SDK_INT < 0x0000001d
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && android.os.Build.VERSION.SDK_INT <= 0x0000001c) {
        String SDcardUUID = args.getString(0);
        StorageVolume sdCard = null;

        for(StorageVolume volume: this.storageManager.getStorageVolumes()){
          String uuid = volume.getUuid();
          if(uuid != null && uuid.equals(SDcardUUID)){
            sdCard = volume;
          }
        }

        if (sdCard != null) {
          intent = sdCard.createAccessIntent(null);
        }
      }

      if (intent == null) {
        Uri uri = getExternalFilesDirUri(this.cordova.getContext());
        if (args.length() > 0) {
          // TODO: folder select
        }
        this.REQUEST_CODE = this.DOCUMENT_TREE;
        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (uri != null) {
          intent.putExtra("android.provider.extra.INITIAL_URI", uri);
        }
      }
      cordova.startActivityForResult(this, intent, this.REQUEST_CODE);
    }else if ("open document".equals(action)){
      Intent intent = new Intent();
      String mimeType = "*/*";
      intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      if(args.length() > 0) mimeType = args.getString(0);
      intent.setType(mimeType);
      cordova.startActivityForResult(this, intent, this.OPEN_DOCUMENT);
      return true;

    }else if("list".equals(action)){

      JSONObject volumes = new JSONObject();

      for(StorageVolume volume: this.storageManager.getStorageVolumes()){
        String name = volume.getDescription(this.context);
        String uuid = volume.getUuid();
        if(name != null && uuid != null){
          volumes.put(uuid, name);
        }
      }

      this.cb.success(volumes);
    }else if ("exists".equals(action)) {
      String pathname = args.getString(0);
      this.exists(formatUri(pathname));
    }else if("format uri".equals(action)) {
      String pathname = args.getString(0);
      this.cb.success(formatUri(pathname));
    }else if("create directory".equals(action)) {
      String pathname = args.getString(0);
      String dir = args.getString(1);
      this.createDir(pathname, dir);
    }else if("create file".equals(action)) {
      String pathname = args.getString(0);
      String filename = args.getString(1);
      this.createFile(pathname, filename);
    }else if("stats".equals(action)) {
      String filename = args.getString(0);
      this.getStats(filename);
    }else if("getpath".equals(action)) {
      String root = args.getString(0);
      String file = args.getString(1);
      this.getPath(root, file);
    }else if("storage permission".equals(action)) {
      String uuid = args.getString(0);
      this.getStorageAccess(uuid);
    }else if("list volumes".equals(action)) {
      this.getStorageVolumes();
    }else if("storage permission".equals(action)) {
      String fileName = args.getString(0);
      String content = args.getString(1);
      this.writeFile(formatUri(fileName), content);
    }else if("list directory".equals(action)) {
      String src = args.getString(0);
      if (src.contains(SAPERATOR)) {
        String splittedStr[] = src.split(SAPERATOR, 2);
        String srcPath = splittedStr[0];
        String parentDocId = splittedStr[1];
        this.listDir(srcPath, parentDocId);
      }
      else {
        this.listDir(src, null);
      }
    }
    else {
      if(args.length() < 2){
        this.error("Few paramerter are missing");
        return false;
      }

      this.rootPath = args.getString(0);
      String arg1 = args.getString(1);
      String arg2 = null;
      String arg3 = null;
      int argLen = args.length();

      if(argLen == 3){
        arg2 = args.getString(2);
      }
      if(argLen == 4) {
        arg3 = args.getString(3);
      }

      switch(action){
        case "rename":
          if(arg2 == null){
            this.error("Missing argument 'newname'");
            return false;
          }
          this.rename(arg1, arg2);
          break;
        case "delete":
          this.delete(arg1);
          break;
        case "syncFile":
          DocumentFile sdcardDir = DocumentFile.fromTreeUri(context, Uri.parse(arg1));
          boolean isSync = this.syncFile(sdcardDir);
          if (isSync) {
            this.cb.success("SUCCESS");
          }
          else {
            this.error("couldn't sync");
          }
          break;
        case "copy":
        case "move":
          if(arg2 == null){
            this.error("Missing argument 'destinationPath'");
            return false;
          }
          if(action.equals("move")) {
            this.move(arg1, arg2);
          }
          else {
            try {
              this.copy(arg1, arg2, arg3);
            } catch (IOException e) {
              e.printStackTrace();
              Log.d(TAG, e.toString());
            }
          }
          break;
        default:
          break;
      }
    }

    return true;

  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  public void getStorageVolumes() {
    try {
      JSONArray result = new JSONArray();
      for (StorageVolume volume : this.storageManager.getStorageVolumes()) {
        String name = volume.getDescription(this.context);
        String uuid = volume.getUuid();
        JSONObject volumeData = new JSONObject();
        if (name != null && uuid != null) {
          volumeData.put("uuid", uuid);
          volumeData.put("name", name);

          result.put(volumeData);
        }
      }

      this.cb.success(result);
    } catch (JSONException e) {
      this.error(e.toString());
    }
  }

  public void getStorageAccess(String SDCardUUID) {

    Intent intent = null;

    // VERSION.SDK_INT >= 0x00000018 && VERSION.SDK_INT < 0x0000001d

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
            && android.os.Build.VERSION.SDK_INT <= 0x0000001c) {

      StorageVolume sdCard = null;

      for (StorageVolume volume : this.storageManager.getStorageVolumes()) {
        String uuid = volume.getUuid();
        if (uuid != null && uuid.equals(SDCardUUID)) {
          sdCard = volume;
        }
      }

      if (sdCard != null) {
        intent = sdCard.createAccessIntent(null);
      }
    }

    if (intent == null) {
      this.REQUEST_CODE = this.DOCUMENT_TREE;
      intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    }

    cordova.startActivityForResult(this, intent, this.REQUEST_CODE);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data){

    super.onActivityResult(requestCode, resultCode, data);

    if(requestCode == this.OPEN_DOCUMENT){

      if(resultCode == Activity.RESULT_OK){

        try{

          Uri uri = data.getData();
          this.takePermission(uri);
          DocumentFile file = DocumentFile.fromSingleUri(this.context, uri);
          JSONObject res = new JSONObject();

          res.put("length", file.length());
          res.put("type", file.getType());
          res.put("filename", file.getName());
          res.put("canWrite", file.canWrite());
          res.put("uri", uri.toString());
          this.cb.success(res);

        }catch(JSONException e){

          this.error(e.toString());

        }

      }

    }else{

      if(requestCode == this.ACCESS_INTENT && resultCode == Activity.RESULT_CANCELED){
        this.error("Canceled");
        return;
      }

      Uri uri = data.getData();
      if(uri  == null){
        this.error("Empty uri");
      }else{

        this.takePermission(uri);
        DocumentFile file = DocumentFile.fromTreeUri(this.context, uri);
        if(file!=null && file.canWrite()){
          this.cb.success(uri.toString());
        }else{
          this.error("No write permisson: "+uri.toString());
        }

      }

    }

  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private String formatUri(String filename) {
    if (filename.contains(SAPERATOR)) {

      String splittedStr[] = filename.split(SAPERATOR, 2);
      String rootUri = splittedStr[0];
      String docId = splittedStr[1];

      Uri uri = getUri(rootUri, docId);

      return uri.toString();

    } else {

      return filename;

    }
  }


  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void createDir(String parent, String name) {
    create(parent, name, Document.MIME_TYPE_DIR);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void createFile(String parent, String name) {
    String mimeType = URLConnection.guessContentTypeFromName(name);
    String ext = FilenameUtils.getExtension(name);
    if (mimeType == null && ext != null)
      mimeType = "text/" + ext;
    else
      mimeType = "text/plain";

    create(parent, name, mimeType);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void create(String parent, String name, String mimeType) {

    try {

      String srcUri = null, docId = null;
      Uri parentUri = null;

      if (parent.contains(SAPERATOR)) {
        String splittedStr[] = parent.split(SAPERATOR, 2);
        srcUri = splittedStr[0];
        docId = splittedStr[1];
        parentUri = getUri(srcUri, docId);
      } else {
        srcUri = parent;
        parentUri = Uri.parse(srcUri);
        docId = DocumentsContract.getTreeDocumentId(parentUri);
        parentUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, docId);
      }

      ContentResolver contentResolver = this.context.getContentResolver();
      Uri newDocumentUri = DocumentsContract.createDocument(contentResolver, parentUri, mimeType, name);
      DocumentFile file = getFile(newDocumentUri);
      if (!name.equals(file.getName()))
        newDocumentUri = DocumentsContract.renameDocument(contentResolver, newDocumentUri, name);

      docId = DocumentsContract.getDocumentId(newDocumentUri);
      if (newDocumentUri != null)
        this.cb.success(srcUri + SAPERATOR + docId);
      else
        this.error("Unable to create " + parent);

    } catch (Exception e) {
      Log.d("SDcard create " + mimeType, "Unable to create", e);
      this.error(e.toString());
    }

  }

  private void writeFile(String filename, String content){
    final CallbackContext callback = this.cb;
    final Context context = this.context;

    cordova.getThreadPool().execute(new Runnable() {

      @Override
      public void run() {
        try {
          DocumentFile file = getFile(filename);

          if (file.canWrite()) {

            OutputStream op = context.getContentResolver().openOutputStream(file.getUri(), "rwt");
            PrintWriter pw = new PrintWriter(op, true);

            pw.print(content);
            pw.flush();
            pw.close();
            op.close();

            callback.success("OK");

          } else {
            callback.error("No write permission - " + filename);
          }
        } catch (IOException e) {
          callback.error(e.toString() + ": " + filename);
        }
      }
    });
  }


  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void rename(String filename, String newFile){
    try {

      String srcUri = null, docId = null;
      Uri fileUri = null;
      if (filename.contains(SAPERATOR)) {
        String splittedStr[] = filename.split(SAPERATOR, 2);
        srcUri = splittedStr[0];
        docId = splittedStr[1];
        fileUri = getUri(srcUri, docId);
      } else {
        srcUri = filename;
        fileUri = Uri.parse(filename);
      }

      ContentResolver contentResolver = this.context.getContentResolver();
      Uri renamedDocument = DocumentsContract.renameDocument(contentResolver, fileUri, newFile);
      docId = DocumentsContract.getDocumentId(renamedDocument);

      if (renamedDocument != null)
        this.cb.success(srcUri + SAPERATOR + docId);
      else
        this.error("Unable to rename " + filename);

    } catch (Exception e) {
      Log.d("SDcard rename", "Unable to rename", e);
      this.error(e.toString());
    }
  }

  private void delete(String filename) {
    ContentResolver contentResolver = context.getContentResolver();
    Uri fileUri = Uri.parse(filename);

    try {
      boolean fileDeleted = DocumentsContract.deleteDocument(contentResolver, fileUri);

      if (fileDeleted)
        this.cb.success(filename);
      else
        this.error("Unable to delete file " + filename);
    } catch (FileNotFoundException e) {
      this.error(e.toString());
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  private void move(String src, String dest){
    final ContentResolver contentResolver = this.context.getContentResolver();
    final String splittedStr[] = src.split(SAPERATOR, 2);
    final String rootUri = splittedStr[0];
    final String srcId = splittedStr[1];
    final String destId = dest.split(SAPERATOR, 2)[1];
    final CallbackContext callback = this.cb;

    cordova.getThreadPool().execute(new Runnable() {

      @Override
      public void run() {

        try {
          Uri newUri = copy(rootUri, srcId, destId);
          if (newUri == null)
            callback.error("Unable to copy " + src);
          else {
            DocumentsContract.deleteDocument(contentResolver, getUri(rootUri, srcId));
            callback.success(rootUri + SAPERATOR + DocumentsContract.getDocumentId(newUri));
          }
        } catch (Exception e) {
          callback.error(e.toString());
        }

      }
    });
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private Uri copy(String src, String dest, String subfolder) throws IOException {
    // src ファイルの取得
    File file = new File(Uri.parse(src.replace("file://", "")).getPath());
    if (!file.exists()) {
      this.error("Not found(350): " + src);
      return null;
    }
    DocumentFile srcfile = DocumentFile.fromFile(file);
    if(srcfile == null){
      this.error("Not found(350): " + src);
      return null;
    }

    // download folder がなければ作る
    DocumentFile destfile = DocumentFile.fromTreeUri(this.context, Uri.parse(dest));
    DocumentFile root = destfile.findFile("download");
    if (root == null) {
      root = destfile.createDirectory("download");
    }

    // download folder 直下に sub directory を作る
    DocumentFile subDir = root.findFile(subfolder);
    if (subfolder == null || subfolder == "null") {
      subDir = destfile;
    }
    else {
      if (subDir == null) {
        subDir = root.createDirectory(subfolder);
      }
    }
    destfile = subDir;
    if(destfile == null){
      this.error("指定された folder が存在しません " + destfile);
      return null;
    }
    // コピーの開始
    return this.copy(srcfile, destfile);
  }

  private boolean syncFile(DocumentFile path) {
    DocumentFile list[] = path.listFiles();
    try {
      for( int i=0; i< list.length; i++) {
        List<String> pathList = new ArrayList<String>();
        if (list[i].isFile()) {
          DocumentFile f = list[i];
          File target = new File(context.getExternalFilesDir("").getPath(), f.getName());
          InputStream is = context.getContentResolver().openInputStream(f.getUri());
          OutputStream os = context.getContentResolver().openOutputStream(Uri.fromFile(target));
          int DEFAULT_BUFFER_SIZE = 1024 * 4;
          byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
          int size = -1;
          int doing = 0;
          while (-1 != (size = is.read(buffer))) {
            os.write(buffer, 0, size);
          }
        }
      }
      return true;
    }
    catch (IOException e) {
      e.printStackTrace();
      Log.d(TAG, e.toString());
      return false;
    }
  }
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private Uri copy(DocumentFile src, DocumentFile dest) throws IOException {
    if(src.isFile()){
      Uri newUri = this.copyFile(src, dest);
      this.cb.success("SUCCESS");
      return newUri;
    }
    else {
      return null;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private Uri copyFile(DocumentFile src, DocumentFile dest) throws IOException, FileNotFoundException {

    ContentResolver contentResolver = this.context.getContentResolver();

    Uri newFileUri = DocumentsContract.createDocument(contentResolver, dest.getUri(), src.getType(), src.getName());
    DocumentFile newFile = getFile(newFileUri);
    InputStream is = contentResolver.openInputStream(src.getUri());
    OutputStream os = contentResolver.openOutputStream(newFile.getUri(), "rwt");

    if (is == null || os == null) {
      DocumentsContract.deleteDocument(contentResolver, newFileUri);
      return null;
    }

    IOUtils.copy(is, os);

    is.close();
    os.close();

    if (src.length() == newFile.length())
      return newFile.getUri();
    else {
      DocumentsContract.deleteDocument(contentResolver, newFileUri);
      return null;
    }

  }



  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void listDir(String src, String parentDocId) {
    Uri srcUri = Uri.parse(src);
    ContentResolver contentResolver = this.context.getContentResolver();

    if (parentDocId == null) {
      parentDocId = DocumentsContract.getTreeDocumentId(srcUri);
    }

    Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(srcUri, parentDocId);

    JSONArray result = new JSONArray();
    Cursor c = contentResolver.query(childrenUri,
            new String[] { Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE }, null,
            null, null);

    try {
      while (c.moveToNext()) {
        JSONObject fileData = new JSONObject();
        String docId = c.getString(0);
        String name = c.getString(1);
        String mime = c.getString(2);
        boolean isDirectory = isDirectory(mime);

        fileData.put("name", name);
        fileData.put("mime", mime);
        fileData.put("isDirectory", isDirectory);
        fileData.put("isFile", !isDirectory);
        fileData.put("uri", src + this.SAPERATOR + docId);
        result.put(fileData);
      }

      this.cb.success(result);
    } catch (JSONException e) {

      this.error(e.toString());

    } finally {
      if (c != null) {
        try {
          c.close();
        } catch (RuntimeException re) {
          throw re;
        } catch (Exception ignore) {
          // ignore exception
        }
      }
    }
  }

  private boolean isDirectory(String mimeType) {
    return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void getStats(String filename) {
    String fileUri = formatUri(filename);

    try {
      DocumentFile file = getFile(fileUri);

      JSONObject result = new JSONObject();
      result.put("exists", file.exists());
      result.put("canRead", file.canRead());
      result.put("canWrite", file.canWrite());
      result.put("name", file.getName());
      result.put("length", file.length());
      result.put("type", file.getType());
      result.put("isFile", file.isFile());
      result.put("isDirectory", file.isDirectory());
      result.put("isVirtual", file.isVirtual());
      result.put("lastModified", file.lastModified());

      this.cb.success(result);
    } catch (Exception e) {
      this.error(e.getMessage());
    }
  }

  private void getPath(String uriString, String src) {
    DocumentFile file = geRelativetDocumentFile(uriString, src);

    if (file == null) {

      this.error("Unable to get file");

    } else {

      Uri uri = file.getUri();
      String path = uri.getPath();

      if (path != null) {
        this.cb.success(uri.toString());
      } else {
        this.error("Unable to get path");
      }

    }
  }


  private void getPath(String src){
    DocumentFile file = this.getDocumentFile(src);

    if(file == null){

      this.error("Unable to get file");

    }else{

      Uri uri = file.getUri();
      String path = uri.getPath();

      if(path != null){
        this.cb.success(uri.toString());
      }else{
        this.error("Unable to get path");
      }

    }
  }

  private void error(String err) {
    this.cb.error("ERROR: " + err);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private Uri getUri(String src, String docId) {
    Uri srcUri = Uri.parse(src);
    String srcId = DocumentsContract.getTreeDocumentId(srcUri);
    srcUri = DocumentsContract.buildDocumentUriUsingTree(srcUri, srcId);
    return DocumentsContract.buildDocumentUriUsingTree(srcUri, docId);
  }

  private void exists(String path) {
    DocumentFile file = DocumentFile.fromSingleUri(this.context, Uri.parse(path));

    if (file == null) {
      this.error("Unable to get file");
    } else {

      if (file.exists()) {
        this.cb.success("TRUE");
      } else {
        this.cb.success("FALSE");
      }

    }

  }


  private DocumentFile getFile(Uri uri) {
    return getFile(uri.toString());
  }

  private DocumentFile getFile(String filePath) {
    Uri fileUri = Uri.parse(filePath);
    DocumentFile documentFile = null;

    if (filePath.matches("file:///(.*)")) {
      File file = new File(fileUri.getPath());
      documentFile = DocumentFile.fromFile(file);
    } else {
      documentFile = DocumentFile.fromSingleUri(this.context, Uri.parse(filePath));
    }

    return documentFile;
  }

  private DocumentFile getDocumentFile(String filename){
    return _DocumentFile(filename, 0);
  }

  private DocumentFile geRelativetDocumentFile(String uri, String filename) {

    List<String> paths = new ArrayList<String>();
    DocumentFile file = null;

    file = DocumentFile.fromTreeUri(this.context, Uri.parse(uri));
    if (!file.canWrite()) {
      this.error("No write permission");
      return null;
    }

    paths.addAll(Arrays.asList(filename.split("/")));

    while (paths.size() > 0) {
      String path = paths.remove(0);
      filename = TextUtils.join("/", paths);

      if (!path.equals("")) {

        file = file.findFile(path);

        if (file == null)
          return null;
      }
    }

    return file;

  }
  private DocumentFile getParentDocumentFile(String filename){
    return _DocumentFile(filename, 1);
  }

  private DocumentFile _DocumentFile(String filename, int limit){

    List<String> paths = new ArrayList<String>();
    DocumentFile file = null;

    file = DocumentFile.fromTreeUri(this.context, Uri.parse(this.rootPath));
    if(!file.canWrite()){
      this.error("No write permission");
      return null;
    }

    paths.addAll(Arrays.asList(filename.split("/")));

    while(paths.size() > limit){
      String path = paths.remove(0);
      filename = TextUtils.join("/", paths);

      if(!path.equals("")){

        file = file.findFile(path);

        if(file == null) return null;
      }
    }

    return file;

  }

  private void takePermission(Uri uri){
    this.contentResolver = this.context.getContentResolver();
    this.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
  }

  public static android.net.Uri getExternalFilesDirUri(Context context) {
    try {
      /**
       * Determine the app's private data folder on external storage if present.
       * e.g. "/storage/abcd-efgh/Android/com.nutomic.syncthinandroid/files"
       */
      ArrayList<File> externalFilesDir = new ArrayList<>();
      externalFilesDir.addAll(Arrays.asList(context.getExternalFilesDirs(null)));
      externalFilesDir.remove(context.getExternalFilesDir(null));
      if (externalFilesDir.size() == 0) {
        return null;
      }
      String absPath = externalFilesDir.get(0).getAbsolutePath();
      String[] segments = absPath.split("/");
      if (segments.length < 2) {
        return null;
      }
      // Extract the volumeId, e.g. "abcd-efgh"
      String volumeId = segments[2];
      // Build the content Uri for our private "files" folder.
      return android.net.Uri.parse(
              "content://com.android.externalstorage.documents/document/" +
                      volumeId + "%3AAndroid%2Fdata%2F" +
                      context.getPackageName() + "%2Ffiles");
    } catch (Exception e) {
      //  Log.w(TAG, "getExternalFilesDirUri exception", e);
    }
    return null;
  }
}
