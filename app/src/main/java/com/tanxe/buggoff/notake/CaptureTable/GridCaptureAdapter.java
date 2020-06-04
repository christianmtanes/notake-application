package com.tanxe.buggoff.notake.CaptureTable;

import android.accounts.Account;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tanxe.buggoff.notake.CaptureView;
import com.tanxe.buggoff.notake.Notebook.NotebookActivity;
import com.tanxe.buggoff.notake.NotebookTable.Notebook;
import com.tanxe.buggoff.notake.R;
import com.tanxe.buggoff.notake.SearchActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GridCaptureAdapter extends ArrayAdapter<Capture> {
    private final String TAG = "GridCaptureAdapter";
    private final String MY_CAPTURE = "my capture";
    // Global instance of the HTTP transport
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    // Global instance of the JSON factory
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String SCOPES = "https://www.googleapis.com/auth/documents";
    private final String SEARCH_FOR = "search for";
    private final String MY_NOTEBOOK = "my notebook";
    private Notebook notebook;
    private final int MAX_LINES = 2;


    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
    private Context context;
    private LayoutInflater layoutInflater;
    private int resource;
    private String textViewResourceId;
    private ArrayList<Capture> captures;
    private CaptureRoomDatabase local_db;
    private ArrayList<Capture> filteredCaptures;

    public GridCaptureAdapter(Context context, int resource, String textViewResourceId, ArrayList<Capture> captures, Notebook notebook) {
        super(context, resource, captures);
        this.layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        this.captures = captures;
        this.context = context;
        this.resource = resource;
        this.textViewResourceId = textViewResourceId;
        this.notebook = notebook;
        local_db = CaptureRoomDatabase.getDatabase(context);
    }

    private static class ViewHolder {
        ImageView capture;
        TextView textView;
        ProgressBar progressBar;
        ImageView remove;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final GridCaptureAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(resource, parent, false);
            holder = new GridCaptureAdapter.ViewHolder();
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.gridCaptureProgressBar);
            holder.capture = (ImageView) convertView.findViewById(R.id.gridCaptureView);
            holder.textView = (TextView) convertView.findViewById(R.id.comments);
            holder.remove = (ImageView) convertView.findViewById(R.id.remove_icon);
            convertView.setTag(holder);
        } else {
            holder = (GridCaptureAdapter.ViewHolder) convertView.getTag();
        }

        final Capture capture = getItem(position);

        setTextView(holder, capture);
        onImageClick(holder, capture);
        onRemoveClick(holder, capture);
        loadImage(holder, capture);
        return convertView;
    }

    private void startSearchActivity(String hashtag) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra(MY_NOTEBOOK, notebook);
        intent.putExtra(SEARCH_FOR, hashtag);
        context.startActivity(intent);
    }


    private void setTextView(final ViewHolder holder, final Capture capture) {
        holder.textView.setText(capture.getComments());
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CaptureView.class);
                intent.putExtra(MY_NOTEBOOK, notebook);
                intent.putExtra(MY_CAPTURE, capture);
                context.startActivity(intent);
            }
        });
        holder.textView.post(new Runnable() {
            @Override
            public void run() {
                String text = holder.textView.getText().toString();
                SpannableString truncatedSpannableString = new SpannableString(text);
                if (holder.textView.getLineCount() > MAX_LINES) {
                    holder.textView.setMaxLines(MAX_LINES);
                    int lastCharShown = holder.textView.getLayout().getLineVisibleEnd(MAX_LINES - 1);
                    String moreString = context.getString(R.string.more);
                    String suffix = "  " + moreString;
                    String actionDisplayText = text.substring(0, lastCharShown - suffix.length() - 3) + "..." + suffix;
                    ClickableSpan privacy = getClickableSpan(capture);
                    truncatedSpannableString = new SpannableString(actionDisplayText);
                    int startIndex = actionDisplayText.indexOf(moreString);
                    truncatedSpannableString.setSpan(privacy, startIndex, startIndex + moreString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                hashTagSpan(truncatedSpannableString);
                holder.textView.setText(truncatedSpannableString);
                holder.textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        });
        holder.textView.setFocusableInTouchMode(true);
        holder.textView.setFocusable(true);
    }

    private void hashTagSpan(SpannableString truncatedSpannableString) {
        Matcher matcher = Pattern.compile("#([A-Za-z0-9_-]+)").matcher(truncatedSpannableString);

        while (matcher.find()) {
            ClickableSpan privacyHS = new ClickableSpan() {
                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setColor(context.getColor(R.color.colorPrimary));
                    ds.setUnderlineText(false);
                }

                @Override
                public void onClick(View textView) {
                    TextView tv = (TextView) textView;
                    Spanned s = (Spanned) tv.getText();
                    int start = s.getSpanStart(this);
                    int end = s.getSpanEnd(this);
                    startSearchActivity(s.subSequence(start, end).toString());
                }
            };
            truncatedSpannableString.setSpan(privacyHS, matcher.start(), matcher.end(), 0);
            String tag = matcher.group(0);
        }
    }

    private ClickableSpan getClickableSpan(final Capture capture) {
        return new ClickableSpan() {
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(context.getColor(R.color.blue));
                ds.setUnderlineText(false);
            }

            @Override
            public void onClick(View textView) {
                Intent intent = new Intent(context, CaptureView.class);
                intent.putExtra(MY_NOTEBOOK, notebook);
                intent.putExtra(MY_CAPTURE, capture);
                context.startActivity(intent);
            }
        };
    }

    private void loadImage(final ViewHolder holder, Capture capture) {
        String image = "https://drive.google.com/a/mail.huji.ac.il/uc?authuser=0&id="
                + capture.getImageID() + "&export=download";
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage("file:///" + capture.getImagePath(), holder.capture, new ImageLoadingListener() {

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                if (holder.progressBar != null) {
                    holder.progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                if (holder.progressBar != null) {
                    holder.progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (holder.progressBar != null) {
                    holder.progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                if (holder.progressBar != null) {
                    holder.progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void onFocusChange(final ViewHolder holder, final Capture capture) {
        Log.d(TAG, "focusChange");
        holder.textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    return;
                }
                String newText = holder.textView.getText().toString();
                updateTags(capture, newText);
                local_db.updateTags(capture.getId(), capture.getTags());
                local_db.updateNotes(capture.getId(), newText);
                capture.setComments(newText);
                new replaceText((NotebookActivity) context, capture, capture.getImageID(), newText).execute(capture.getAccount());
            }
        });
    }

    private void updateTags(Capture capture, String txt) {
        String regexPattern = "(#\\w+)";

        Pattern p = Pattern.compile(regexPattern);
        Matcher m = p.matcher(txt);
        ArrayList<String> tags = new ArrayList<>();
        while (m.find()) {
            String hashtag = m.group(1);
            tags.add(hashtag);
        }
        capture.setTags(tags);
    }

    private void onRemoveClick(ViewHolder holder, final Capture capture) {
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                remove(capture);
                                local_db.deleteCapture(capture);
                                new DeleteCaptureFromFirebase().execute(capture);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Are you sure, you want to delete this capture? ").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });
    }

    private void onImageClick(ViewHolder holder, final Capture capture) {
        holder.capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CaptureView.class);
                intent.putExtra(MY_CAPTURE, capture);
                intent.putExtra(MY_NOTEBOOK, notebook);
                context.startActivity(intent);
            }
        });
    }

    private class DeleteCaptureFromFirebase extends AsyncTask<Capture, Void, Void> {
        @Override
        protected Void doInBackground(Capture... captures) {
            Log.d(TAG, "delete capture from firebase");

            mFirestore.collection("users")
                    .document(String.valueOf(captures[0].getAccount()))
                    .collection("notebooks").document(captures[0].getNotebookID())
                    .collection("Captures").document(captures[0].getId())
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "capture successfully deleted!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Write failed
                            // ...
                            Log.w(TAG, "Error deleting capture", e);
                        }
                    });
            return null;
        }
    }

    private class replaceText extends AsyncTask<Account, Void, String> {

        private WeakReference<NotebookActivity> mActivityRef;
        private String docID;
        private String imageID;
        private String replaceWith;
        private Capture capture;

        public replaceText(NotebookActivity activity, Capture capture, String imageID, String replaceWith) {
            mActivityRef = new WeakReference<>(activity);
            this.docID = capture.getDocID();
            this.imageID = imageID;
            this.replaceWith = replaceWith;
            this.capture = capture;
        }

        @Override
        protected String doInBackground(Account... accounts) {
            if (mActivityRef.get() == null) {
                return null;
            }
            Log.d(TAG, "doInBachground");
            String doc_id = "";
            while (docID == null) {
                docID = notebook.getDocID();
            }


            Context context = mActivityRef.get();
            try {

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
            System.out.println("old Range: " + range.getStartIndex() + "," + range.getEndIndex());

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
    }

    @NonNull
    @Override
    public Filter getFilter() {
        Filter myFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults = new FilterResults();
                List<Capture> captures = local_db.getALLCapturesByID(notebook.getId());
                if (charSequence.equals("")) {
                    filterResults.values = captures;
                    filterResults.count = captures.size();
                    return filterResults;
                }

                ArrayList<Capture> filterResultsData = new ArrayList<>();
                String filter = charSequence.toString().toLowerCase();

                for (Capture capture : captures) {
                    String captureNotes = capture.getComments();
                    if (captureNotes != null) {
                        if (captureNotes.toLowerCase().contains(filter)) {
                            filterResultsData.add(capture);
                        }
                    }
                }
                filterResults.values = filterResultsData;
                filterResults.count = filterResultsData.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredCaptures = (ArrayList<Capture>) filterResults.values;
                clear();
                addAll(filteredCaptures);
                notifyDataSetChanged();
            }
        };
        return myFilter;
    }
}