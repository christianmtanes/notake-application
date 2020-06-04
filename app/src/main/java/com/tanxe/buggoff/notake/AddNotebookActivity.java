package com.tanxe.buggoff.notake;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.tanxe.buggoff.notake.NotebookTable.Notebook;
import com.tanxe.buggoff.notake.NotebookTable.NotebookRoomDatabase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.Dimension;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.ParagraphStyle;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.TextStyle;
import com.google.api.services.docs.v1.model.UpdateParagraphStyleRequest;
import com.google.api.services.docs.v1.model.UpdateTextStyleRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddNotebookActivity extends AppCompatActivity {

    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
    private EditText editTextNBName;
    private EditText editTextNPcover;
    private NotebookRoomDatabase local_db;
    private String DocID;
    private Account mAccount;
    private AddNotebookActivity context;
    private String defaultCover = "";

    private final String SCOPES = "https://www.googleapis.com/auth/documents";
    // Global instance of the HTTP transport
    private final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    // Global instance of the JSON factory
    private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final String TAG = "AddNotebookActivity";
    private final String ADDED_NOTEBOOK = "added notebook";
    private final int RESULT_CODE = 1;
    private final int RC_RECOVERABLE = 9002;
    private final String ACCOUNT = "Account";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_notebook_layout);

        context = this;
        editTextNBName = (EditText) findViewById(R.id.notebook_name);
        editTextNPcover = (EditText) findViewById(R.id.notebook_cover);

        Intent intent = this.getIntent();
        mAccount = intent.getExtras().getParcelable(ACCOUNT);
        local_db = NotebookRoomDatabase.getDatabase(this);
        setCoverOptions();
    }

    private void setCoverOptions() {
        final RelativeLayout relativeLayout = findViewById(R.id.relLayout2);
        final RelativeLayout relativeLayout2 = findViewById(R.id.images);
        NavigationView navigationView = findViewById(R.id.chooseCover);
        navigationView.setCheckedItem(R.id.defaultCover);
        onImageclick(findViewById(R.id.nb2));
        relativeLayout.setVisibility(View.GONE);
        relativeLayout2.setVisibility(View.VISIBLE);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setCheckable(true);
                        menuItem.setChecked(true);
                        if (menuItem.getItemId() == R.id.defaultCover) {
                            relativeLayout.setVisibility(View.GONE);
                            relativeLayout2.setVisibility(View.VISIBLE);
                            onImageclick(findViewById(R.id.nb2));
                        }
                        if (menuItem.getItemId() == R.id.addLinke) {
                            relativeLayout.setVisibility(View.VISIBLE);
                            relativeLayout2.setVisibility(View.GONE);
                            defaultCover = "";
                        }
                        return true;
                    }
                });
    }

    public void onImageclick(View view) {
        int viewid = view.getId();
        ImageView nb2 = findViewById(R.id.nb2);
        ImageView nb3 = findViewById(R.id.nb3);
        ImageView nb4 = findViewById(R.id.nb4);
        ImageView nb5 = findViewById(R.id.nb5);
        ImageView nb6 = findViewById(R.id.nb6);
        ImageView nb7 = findViewById(R.id.nb7);
        ImageView nb8 = findViewById(R.id.nb8);

        if ((viewid == R.id.nb2)) {
            nb2.setBackgroundColor(context.getColor(R.color.grey_1));
            defaultCover = "nb2";
        } else nb2.setBackgroundColor(context.getColor(R.color.white));

        if ((viewid == R.id.nb3)) {
            nb3.setBackgroundColor(context.getColor(R.color.grey_1));
            defaultCover = "nb3";
        } else nb3.setBackgroundColor(context.getColor(R.color.white));

        if ((viewid == R.id.nb4)) {
            nb4.setBackgroundColor(context.getColor(R.color.grey_1));
            defaultCover = "nb4";
        } else nb4.setBackgroundColor(context.getColor(R.color.white));

        if ((viewid == R.id.nb5)) {
            nb5.setBackgroundColor(context.getColor(R.color.grey_1));
            defaultCover = "nb5";
        } else nb5.setBackgroundColor(context.getColor(R.color.white));

        if ((viewid == R.id.nb6)) {
            nb6.setBackgroundColor(context.getColor(R.color.grey_1));
            defaultCover = "nb6";
        } else nb6.setBackgroundColor(context.getColor(R.color.white));

        if ((viewid == R.id.nb7)) {
            nb7.setBackgroundColor(context.getColor(R.color.grey_1));
            defaultCover = "nb7";
        } else nb7.setBackgroundColor(context.getColor(R.color.white));

        if ((viewid == R.id.nb8)) {
            nb8.setBackgroundColor(context.getColor(R.color.grey_1));
            defaultCover = "nb8";
        } else nb8.setBackgroundColor(context.getColor(R.color.white));
    }


    public void onBackClick(View view) {
        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, resultIntent);
        finish();
    }

    public void onAddClick(View view) {
        String notebookName = editTextNBName.getText().toString();
        String notebookCover = editTextNPcover.getText().toString();

        if (notebookName.isEmpty()) {
            showSnackbar(view, "Fill the notebook name!");
        } else if (notebookCover == "" && defaultCover == "") {
            showSnackbar(view, "Fill the notebook cover URL!");
        } else {
            String cover = "";
            if (!notebookCover.isEmpty()) {
                cover = notebookCover;
            }
            if (defaultCover != "")
                cover = defaultCover;
            Notebook notebook = new Notebook(notebookName, cover, mAccount);
            local_db.insertNotebook(notebook);
            new CreateDocFile(this, notebook).execute(mAccount);
            finish();
        }
    }


    public void showSnackbar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }


    public Map<String, Object> toMap(Notebook notebook) {
        Log.d(TAG, "map notebook");
        HashMap<String, Object> notebookMap = new HashMap<>();
        notebookMap.put("ID", notebook.getId());
        notebookMap.put("account", notebook.getAccount());
        notebookMap.put("docID", notebook.getDocID());
        notebookMap.put("name", notebook.getName());
        notebookMap.put("coverImage", notebook.getCoverImage());
        notebookMap.put("modifiedDate", notebook.getModifiedDate());
        notebookMap.put("createdDate", notebook.getCreatedDate());
        return notebookMap;
    }

    private class AddNotebookToFirebase extends AsyncTask<Notebook, Void, Void> {

        @Override
        protected Void doInBackground(final Notebook... notebooks) {
            Log.d(TAG, "add notebook to firebase");
            HashMap<String, Object> notebookMap = (HashMap<String, Object>) toMap(notebooks[0]);

            mFirestore.collection("users")
                    .document(String.valueOf(notebooks[0].getAccount()))
                    .collection("notebooks").document(notebooks[0].getId())
                    .set(notebookMap).addOnSuccessListener(new OnSuccessListener<Void>() {
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

    private class CreateDocFile extends AsyncTask<Account, Void, String> {

        private String title;
        private WeakReference<AddNotebookActivity> mActivityRef;
        private Notebook notebook;

        public CreateDocFile(AddNotebookActivity activity, Notebook notebook) {
            mActivityRef = new WeakReference<>(activity);
            this.title = notebook.getName();
            this.notebook = notebook;
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
                setNewDocRequests(doc_id, service);
            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    mActivityRef.get().onRecoverableAuthException(recoverableException);
                }
            } catch (IOException e) {
                Log.w(TAG, "getContacts:exception", e);
            }

            return doc_id;
        }

        private void setNewDocRequests(String doc_id, Docs service) throws IOException {
            List<Request> requests = new ArrayList<>();
            String newText = title + "\n";
            requests.add(new Request()
                    .setInsertText(
                            new InsertTextRequest()
                                    .setText(newText)
                                    .setLocation(new Location().setIndex(1)))
            );

            requests.add(new Request().setUpdateTextStyle(new UpdateTextStyleRequest()
                    .setTextStyle(new TextStyle()
                            .setBold(true)
                            .setItalic(false).setUnderline(true).setFontSize(new Dimension().setMagnitude((double) 22).setUnit("PT")))
                    .setRange(new Range()
                            .setStartIndex(1)
                            .setEndIndex(1 + newText.length())).setFields("Bold,fontSize,Italic")
            ));
            requests.add(new Request().setUpdateParagraphStyle(new UpdateParagraphStyleRequest()
                    .setRange(new Range()
                            .setStartIndex(1)
                            .setEndIndex(1 + newText.length()))
                    .setParagraphStyle(new ParagraphStyle().setAlignment("CENTER")


                    )
                    .setFields("alignment")
            ));

            BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
            BatchUpdateDocumentResponse response = service.documents()
                    .batchUpdate(doc_id, body).execute();
        }

        @Override
        protected void onPostExecute(String doc_id) {
            super.onPostExecute(doc_id);
            System.out.println("onPostExecute!!");
            if (mActivityRef.get() != null) {
                mActivityRef.get().onDocLoadFinished(doc_id, notebook);
            }
        }
    }

    @NonNull
    private Docs getDocsService(Context context, Account account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(SCOPES));
        credential.setSelectedAccount(account);
        return new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("Create Doc in Drive :)").build();
    }

    protected void onRecoverableAuthException(UserRecoverableAuthIOException recoverableException) {
        Log.w(TAG, "onRecoverableAuthException", recoverableException);
        startActivityForResult(recoverableException.getIntent(), RC_RECOVERABLE);
    }

    protected void onDocLoadFinished(@Nullable String imageID, Notebook notebook) {
        DocID = imageID;
        local_db.updateDocId(notebook.getId(), DocID);
        notebook.setDocID(DocID);
        new AddNotebookToFirebase().execute(notebook);
    }
}