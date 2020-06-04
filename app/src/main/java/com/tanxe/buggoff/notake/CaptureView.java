package com.tanxe.buggoff.notake;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tanxe.buggoff.notake.CaptureTable.Capture;
import com.tanxe.buggoff.notake.CaptureTable.CaptureRoomDatabase;
import com.tanxe.buggoff.notake.NotebookTable.Notebook;
import com.tanxe.buggoff.notake.NotebookTable.NotebookRoomDatabase;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.CreateNamedRangeRequest;
import com.google.api.services.docs.v1.model.DeleteContentRangeRequest;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.NamedRange;
import com.google.api.services.docs.v1.model.NamedRanges;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.WriteControl;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaptureView extends AppCompatActivity {

    private final String MY_CAPTURE = "my capture";
    private final String TAG = "Captureview";

    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    // Global instance of the JSON factory
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String SCOPES = "https://www.googleapis.com/auth/documents";
    private final String SEARCH_FOR = "search for";
    private final String MY_NOTEBOOK = "my notebook";
    private ArrayList<String> tags = new ArrayList<>();
    private CaptureRoomDatabase local_db;
    private NotebookRoomDatabase local_dbNB;

    private CaptureView context;
    private Notebook notebook;
    private Capture capture;
    private ImageView imageView;
    private ProgressBar progressBar;
    private EditText editText;
    private TextView textView;
    private String DocID;
    private boolean isTextChanged = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.capture_view);

        context = this;
        local_db = CaptureRoomDatabase.getDatabase(context);
        local_dbNB = NotebookRoomDatabase.getDatabase(context);

        Intent intent = context.getIntent();
        capture = intent.getExtras().getParcelable(MY_CAPTURE);
        notebook = intent.getExtras().getParcelable(MY_NOTEBOOK);

        textView = (TextView) findViewById(R.id.notebook_name);
        textView.setText(capture.getNotebookName());

        imageView = (ImageView) findViewById(R.id.gridCaptureView);
        progressBar = (ProgressBar) findViewById(R.id.gridCaptureProgressBar);

        setEditText();
        loadImage();
        onFocusChange();
    }

    private void setEditText() {
        editText = (EditText) findViewById(R.id.comments);
        HashTagHelper mTextHashTagHelper = HashTagHelper.Creator.create(context.getColor(R.color.colorPrimary), new HashTagHelper.OnHashTagClickListener() {
            @Override
            public void onHashTagClicked(String hashTag) {
                startSearchActivity("#" + hashTag);
            }
        });
        mTextHashTagHelper.handle(editText);
        editText.setText(capture.getComments());
    }

    private void startSearchActivity(String hashtag) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra(MY_NOTEBOOK, notebook);
        intent.putExtra(SEARCH_FOR, hashtag);
        context.startActivity(intent);
    }


    private void onFocusChange() {
        Log.d(TAG, "focusChange");
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String newText = editText.getText().toString();
                capture.setComments(newText);
                local_db.updateNotes(capture.getId(), newText);
                isTextChanged = true;
                getTags(newText);
            }
        });
    }

    private void getTags(String txt) {
        String regexPattern = "(#\\w+)";

        Pattern p = Pattern.compile(regexPattern);
        Matcher m = p.matcher(txt);
        while (m.find()) {
            String hashtag = m.group(1);
            tags.add(hashtag);
        }
    }

    private void loadImage() {
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage("file:///" + capture.getImagePath(), imageView, new ImageLoadingListener() {

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    public void onBackClick(View view) {
        if (isTextChanged) {
            new replaceText(context, capture.getDocID(), capture.getImageID(),
                    capture.getComments()).execute(capture.getAccount());
            capture.setTags(tags);
            local_db.updateTags(capture.getId(), tags);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isTextChanged) {
            new replaceText(context, capture.getDocID(), capture.getImageID(),
                    capture.getComments()).execute(capture.getAccount());
            capture.setTags(tags);
            local_db.updateTags(capture.getId(), tags);
        }
    }

    private class replaceText extends AsyncTask<Account, Void, String> {

        private WeakReference<CaptureView> mActivityRef;
        private String docID;
        private String imageID;
        private String replaceWith;

        public replaceText(CaptureView activity, String docID, String imageID, String replaceWith) {
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
            Log.d(TAG, "doInBachground");
            String doc_id = "";
            Context context = mActivityRef.get();
            try {
                while (docID == null) {
                    docID = notebook.getDocID();
                }
                replaceNamedRange(getDocsService(context, accounts[0]), this.docID, this.imageID, replaceWith);
            } catch (UserRecoverableAuthIOException recoverableException) {
                if (mActivityRef.get() != null) {
                    onRecoverableAuthException(recoverableException);
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
                onDocLoadFinished(doc_id);
            }
        }
    }

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


    protected void onRecoverableAuthException(UserRecoverableAuthIOException recoverableException) {
        Log.w(TAG, "onRecoverableAuthException", recoverableException);
//        startActivityForResult(recoverableException.getIntent(), RC_RECOVERABLE);
    }

    protected void onDocLoadFinished(@Nullable String imageID) {
        DocID = imageID;
    }
}
