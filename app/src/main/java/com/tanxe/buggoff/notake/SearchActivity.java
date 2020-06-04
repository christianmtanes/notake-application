package com.tanxe.buggoff.notake;

import android.accounts.Account;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.tanxe.buggoff.notake.CaptureTable.Capture;
import com.tanxe.buggoff.notake.CaptureTable.CaptureRoomDatabase;
import com.tanxe.buggoff.notake.CaptureTable.GridCaptureAdapter;
import com.tanxe.buggoff.notake.NotebookTable.Notebook;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText searchET;
    private Notebook myNotebook;
    private SearchActivity context;
    private CaptureRoomDatabase local_db;
    private final String TAG = "SearchActivity";
    private final String MY_NOTEBOOK = "my notebook";
    private final String SEARCH_FOR = "search for";
    private String hashtag = "";
    private GridCaptureAdapter gridCaptureAdapter;
    private ArrayList<Capture> myCaptures = new ArrayList<>();
    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        context = this;
        Intent intent = context.getIntent();
        myNotebook = intent.getExtras().getParcelable(MY_NOTEBOOK);
        hashtag = intent.getExtras().getString(SEARCH_FOR);
        setUpGrid();
        setSearchView();
        setLocalDB();
        if (hashtag != "" && hashtag != null) {
            searchET.setText(hashtag);
            gridCaptureAdapter.getFilter().filter(hashtag);
        }
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

    public void onBackClick(View view) {
        finish();
    }

    private void setUpGrid() {
        GridView gridView = findViewById(R.id.gridViewNB);
        gridCaptureAdapter = new GridCaptureAdapter(context, R.layout.layout_grid_capture_view, "", myCaptures, myNotebook);
        gridView.setAdapter(gridCaptureAdapter);
    }

    private void setSearchView() {
        searchET = (EditText) findViewById(R.id.searchET);
        searchET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                gridCaptureAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
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
}
