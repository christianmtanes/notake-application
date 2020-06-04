package com.tanxe.buggoff.notake.Notebook;

import android.Manifest;
import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.tanxe.buggoff.notake.CaptureTable.Capture;
import com.tanxe.buggoff.notake.CaptureTable.CaptureRoomDatabase;
import com.tanxe.buggoff.notake.CaptureTable.GridCaptureAdapter;
import com.tanxe.buggoff.notake.NotebookTable.Notebook;
import com.tanxe.buggoff.notake.R;
import com.tanxe.buggoff.notake.SearchActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.CreateNamedRangeRequest;
import com.google.api.services.docs.v1.model.Dimension;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertInlineImageRequest;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.ParagraphElement;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.Size;
import com.google.api.services.docs.v1.model.StructuralElement;
import com.google.api.services.docs.v1.model.TableCell;
import com.google.api.services.docs.v1.model.TableRow;
import com.google.api.services.docs.v1.model.TextRun;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.scanlibrary.ScanActivity;
import com.scanlibrary.ScanConstants;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Environment.*;

public class NotebookActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 99;
    private final String TAG = "NotebookActivity";
    private static final int RC_RECOVERABLE = 9002;
    private final String MY_NOTEBOOK = "my notebook";
    private static final String SCOPES = "https://www.googleapis.com/auth/documents";
    private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file";
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private Toolbar mToolbar;
    private String RevisionID;
    private Notebook myNotebook;
    private NotebookActivity context;
    private GridCaptureAdapter gridCaptureAdapter;
    private SwipeRefreshLayout mySwipeRefreshLayout;
    private CaptureRoomDatabase local_db;

    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
    private final String CAMERA_PERMISSION = android.Manifest.permission.CAMERA;
    private final String WRITE_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private final String READ_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;
    private ArrayList<Capture> myCaptures = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notebook);

        context = this;
        Intent intent = context.getIntent();
        myNotebook = intent.getExtras().getParcelable(MY_NOTEBOOK);//our notebook id
        setBottomNavView();
        setUpGrid();
        setLocalDB();

        TextView textViewNBName = (TextView) findViewById(R.id.my_notebook);
        textViewNBName.setText(myNotebook.getName());

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        }

        mToolbar = (Toolbar) findViewById(R.id.back_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(null);
        onSwipeActivity();
        Toast toast = Toast.makeText(context, "View-Only Mode, Press on capture to edit!", Toast.LENGTH_LONG);
//        toast.setGravity(Gravity.BOVTTOM|Gravity.CENTER_VERTICAL, 0 ,0);
//        View view = toast.getView();
//        view.setBackgroundColor(context.getColor(R.color.blue_2));
        toast.show();
    }

    private void setLocalDB() {
        local_db = CaptureRoomDatabase.getDatabase(this);
        local_db.getCapturesByID(myNotebook.getId()).observe(this, new Observer<List<Capture>>() {
            @Override
            public void onChanged(@Nullable final List<Capture> captures) {
                gridCaptureAdapter.clear();
                new loadFromFirebase().execute();
                gridCaptureAdapter.addAll(captures);
                gridCaptureAdapter.notifyDataSetChanged();
                Log.d(TAG, "on changeeeed else");
            }
        });
    }


    /*
     * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
     * performs a swipe-to-refresh gesture.
     */
    public void onSwipeActivity() {
        mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        final NotebookActivity tmp = this;
        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.i(TAG, "onRefresh called from SwipeRefreshLayout");
                        local_db.getCapturesByID(myNotebook.getId()).observe(context, new Observer<List<Capture>>() {
                            @Override
                            public void onChanged(@Nullable final List<Capture> captures) {
                                if (captures.isEmpty()) {
                                    Log.d(TAG, "on changeeeed if");
                                    new loadFromFirebase().execute();
                                } else {
                                    gridCaptureAdapter.clear();
                                    gridCaptureAdapter.addAll(captures);
                                    gridCaptureAdapter.notifyDataSetChanged();
                                    Log.d(TAG, "on changeeeed else");
                                }
                            }
                        });
                        new CheckForFileUpdates(tmp, myNotebook.getDocID()).execute(myNotebook.getAccount());
                    }
                }
        );
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notebook_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.doc_file:
//                new getDocLink(this, myNotebook.getDocID()).execute(myNotebook.getAccount());
                viewDoc("https://docs.google.com/document/d/" + myNotebook.getDocID() + "/edit?usp=sharing");
                break;
            case R.id.pdf_file:
                new DownloadPdf(this, myNotebook.getDocID()).execute(myNotebook.getAccount());
                break;

            case R.id.search:
                startSearchActivity();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startSearchActivity() {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra(MY_NOTEBOOK, myNotebook);
        startActivity(intent);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Toast.makeText(getApplicationContext(), "openCv successfully loaded", Toast.LENGTH_SHORT).show();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private List<String> checkAndRequestPermissions(Context context) {

        int camera = ContextCompat.checkSelfPermission(context, CAMERA_PERMISSION);
        int readStorage = ContextCompat.checkSelfPermission(context, READ_STORAGE_PERMISSION);
        int writeStorage = ContextCompat.checkSelfPermission(context, WRITE_STORAGE_PERMISSION);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (camera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(CAMERA_PERMISSION);
        }
        if (readStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(READ_STORAGE_PERMISSION);
        }
        if (writeStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(WRITE_STORAGE_PERMISSION);
        }
        return listPermissionsNeeded;
    }

    private boolean permissions(List<String> listPermissionsNeeded) {

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), 1);
            return false;
        }
        return true;
    }

    public void onBackClick(View view) {
        finish();
    }

    /**
     * BottomNavigationView setup.
     */
    private void setBottomNavView() {
        Log.d(TAG, "setup bottomAddView");
        BottomNavigationViewEx bottomNavigationViewEx = (BottomNavigationViewEx) findViewById(R.id.bottomNavViewBar);
        bottomNavigationViewEx.enableItemShiftingMode(false);
        bottomNavigationViewEx.enableShiftingMode(false);
        bottomNavigationViewEx.setTextVisibility(false);
        bottomNavigationViewEx.enableAnimation(false);

        bottomNavigationViewEx.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                List<String> permissionList = checkAndRequestPermissions(context);

                if (permissions(permissionList)) {
                    startScan(ScanConstants.OPEN_CAMERA); // call your camera instead of this method
                }
                return false;
            }
        });
    }

    protected void startScan(int preference) {

        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, preference);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                getContentResolver().delete(uri, null, null);
                Date date = Calendar.getInstance().getTime();
                Capture capture = new Capture(myNotebook.getId(), myNotebook.getName(),
                        "", date, myNotebook.getAccount(), myNotebook.getDocID());
                uploadImage(bitmap, capture);
                local_db.insertCapture(capture);
                new InsertImage(this, myNotebook.getDocID(), capture).execute(myNotebook.getAccount());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setUpGrid() {

        GridView gridView = findViewById(R.id.gridViewNB);
        gridCaptureAdapter = new GridCaptureAdapter(context, R.layout.layout_grid_capture_view, "", myCaptures, myNotebook);
        gridView.setAdapter(gridCaptureAdapter);
    }

    private Map<String, Object> toMap(Capture capture) {
        Log.d(TAG, "map capture");
        HashMap<String, Object> captureMap = new HashMap<>();
        captureMap.put("ID", capture.getId());
        captureMap.put("comments", capture.getComments());
        captureMap.put("tags", capture.getTags());
        captureMap.put("date", capture.getDate());
        captureMap.put("imagePath", capture.getImagePath());
        captureMap.put("notebookID", capture.getNotebookID());
        captureMap.put("docID", capture.getDocID());
        captureMap.put("account", capture.getAccount());
        captureMap.put("imageID", capture.getImageID());
        captureMap.put("notebookName", capture.getNotebookName());

        return captureMap;
    }

    private String uploadImage(Bitmap bitmap, Capture capture) {
        Log.d(TAG, "upload image");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

        String pathname = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS) + "/" + myNotebook.getName() + capture.getId() + ".jpg";

        java.io.File f = new java.io.File(pathname);
        try {
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            fo.close();
            Log.d(TAG, "Image uploading success");
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "image uploading failed", e);
        }
        capture.setImagePath(pathname);
        return pathname;
    }

    private class AddCaptureToFirebase extends AsyncTask<Capture, Void, Void> {
        @Override
        protected Void doInBackground(Capture... captures) {
            Log.d(TAG, "add capture to firebase");
            HashMap<String, Object> captureMap = (HashMap<String, Object>) toMap(captures[0]);

            mFirestore.collection("users")
                    .document(String.valueOf(myNotebook.getAccount()))
                    .collection("notebooks").document(myNotebook.getId())
                    .collection("Captures").document(captures[0].getId())
                    .set(captureMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Write failed
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
            return null;
        }
    }

    private class InsertImage extends AsyncTask<Account, Void, String> {

        private WeakReference<NotebookActivity> mActivityRef;
        private String mFilePath;
        private String mDocID;
        private Capture capture;

        public InsertImage(NotebookActivity activity, String docID, Capture capture) {
            mActivityRef = new WeakReference<>(activity);
            mDocID = docID;
            mFilePath = capture.getImagePath();
            this.capture = capture;
            System.out.println("TEST OF UPLOAD: \n");
        }

        @Override
        protected String doInBackground(Account... accounts) {
            if (mActivityRef.get() == null) {
                return null;
            }

            Context context = mActivityRef.get().getApplicationContext();
            return uploadImage(context, accounts[0]);
        }

        @NonNull
        private String uploadImage(Context context, Account account) {
            String fileId = "";
            System.out.println("TEST OF UPLOAD: \n");

            try {
                System.out.println("TEST OF UPLOAD: \n");

                //Image Upload.
                File fileMetadata = new File();
                fileMetadata.setName("photo.jpg");
                String path = mFilePath;
                java.io.File filePath = new java.io.File(path);
                FileContent mediaContent = new FileContent("image/jpeg", filePath);
                Drive service2 = getDriveService(context, account);
                File file = service2.files().create(fileMetadata, mediaContent)
                        .setFields("id").execute();
                fileId = file.getId();
                setSharable(fileId, service2, file);

//                System.out.println(file.getPermissions() + " File ID: " + file.getId() + "link: " + file.getWebViewLink() + "link2: " + file.getWebContentLink());

                Docs service = getDocsService(context, account);
                if (mDocID == null) {
                    while (mDocID == null) {
                        mDocID = myNotebook.getDocID();
                    }
                    capture.setDocID(mDocID);
                    local_db.updateDocID(capture.getId(), mDocID);
                }
                String doc_id2 = mDocID;

                Document doc2read = service.documents().get(doc_id2).execute();
                int max_num = readStructrualElements(doc2read.getBody().getContent());
                System.out.println("Max Line :- " + max_num);
                SetUploadImageRequests(fileId, service, doc_id2, max_num);
                RevisionID = service.documents().get(doc_id2).execute().getRevisionId();
            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    mActivityRef.get().onRecoverableAuthException(recoverableException);
                }
            } catch (IOException e) {
                Log.w(TAG, "getContacts:exception", e);
            }

            return fileId;
        }

        private void setSharable(String fileId, Drive service2, File file) throws IOException {
            JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
                @Override
                public void onFailure(GoogleJsonError e,
                                      HttpHeaders responseHeaders) {
                    // Handle error
                    System.err.println(e.getMessage());
                }

                @Override
                public void onSuccess(Permission permission,
                                      HttpHeaders responseHeaders) {
                    System.out.println("Permission ID: " + permission.getId());
                }
            };
            file.setShared(Boolean.TRUE);

            BatchRequest batch = service2.batch();
            Permission userPermission = new Permission()
                    .setType("anyone")
                    .setRole("writer");
            service2.permissions().create(fileId, userPermission)
                    .setFields("id")
                    .queue(batch, callback);

            Permission domainPermission = new Permission()
                    .setType("anyone")
                    .setRole("reader");
            service2.permissions().create(fileId, domainPermission)
                    .setFields("id")
                    .queue(batch, callback);

            batch.execute();
        }

        @Override
        protected void onPostExecute(String fileId) {
            super.onPostExecute(fileId);
            if (mActivityRef.get() != null) {
                mActivityRef.get().onImageLoadFinished(fileId, capture);
            }
        }
    }

    private void SetUploadImageRequests(String fileId, Docs service, String doc_id2, int max_num) throws IOException {
        List<Request> requests = new ArrayList<>();
//                String newPage = TextUtils.join("", Collections.nCopies((45 - (max_num % 45)), "\n"));
        String newPage = "\n\n\n";
        System.out.println("RES:" + newPage);

        String urlLink = "https://drive.google.com/a/mail.huji.ac.il/uc?authuser=0&id=" + fileId + "&export=download";
        requests.add(new Request().setInsertInlineImage(new InsertInlineImageRequest()
                .setUri(urlLink)
                .setLocation(new Location().setIndex(max_num - 1))
                .setObjectSize(new Size()
                        .setHeight(new Dimension()
                                .setMagnitude(300.0)
                                .setUnit("PT"))
                        .setWidth(new Dimension()
                                .setMagnitude(150.0)
                                .setUnit("PT")))));
        String newText = "\n<Comments_" + fileId + "/>\n INSERT COMMENT HERE.\n<Comments_End>";
        requests.add(new Request()
                .setInsertText(
                        new InsertTextRequest()
                                .setText(newText)
                                .setLocation(new Location().setIndex(max_num))));
        requests.add(
                new Request()
                        .setCreateNamedRange(
                                new CreateNamedRangeRequest()
                                        .setName(fileId)
                                        .setRange(
                                                new Range()
                                                        .setStartIndex(max_num)
                                                        .setEndIndex(max_num + newText.length()))));
//        requests.add(new Request().setUpdateTextStyle(new UpdateTextStyleRequest()
//                .setTextStyle(new TextStyle()
//                        .setBold(true)
//                        .setItalic(true))
//                .setRange(new Range()
//                        .setStartIndex(max_num)
//                        .setEndIndex(max_num + newText.length()))
//                .setFields("bold")));
//
//        requests.add(new Request().setInsertText(
//                new InsertTextRequest()
//                        .setText(newPage)
//                        .setLocation(new Location().setIndex(max_num + newText.length()))));
        BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
        BatchUpdateDocumentResponse response = service.documents()
                .batchUpdate(doc_id2, body).execute();
    }

    private class CheckForFileUpdates extends AsyncTask<Account, Void, String> {

        private WeakReference<NotebookActivity> mActivityRef;
        private String mFilePath;
        private String mDocID;
        private Capture capture;

        public CheckForFileUpdates(NotebookActivity activity, String docID) {
            mActivityRef = new WeakReference<>(activity);
            mDocID = docID;
            System.out.println("CheckForFileUpdates ASYNC \n");
        }

        @Override
        protected String doInBackground(Account... accounts) {
            if (mActivityRef.get() == null) {
                return null;
            }

            Context context = mActivityRef.get().getApplicationContext();
            return checkForUpdates(context, accounts[0]);
        }

        @NonNull
        private String checkForUpdates(Context context, Account account) {
            String revision = "";
            try {
                Docs service = getDocsService(context, account);
                String doc_id2 = mDocID;
                revision = service.documents().get(doc_id2).execute().getRevisionId();
            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    mActivityRef.get().onRecoverableAuthException(recoverableException);
                }
            } catch (IOException e) {
                Log.w(TAG, "getContacts:exception", e);
            }

            return revision;
        }


        @Override
        protected void onPostExecute(String revision) {
            super.onPostExecute(revision);
            if (mActivityRef.get() != null) {
                mActivityRef.get().onRevisionCheckFinished(revision);
            }
        }
    }

    private class DownloadPdf extends AsyncTask<Account, Void, String> {

        private WeakReference<NotebookActivity> mActivityRef;
        private String mDocID;

        public DownloadPdf(NotebookActivity activity, String docID) {
            mActivityRef = new WeakReference<>(activity);
            mDocID = docID;
            System.out.println("downloadPDF ASYNC \n");
        }

        @Override
        protected String doInBackground(Account... accounts) {
            if (mActivityRef.get() == null) {
                return null;
            }

            Context context = mActivityRef.get().getApplicationContext();
            return createPDF(context, accounts[0]);
        }

        @NonNull
        private String createPDF(Context context, Account account) {
            String pathname = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS) + "/" + myNotebook.getName() + mDocID + ".pdf";
            java.io.File file = new java.io.File(pathname);

            try {
                Drive service = getDriveService(context, account);

                OutputStream out = new FileOutputStream(file);

                service.files().export(mDocID, "application/pdf")
                        .executeMediaAndDownloadTo(out);
//                Toast.makeText(context, file.getPath(), Toast.LENGTH_SHORT).show();

            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    mActivityRef.get().onRecoverableAuthException(recoverableException);
                }
            } catch (IOException e) {
                Log.w(TAG, "pdf download :exception", e);
            }

            return pathname;
        }


        @Override
        protected void onPostExecute(String file) {
            super.onPostExecute(file);
            if (mActivityRef.get() != null) {
                mActivityRef.get().onCreatePdfFinish(file);
            }
        }
    }

//    private class DownloadDoc extends AsyncTask<Account, Void, String> {
//
//        private WeakReference<NotebookActivity> mActivityRef;
//        private String mDocID;
//
//        public DownloadDoc(NotebookActivity activity, String docID) {
//            mActivityRef = new WeakReference<>(activity);
//            mDocID = docID;
//            System.out.println("downloadPDF ASYNC \n");
//        }
//
//        @Override
//        protected String doInBackground(Account... accounts) {
//            if (mActivityRef.get() == null) {
//                return null;
//            }
//
//            Context context = mActivityRef.get().getApplicationContext();
//            return DownloadDoc(context, accounts[0]);
//        }
//
//        @NonNull
//        private String DownloadDoc(Context context, Account account) {
//            String url = "https://docs.google.com/document/d/"+ mDocID+"/edit?usp=sharing";
//
//            try {
//                url = "https://docs.google.com/document/d/"+ mDocID+"/edit?usp=sharing";
//
////                Drive service = getDriveService(context, account);
////
////                OutputStream out = new FileOutputStream(file);
////
////                service.files().get(mDocID)
////                        .executeMediaAndDownloadTo(out);
////                FileOutputStream fos = new FileOutputStream(file);
////                Toast.makeText(context, file.getPath(), Toast.LENGTH_SHORT).show();
//            } catch (UserRecoverableAuthIOException recoverableException) {
//                if (mActivityRef.get() != null) {
//                    mActivityRef.get().onRecoverableAuthException(recoverableException);
//                }
//            } catch (IOException e) {
//                Log.w(TAG, "doc download :exception", e);
//            }
//
//            return url;
//        }
//
//
//        @Override
//        protected void onPostExecute(String file) {
//            super.onPostExecute(file);
//            if (mActivityRef.get() != null) {
//                mActivityRef.get().viewDoc(file);
//            }
//        }
//    }

    public class MAGFileProvider extends FileProvider {
        //we extend fileprovider to stop collision with chatbot library file provider
        //this class is empty and used in manifest
    }

    private void viewPdf(String pdfFile) {
//        if(Build.VERSION.SDK_INT>=24){
//            try{
//                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
//                m.invoke(null);
//            }catch(Exception e){
//                e.printStackTrace();
//            }
//        }
        java.io.File file = new java.io.File(pdfFile);
        if (file.exists() == true) {

//            Uri uri = Uri.fromFile(file);
            Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", file);

            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pdfIntent.setDataAndType(uri, "application/pdf");
            pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pdfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(pdfIntent);
        }
    }

    private void viewDoc(String docFile) {

//        Uri path = Uri.fromFile(pdfFile);

        // Setting the intent for pdf reader
//        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
//        pdfIntent.setPackage("com.google.android.apps.docs");
//        pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

//        java.io.File file = new java.io.File(pdfFile);
//        if (file.exists() == true) {

//            Uri uri = Uri.fromFile(file);
//            Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", file);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(docFile));
        startActivity(i);
//        } catch (ActivityNotFoundException e) {
//            Toast.makeText(this, "Can't read doc file", Toast.LENGTH_SHORT).show();
//        }
//        }
    }

    private class getDocLink extends AsyncTask<Account, Void, String> {

        private WeakReference<NotebookActivity> mActivityRef;
        private String mDocID;

        public getDocLink(NotebookActivity activity, String docID) {
            mActivityRef = new WeakReference<>(activity);
            mDocID = docID;
            System.out.println("viewable link ASYNC \n");
        }

        @Override
        protected String doInBackground(Account... accounts) {
            if (mActivityRef.get() == null) {
                return null;
            }

            Context context = mActivityRef.get().getApplicationContext();
            return getViewLink(context, accounts[0]);
        }

        @NonNull
        private String getViewLink(Context context, Account account) {
            String link = "";
            try {

                Drive service = getDriveService(context, account);
                link = service.files().get(mDocID).execute().getWebViewLink();
                setClipboard(context, link);
                Toast.makeText(context, "Link Copied into copyboard!", Toast.LENGTH_SHORT).show();
            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    mActivityRef.get().onRecoverableAuthException(recoverableException);
                }
            } catch (IOException e) {
                Log.w(TAG, "pdf download :exception", e);
            }

            return link;
        }


        @Override
        protected void onPostExecute(String file) {
            super.onPostExecute(file);
            if (mActivityRef.get() != null) {
                mActivityRef.get().onLinkFinished(file);
            }
        }
    }

    private void onLinkFinished(String file) {
    }

    private void onCreatePdfFinish(String file) {
        System.out.println("ONCREATE PDF FINISH");
        viewPdf(file);
    }

    private void setClipboard(Context context, String text) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    @NonNull
    private Docs getDocsService(Context context, Account account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(SCOPES));
        credential.setSelectedAccount(account);
        return new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("Create Doc in Drive :)")
                .build();
    }

    protected void onRecoverableAuthException(UserRecoverableAuthIOException recoverableException) {
        Log.w(TAG, "onRecoverableAuthException", recoverableException);
        startActivityForResult(recoverableException.getIntent(), RC_RECOVERABLE);
    }

    private Drive getDriveService(Context context, Account account) {
        GoogleAccountCredential credential2 = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DRIVE_SCOPE));
        credential2.setSelectedAccount(account);
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential2)
                .setApplicationName("Upload File")
                .build();
    }


    protected void onImageLoadFinished(@Nullable String imageID, Capture capture) {
//        hideProgressDialog();
        capture.setImageID(imageID);
        local_db.updateImageID(capture.getId(), imageID);
        local_db.updateImageID(capture.getId(), imageID);
        new AddCaptureToFirebase().execute(capture);
    }

    protected void onRevisionCheckFinished(@Nullable String revision) {
//        hideProgressDialog();
        //Abort if there are no changes.
        if (RevisionID == revision) {
            return;
        }
        Log.w(TAG, "There Are changes in doc!");
        new getCommentUpdates(this, myNotebook.getDocID(), myCaptures).execute(myNotebook.getAccount());
    }

    private int readStructrualElements(List<StructuralElement> elements) {
        int maxNum = 1;
        StringBuilder sb = new StringBuilder();
        for (StructuralElement element : elements) {
            if (element.getParagraph() != null) {
                for (ParagraphElement paragraphElement : element.getParagraph().getElements()) {
                    sb.append(readParagraphElement(paragraphElement));
                    maxNum = Math.max(maxNum, paragraphElement.getEndIndex());
                }
            } else if (element.getTable() != null) {
                // The text in table cells are in nested Structural Elements and tables may be
                // nested.
                for (TableRow row : element.getTable().getTableRows()) {
                    for (TableCell cell : row.getTableCells()) {
                        sb.append(readStructrualElements(cell.getContent()));
                        maxNum = Math.max(maxNum, element.getEndIndex());
                    }
                }
            } else if (element.getTableOfContents() != null) {
                // The text in the TOC is also in a Structural Element.
                sb.append(readStructrualElements(element.getTableOfContents().getContent()));
                maxNum = Math.max(maxNum, element.getEndIndex());
            }
        }
//        return sb.toString();
        return maxNum;
    }

    private static String readParagraphElement(ParagraphElement element) {
        TextRun run = element.getTextRun();
        if (run == null || run.getContent() == null) {
            // The TextRun can be null if there is an inline object.
            return "";
        }
        return run.getContent();
    }

    private class loadFromFirebase extends AsyncTask<Notebook, Void, Void> {
        @Override
        protected Void doInBackground(final Notebook... notebooks) {
            Log.d(TAG, "load data from the firebase");
            mFirestore.collection("users")
                    .document(String.valueOf(myNotebook.getAccount()))
                    .collection("notebooks").document(myNotebook.getId())
                    .collection("Captures")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w(TAG, "Listen failed.", e);
                                return;
                            }
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                if (doc.exists()) {
                                    HashMap hashMap = (HashMap) doc.get("account");
                                    Account account = new Account((String) hashMap.get("name"), (String) hashMap.get("type"));
                                    Capture capture = new Capture(doc.getString("ID"),
                                            doc.getString("notebookID"),
                                            doc.getString("notebookName"),
                                            doc.getString("comments"),
                                            doc.getString("imagePath"),
                                            (ArrayList<String>) doc.get("tags"),
                                            doc.getDate("date"), account,
                                            doc.getString("docID"),
                                            doc.getString("imageID"));
                                    local_db.insertCapture(capture);
                                }
                            }
                        }
                    });
            return null;
        }
    }

    private class getCommentUpdates extends AsyncTask<Account, Void, ArrayList<String>> {

        private WeakReference<NotebookActivity> mActivityRef;
        private String mDocID;
        private ArrayList<Capture> myCaptures;

        public getCommentUpdates(NotebookActivity activity, String docID, ArrayList<Capture> myCaptures) {
            mActivityRef = new WeakReference<>(activity);
            mDocID = docID;
            this.myCaptures = myCaptures;
            System.out.println("getCommentUpdates ASYNC \n");
        }

        @Override
        protected ArrayList<String> doInBackground(Account... accounts) {
            if (mActivityRef.get() == null) {
                return null;
            }

            Context context = mActivityRef.get().getApplicationContext();
            return getCommentUpdates(context, accounts[0]);
        }

        @NonNull
        private ArrayList<String> getCommentUpdates(Context context, Account account) {
            ArrayList<String> texts = null;
            try {
                Docs service = getDocsService(context, account);
                while (mDocID == null) {
                    mDocID = myNotebook.getDocID();
                }
                String doc_id2 = mDocID;
                ArrayList<String> IDS = new ArrayList<String>();
                System.out.println("1 getCommentUpdates ASYNC \n");

                for (Capture img : myCaptures) {
                    IDS.add(img.getImageID());
                }
                System.out.println("2 getCommentUpdates ASYNC \n");

                texts = GetTextforID(service, doc_id2, IDS);
            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    mActivityRef.get().onRecoverableAuthException(recoverableException);
                }
            } catch (IOException e) {
                Log.w(TAG, "getContacts:exception", e);
            }

            return texts;
        }


        @Override
        protected void onPostExecute(ArrayList<String> revisionMessages) {
            super.onPostExecute(revisionMessages);
            if (mActivityRef.get() != null) {
                mActivityRef.get().replaceText(revisionMessages);
            }
        }
    }

    private void replaceText(ArrayList<String> revisionMessages) {
        for (int index = 0; index < myCaptures.size(); index++) {
            Capture capture = myCaptures.get(index);
            System.out.println("BEFORE: " + capture.getComments());
//            capture.setComments(revisionMessages.get(index));
            String arg = revisionMessages.get(index);
            local_db.updateNotes(capture.getId(), arg);
            capture.setComments(arg);
            System.out.println("BEFORE: " + capture.getComments() + "." + arg);
            mySwipeRefreshLayout.setRefreshing(false);
        }
    }

    private ArrayList<String> GetTextforID(Docs service, String documentId, ArrayList<String> rangeNames)
            throws IOException {
        // Fetch the document to determine the current indexes of the named ranges.
        Document document = service.documents().get(documentId).execute();
        ArrayList<String> results = new ArrayList<String>();
        String body = GetTextInRange(document.getBody().getContent());
        String result;
        for (String rangeName : rangeNames) {
            result = getText(body, rangeName); //GetTextInRange(document.getBody().getContent());
            System.out.println("in ReplaceTExtinRange: " + result);
            results.add(result);
//            // Find the matching named ranges.
//            NamedRanges namedRangeList = document.getNamedRanges().get(rangeName);
//            if (namedRangeList == null) {
//                throw new IllegalArgumentException("The named range is no longer present in the document.");
//            }
//            // Determine all the ranges of text to be removed, and at which indexes the replacement text
//            // should be inserted.
//            List<Range> allRanges = new ArrayList<>();
//            for (NamedRange namedRange : namedRangeList.getNamedRanges()) {
//                System.out.println(namedRange.getRanges());
//                allRanges.addAll(namedRange.getRanges());
//            }
//
////        // Sort the list of ranges by startIndex, in descending order.
////        allRanges.sort(Comparator.comparing(Range::getStartIndex).reversed());
//            String result = "";
//            // Create a sequence of requests for each range.
//            for (Range range : allRanges) {
//
//            }
        }
        System.out.println("3 getCommentUpdates ASYNC \n");

        return results;
    }

    private static String GetTextInRange(List<StructuralElement> elements) {
        StringBuilder sb = new StringBuilder();
        for (StructuralElement element : elements) {
            if (element.getParagraph() != null) {
                for (ParagraphElement paragraphElement : element.getParagraph().getElements()) {
                    sb.append(readParagraphElement(paragraphElement));
                }
            } else if (element.getTable() != null) {
                // The text in table cells are in nested Structural Elements and tables may be
                // nested.
                for (TableRow row : element.getTable().getTableRows()) {
                    for (TableCell cell : row.getTableCells()) {
                        sb.append(GetTextInRange(cell.getContent()));
                    }
                }
            } else if (element.getTableOfContents() != null) {
                // The text in the TOC is also in a Structural Element.
                sb.append(GetTextInRange(element.getTableOfContents().getContent()));
            }
        }
        return sb.toString();
    }

    private String getText(String txt, String fileId) {
        String fromS = "\n<Comments_" + fileId + "/>";
        String toS = "<Comments_End>";

        txt = txt.substring(txt.indexOf(fromS) + 1 + fromS.length());
        txt = txt.substring(0, txt.indexOf(toS));
        return txt;
    }
}