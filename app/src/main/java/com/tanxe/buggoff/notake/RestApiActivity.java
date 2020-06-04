package com.tanxe.buggoff.notake;

import android.accounts.Account;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tanxe.buggoff.notake.Home.HomeActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import com.google.api.services.docs.v1.model.DeleteContentRangeRequest;
import com.google.api.services.docs.v1.model.Dimension;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertInlineImageRequest;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.NamedRange;
import com.google.api.services.docs.v1.model.NamedRanges;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.Size;
import com.google.api.services.docs.v1.model.TextStyle;
import com.google.api.services.docs.v1.model.UpdateTextStyleRequest;
import com.google.api.services.docs.v1.model.WriteControl;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.people.v1.model.Person;

import com.google.api.services.docs.v1.model.ParagraphElement;
import com.google.api.services.docs.v1.model.StructuralElement;
import com.google.api.services.docs.v1.model.TableCell;
import com.google.api.services.docs.v1.model.TableRow;
import com.google.api.services.docs.v1.model.TextRun;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.api.services.drive.Drive;

/**
 * Activity to demonstrate using the Google Sign In API with a Google API that uses the Google
 */
public class RestApiActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = "RestApiActivity";

    // Scope for reading user's contacts
    private static final String CONTACTS_SCOPE = "https://www.googleapis.com/auth/contacts.readonly";

    // Bundle key for account object
    private static final String KEY_ACCOUNT = "key_account";
    private static final String SCOPES = "https://www.googleapis.com/auth/documents";
    private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file";

    private final String ACCOUNT = "Account";
    private final String GOOGLE_SIGNIN_CLIENT = "googleSignInClient";

    // Request codes
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_RECOVERABLE = 9002;

    // Global instance of the HTTP transport
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    // Global instance of the JSON factory
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static int isBeg = 0;
    private GoogleSignInClient mGoogleSignInClient;

    private Account mAccount;

    private TextView mStatusTextView;
    private TextView mDetailTextView;
    private ProgressDialog mProgressDialog;
    private String ImageID;
    private String DocID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);

        // Views
        mStatusTextView = findViewById(R.id.status);
        mDetailTextView = findViewById(R.id.detail);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
        // For this example we don't need the disconnect button
        findViewById(R.id.disconnect_button).setVisibility(View.GONE);

        // Restore instance state
        if (savedInstanceState != null) {
            mAccount = savedInstanceState.getParcelable(KEY_ACCOUNT);
        }

        // Configure sign-in to request the user's ID, email address, basic profile,
        // and readonly access to contacts.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(CONTACTS_SCOPE))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        // Show a standard Google Sign In button. If your application does not rely on Google Sign
        // In for authentication you could replace this with a "Get Google Contacts" button
        // or similar.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        if (isBeg == 0) {
            isBeg = 1;

            userSignIn();
        }
    }

    private void userSignIn() {
        // Check if the user is already signed in and all required scopes are granted
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (GoogleSignIn.hasPermissions(account, new Scope(SCOPES))) {
            updateUI(account);
            mAccount = account.getAccount();
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra(ACCOUNT, mAccount);
            startActivity(intent);
            System.out.println("Exit Intent !!!!!!!!!!!!!!!!");

            findViewById(R.id.disconnect_button).setVisibility(View.VISIBLE);

        } else {
            updateUI(null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_ACCOUNT, mAccount);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }

        // Handling a user-recoverable auth exception
        if (requestCode == RC_RECOVERABLE) {
            if (resultCode == RESULT_OK) {
//                getContacts();
            } else {
                Toast.makeText(this, R.string.msg_contacts_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    private void continueToApp() {
        userSignIn();
    }

    private void signOut() {
        // Signing out clears the current authentication state and resets the default user,
        // this should be used to "switch users" without fully un-linking the user's google
        // account from your application.
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                updateUI(null);
            }
        });
    }

    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        Log.d(TAG, "handleSignInResult:" + completedTask.isSuccessful());

        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            updateUI(account);

            // Store the account from the result
            mAccount = account.getAccount();

            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra(ACCOUNT, mAccount);
            findViewById(R.id.disconnect_button).setVisibility(View.VISIBLE);
            startActivity(intent);

            // Asynchronously access the People API for the account
//            getContacts();
        } catch (ApiException e) {
            Log.w(TAG, "handleSignInResult:error", e);

            // Clear the local account
            mAccount = null;

            // Signed out, show unauthenticated UI.
            updateUI(null);
        }
    }

    private void getContacts() {
        if (mAccount == null) {
            Log.w(TAG, "getContacts: null account");
            return;
        }

        showProgressDialog();
//        new DoDocFile(this).execute(mAccount);
        String docid = "1znhO_ls98oWf3WM7EFU29s1--JFHqj8O9DanRph7KW0";

        String ImageId = "1n1_fraVKrCPajfD_f-D8al6yeim7seki";
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Image.jpeg";


//        AsyncTask<Account, Void, String> execute = new InsertImage(this, docid, path);
//        execute.execute(mAccount);
//        AsyncTask<Account, Void, String> execute2 = new replaceText(this, docid, ImageId, "4444");
//        execute2.execute(mAccount);
//        AsyncTask<Account, Void, String> execute3 = new CreateDocFile(this, "AYA_THE_IMBACILE2");
//        execute3.execute(mAccount);

////        System.out.println(execute.getStatus());
////        while(execute.getStatus() != AsyncTask.Status.FINISHED){}
//            System.out.println("Finished:            " + ImageID);
//        replaceNamedRange(service, doc_id2, fileId, "Maybe");
    }

    protected void onConnectionsLoadFinished(@Nullable List<Person> connections) {
        hideProgressDialog();

        if (connections == null) {
            Log.d(TAG, "getContacts:connections: null");
            mDetailTextView.setText(getString(R.string.connections_fmt, "None"));
            return;
        }

        Log.d(TAG, "getContacts:connections: size=" + connections.size());

        // Get names of all connections
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < connections.size(); i++) {
            Person person = connections.get(i);
            if (person.getNames() != null && person.getNames().size() > 0) {
                msg.append(person.getNames().get(0).getDisplayName());

                if (i < connections.size() - 1) {
                    msg.append(",");
                }
            }
        }

        // Display names
        mDetailTextView.setText(getString(R.string.connections_fmt, msg.toString()));
    }

    protected void onImageLoadFinished(@Nullable String imageID) {
        hideProgressDialog();
        ImageID = imageID;
    }

    protected void onDocLoadFinished(@Nullable String imageID) {
        hideProgressDialog();
        DocID = imageID;
    }


    protected void onRecoverableAuthException(UserRecoverableAuthIOException recoverableException) {
        Log.w(TAG, "onRecoverableAuthException", recoverableException);
        startActivityForResult(recoverableException.getIntent(), RC_RECOVERABLE);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                findViewById(R.id.disconnect_button).setVisibility(View.GONE);
                break;

            case R.id.disconnect_button:
                continueToApp();
                break;
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            //uploadImage(R.string.loading)
            mProgressDialog.setMessage("Bullying Google Servers to Submit to our will, please wait");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private static String readParagraphElement(ParagraphElement element) {
        TextRun run = element.getTextRun();
        if (run == null || run.getContent() == null) {
            // The TextRun can be null if there is an inline object.
            return "";
        }
        return run.getContent();
    }

    private static int readStructrualElements(List<StructuralElement> elements) {
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

    private static String GetTextInRange(List<StructuralElement> elements, Range range) {
        StringBuilder sb = new StringBuilder();
        for (StructuralElement element : elements) {
            if (element.getParagraph() != null) {
                if (element.getStartIndex() - 1 == range.getStartIndex() && element.getEndIndex() - 1 == range.getEndIndex()) {
                    for (ParagraphElement paragraphElement : element.getParagraph().getElements()) {
                        sb.append(readParagraphElement(paragraphElement));
                    }
                    break;
                }
            }
        }
        return sb.toString();
    }

    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            mStatusTextView.setText(getString(R.string.signed_in_fmt, account.getDisplayName()));

            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);
            mDetailTextView.setText(null);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    //    public Task<GoogleDriveFileHolder> uploadFile(final java.io.File localFile, final String mimeType, @Nullable final String folderId) {
//        return Tasks.call(mExecutor, new Callable<GoogleDriveFileHolder>() {
//            @Override
//            public GoogleDriveFileHolder call() throws Exception {
//                // Retrieve the metadata as a File object.
//
//                List<String> root;
//                if (folderId == null) {
//                    root = Collections.singletonList("root");
//                } else {
//
//                    root = Collections.singletonList(folderId);
//                }
//
//                File metadata = new File()
//                        .setParents(root)
//                        .setMimeType(mimeType)
//                        .setName(localFile.getName());
//
//                FileContent fileContent = new FileContent(mimeType, localFile);
//
//                File fileMeta = mDriveService.files().create(metadata, fileContent).execute();
//                GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
//                googleDriveFileHolder.setId(fileMeta.getId());
//                googleDriveFileHolder.setName(fileMeta.getName());
//                return googleDriveFileHolder;
//            }
//        });
//    }
    private static class CreateDocFile extends AsyncTask<Account, Void, String> {

        private WeakReference<RestApiActivity> mActivityRef;
        private String title;

        public CreateDocFile(RestApiActivity activity, String title) {
            mActivityRef = new WeakReference<>(activity);
            this.title = title;
        }

        @Override
        protected String doInBackground(Account... accounts) {
            if (mActivityRef.get() == null) {
                return null;
            }
            String doc_id = "";
            Context context = mActivityRef.get().getApplicationContext();
            try {

                Docs service = getDocsService(context, accounts[0]);
                Document doc = new Document()
                        .setTitle(title);
                doc = service.documents().create(doc)
                        .execute();
                doc_id = doc.getDocumentId();
            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    mActivityRef.get().onRecoverableAuthException(recoverableException);
                }
            } catch (IOException e) {
                Log.w(TAG, "getContacts:exception", e);
            }

            return doc_id;
        }

        @Override
        protected void onPostExecute(String doc_id) {
            super.onPostExecute(doc_id);
            if (mActivityRef.get() != null) {
                mActivityRef.get().onDocLoadFinished(doc_id);
            }
        }
    }

    private static class replaceText extends AsyncTask<Account, Void, String> {

        private WeakReference<RestApiActivity> mActivityRef;
        private String docID;
        private String imageID;
        private String replaceWith;

        public replaceText(RestApiActivity activity, String docID, String imageID, String replaceWith) {
            mActivityRef = new WeakReference<>(activity);
            this.docID = docID;
            this.imageID = imageID;
            this.replaceWith = replaceWith;
        }

        @Override
        protected String doInBackground(Account... accounts) {
            if (mActivityRef.get() == null) {
                return null;
            }
            String doc_id = "";
            Context context = mActivityRef.get().getApplicationContext();
            try {

                replaceNamedRange(getDocsService(context, accounts[0]), this.docID, this.imageID, replaceWith);
            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    mActivityRef.get().onRecoverableAuthException(recoverableException);
                }
            } catch (IOException e) {
                Log.w(TAG, "getContacts:exception", e);
            }

            return doc_id;
        }

        @Override
        protected void onPostExecute(String doc_id) {
            super.onPostExecute(doc_id);
            if (mActivityRef.get() != null) {
                mActivityRef.get().onDocLoadFinished(doc_id);
            }
        }
    }

//    private static class InsertImage extends AsyncTask<Account, Void, String> {
//
//        private WeakReference<RestApiActivity> mActivityRef;
//        private String mFilePath;
//        private String mDocID;
//
//        public InsertImage(RestApiActivity activity, String docID, String filePath) {
//            mActivityRef = new WeakReference<>(activity);
//            mDocID = docID;
//            mFilePath = filePath;
//            System.out.println("TEST OF UPLOAD: \n");
//        }
//
//        @Override
//        protected String doInBackground(Account... accounts) {
//            if (mActivityRef.get() == null) {
//                return null;
//            }
//
//            Context context = mActivityRef.get().getApplicationContext();
//            return uploadImage(context, accounts[0]);
//        }
//
//        @NonNull
//        private String uploadImage(Context context, Account account) {
//            String fileId = "";
//            System.out.println("TEST OF UPLOAD: \n");
//
//            try {
//                System.out.println("TEST OF UPLOAD: \n");
//
//                //Image Upload.
//
//                File fileMetadata = new File();
//                fileMetadata.setName("photo.jpg");
//                String path = mFilePath;
//                java.io.File filePath = new java.io.File(path);
//                FileContent mediaContent = new FileContent("image/jpeg", filePath);
//                Drive service2 = getDriveService(context, account);
//                File file = service2.files().create(fileMetadata, mediaContent)
//                        .setFields("id")
//                        .execute();
//                fileId = file.getId();
//
//
//                JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
//                    @Override
//                    public void onFailure(GoogleJsonError e,
//                                          HttpHeaders responseHeaders) {
//                        // Handle error
//                        System.err.println(e.getMessage());
//                    }
//
//                    @Override
//                    public void onSuccess(Permission permission,
//                                          HttpHeaders responseHeaders) {
//                        System.out.println("Permission ID: " + permission.getId());
//                    }
//                };
//                file.setShared(Boolean.TRUE);
//
//                BatchRequest batch = service2.batch();
//                Permission userPermission = new Permission()
//                        .setType("anyone")
//                        .setRole("writer");
//                service2.permissions().create(fileId, userPermission)
//                        .setFields("id")
//                        .queue(batch, callback);
//
//                Permission domainPermission = new Permission()
//                        .setType("anyone")
//                        .setRole("reader");
//                service2.permissions().create(fileId, domainPermission)
//                        .setFields("id")
//                        .queue(batch, callback);
//
//                batch.execute();
//                System.out.println(file.getPermissions() + " File ID: " + file.getId() + "link: " + file.getWebViewLink() + "link2: " + file.getWebContentLink());
////
////                    //End Image Upload
//
//                Docs service = getDocsService(context, account);
//                String doc_id2 = mDocID;
//
//                Document doc2read = service.documents().get(doc_id2).execute();
//                int max_num = readStructrualElements(doc2read.getBody().getContent());
//                System.out.println("Max Line :- " + max_num);
//                List<Request> requests = new ArrayList<>();
//                String newPage = TextUtils.join("", Collections.nCopies((45 - (max_num % 45)), "\n"));
//                System.out.println("RES:" + newPage);
//
//
//                String urlLink = "https://drive.google.com/a/mail.huji.ac.il/uc?authuser=0&id=" + fileId + "&export=download";
//                requests.add(new Request().setInsertInlineImage(new InsertInlineImageRequest()
//                        .setUri(urlLink)
//                        .setLocation(new Location().setIndex(max_num - 1))
//                        .setObjectSize(new Size()
//                                .setHeight(new Dimension()
//                                        .setMagnitude(300.0)
//                                        .setUnit("PT"))
//                                .setWidth(new Dimension()
//                                        .setMagnitude(150.0)
//                                        .setUnit("PT")))));
//                String newText = "\n<Comments_" + fileId + "/>\n INSERT COMMENT HERE.\n<Comments_End>";
//                requests.add(new Request()
//                                .setInsertText(
//                                        new InsertTextRequest()
//                                                .setText(newText)
//                                                .setLocation(new Location().setIndex(max_num)))
//
//
////                        .setFields("bold")).setCreateNamedRange(
////                        new CreateNamedRangeRequest()
////                                .setName(fileId)
////                                .setRange(
////                                        new Range()
////                                                .setStartIndex(max_num +1)
////                                                .setEndIndex(max_num + newText.length())))
//                );
//                requests.add(
//                        new Request()
//                                .setCreateNamedRange(
//                                        new CreateNamedRangeRequest()
//                                                .setName(fileId)
//                                                .setRange(
//                                                        new Range()
////                                                                .setSegmentId(range.getSegmentId())
//                                                                .setStartIndex(max_num)
//                                                                .setEndIndex(max_num + newText.length()))));
//                requests.add(new Request().setUpdateTextStyle(new UpdateTextStyleRequest()
//                        .setTextStyle(new TextStyle()
//                                .setBold(true)
//                                .setItalic(true))
//                        .setRange(new Range()
//                                .setStartIndex(max_num)
//                                .setEndIndex(max_num + newText.length()))
//                        .setFields("bold")));
//
//                requests.add(new Request().setInsertText(
//                        new InsertTextRequest()
//                                .setText(newPage)
//                                .setLocation(new Location().setIndex(max_num + newText.length()))));
//                BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
//                BatchUpdateDocumentResponse response = service.documents()
//                        .batchUpdate(doc_id2, body).execute();
////                replaceNamedRange(service, doc_id2, fileId, "Maybe");
////                replaceNamedRange(service, doc_id2, fileId, "Maybe2");
////                OutputStream outputStream = new ByteArrayOutputStream();
////                File file2 = service2.files().get(doc_id2).me;
//
//
//            } catch (UserRecoverableAuthIOException recoverableException) {
//                if (mActivityRef.get() != null) {
//                    mActivityRef.get().onRecoverableAuthException(recoverableException);
//                }
//            } catch (IOException e) {
//                Log.w(TAG, "getContacts:exception", e);
//            }
//
//            return fileId;
//        }
//
//        @Override
//        protected void onPostExecute(String fileId) {
//            super.onPostExecute(fileId);
//            if (mActivityRef.get() != null) {
//                mActivityRef.get().onImageLoadFinished(fileId);
//            }
//        }
//    }

    @NonNull
    private static Drive getDriveService(Context context, Account account) {
        GoogleAccountCredential credential2 = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DRIVE_SCOPE));
        credential2.setSelectedAccount(account);
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential2)
                .setApplicationName("Upload File")
                .build();
    }

    @NonNull
    private static Docs getDocsService(Context context, Account account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(SCOPES));
        credential.setSelectedAccount(account);
        return new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("Create Doc in Drive :)")
                .build();
    }

//    static String GetTextforID(Docs service, String documentId, String rangeName)
//            throws IOException {
//        // Fetch the document to determine the current indexes of the named ranges.
//        Document document = service.documents().get(documentId).execute();
//
//        // Find the matching named ranges.
//        NamedRanges namedRangeList = document.getNamedRanges().get(rangeName);
//        if (namedRangeList == null) {
//            throw new IllegalArgumentException("The named range is no longer present in the document.");
//        }
//        // Determine all the ranges of text to be removed, and at which indexes the replacement text
//        // should be inserted.
//        List<Range> allRanges = new ArrayList<>();
//        for (NamedRange namedRange : namedRangeList.getNamedRanges()) {
//            allRanges.addAll(namedRange.getRanges());
//        }
//
////        // Sort the list of ranges by startIndex, in descending order.
////        allRanges.sort(Comparator.comparing(Range::getStartIndex).reversed());
//        String result = "";
//        // Create a sequence of requests for each range.
//        for (Range range : allRanges) {
//            // Delete all the content in the existing range.
//            result = GetTextInRange(document.getBody().getContent(), range);
//        }
//        return result;
//    }

    static void replaceNamedRange(Docs service, String documentId, String rangeName, String newText)
            throws IOException {
        // Fetch the document to determine the current indexes of the named ranges.
        Document document = service.documents().get(documentId).execute();

        // Find the matching named ranges.
        NamedRanges namedRangeList = document.getNamedRanges().get(rangeName);
        if (namedRangeList == null) {
            throw new IllegalArgumentException("The named range is no longer present in the document.");
        }
        // Determine all the ranges of text to be removed, and at which indexes the replacement text
        // should be inserted.
        List<Range> allRanges = new ArrayList<>();
        Set<Integer> insertIndexes = new HashSet<>();
        for (NamedRange namedRange : namedRangeList.getNamedRanges()) {
            allRanges.addAll(namedRange.getRanges());
            insertIndexes.add(namedRange.getRanges().get(0).getStartIndex());
        }

//        // Sort the list of ranges by startIndex, in descending order.
//        allRanges.sort(Comparator.comparing(Range::getStartIndex).reversed());

        // Create a sequence of requests for each range.
        List<Request> requests = new ArrayList<>();
        for (Range range : allRanges) {
            // Delete all the content in the existing range.
            requests.add(
                    new Request().setDeleteContentRange(new DeleteContentRangeRequest().setRange(range)));


            if (insertIndexes.contains(range.getStartIndex())) {
                // Insert the replacement text.
                String newestText = "\n<Comments_" + rangeName + "/>\n" + newText + "\n<Comments_End>";

                requests.add(
                        new Request()
                                .setInsertText(
                                        new InsertTextRequest()
                                                .setLocation(
                                                        new Location()
                                                                .setSegmentId(range.getSegmentId())
                                                                .setIndex(range.getStartIndex()))
                                                .setText(newestText)));

                // Re-create the named range on the new text.
                requests.add(
                        new Request()
                                .setCreateNamedRange(
                                        new CreateNamedRangeRequest()
                                                .setName(rangeName)
                                                .setRange(
                                                        new Range()
                                                                .setSegmentId(range.getSegmentId())
                                                                .setStartIndex(range.getStartIndex())
                                                                .setEndIndex(range.getStartIndex() + newestText.length()))));
            }
        }

        // Make a batchUpdate request to apply the changes, ensuring the document hasn't changed since
        // we fetched it.
        BatchUpdateDocumentRequest batchUpdateRequest =
                new BatchUpdateDocumentRequest()
                        .setRequests(requests)
                        .setWriteControl(new WriteControl().setRequiredRevisionId(document.getRevisionId()));
        service.documents().batchUpdate(documentId, batchUpdateRequest).execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]);
    }
//    private static class GetContactsTask extends AsyncTask<Account, Void, List<Person>> {
//
//        private WeakReference<RestApiActivity> mActivityRef;
//
//        public GetContactsTask(RestApiActivity activity) {
//            mActivityRef = new WeakReference<>(activity);
//        }
//
//        @Override
//        protected List<Person> doInBackground(Account... accounts) {
//            if (mActivityRef.get() == null) {
//                return null;
//            }
//
//            Context context = mActivityRef.get().getApplicationContext();
//            try {
//                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
//                        context,
//                        Collections.singleton(CONTACTS_SCOPE));
//                credential.setSelectedAccount(accounts[0]);
//
//                PeopleService service = new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                        .setApplicationName("Create Doc in Drive :)")
//                        .build();
//
//                ListConnectionsResponse connectionsResponse = service
//                        .people()
//                        .connections()
//                        .list("people/me")
//                        .setPersonFields("names,emailAddresses")
//                        .execute();
//
//                return connectionsResponse.getConnections();
//
//            } catch (UserRecoverableAuthIOException recoverableException) {
//                if (mActivityRef.get() != null) {
//                    mActivityRef.get().onRecoverableAuthException(recoverableException);
//                }
//            } catch (IOException e) {
//                Log.w(TAG, "getContacts:exception", e);
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(List<Person> people) {
//            super.onPostExecute(people);
//            if (mActivityRef.get() != null) {
//                mActivityRef.get().onConnectionsLoadFinished(people);
//            }
//        }
//    }
}