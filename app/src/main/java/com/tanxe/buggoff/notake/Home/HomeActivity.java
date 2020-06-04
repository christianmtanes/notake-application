package com.tanxe.buggoff.notake.Home;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.tanxe.buggoff.notake.NotebookTable.Notebook;
import com.tanxe.buggoff.notake.NotebookTable.NotebookRoomDatabase;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;

import com.tanxe.buggoff.notake.AddNotebookActivity;
import com.tanxe.buggoff.notake.R;
import com.tanxe.buggoff.notake.NotebookTable.GridNotebookAdapter;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private GridNotebookAdapter gridNBAdapter;
    private final String GOOGLE_SIGNIN_CLIENT = "googleSignInClient";
    private final String ADDED_NOTEBOOK = "added notebook";
    private final String TAG = "HomeActivity";
    private final String ACCOUNT = "Account";
    private final int REQ_CODE_ADDNB = 1;
    private final int NUM_OF_COL = 2;
    private HomeActivity context;
    private Account mAccount;
    private NotebookRoomDatabase local_db;

    private FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
    private ArrayList<Notebook> myNotebooks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        context = this;

        Intent intent = context.getIntent();
        mAccount = intent.getExtras().getParcelable(ACCOUNT);

        setBottomNavView();
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(context));
        setUpGrid();

        local_db = NotebookRoomDatabase.getDatabase(this);
        local_db.getAllNotebooks().observe(this, new Observer<List<Notebook>>() {
            @Override
            public void onChanged(@Nullable final List<Notebook> notebooks) {
                gridNBAdapter.clear();
                new loadFromFirebase().execute();
                gridNBAdapter.addAll(notebooks);
                gridNBAdapter.notifyDataSetChanged();
                Log.d(TAG, "on changeeeed else");
            }
        });
        setSearchView();
        onSwipeActivity();
    }

    /*
     * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
     * performs a swipe-to-refresh gesture.
     */
    public void onSwipeActivity() {
        final SwipeRefreshLayout mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.i(TAG, "onRefresh called from SwipeRefreshLayout");
                        mySwipeRefreshLayout.setRefreshing(false);
                    }
                }
        );
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
                if (item.getItemId() == R.id.add_icon) {
                    Intent intent = new Intent(context, AddNotebookActivity.class);
                    intent.putExtra(ACCOUNT, mAccount);
                    startActivityForResult(intent, REQ_CODE_ADDNB);
                }
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_ADDNB) {
            if (resultCode == Activity.RESULT_OK) {
                Notebook notebook = (Notebook) data.getExtras().getParcelable(ADDED_NOTEBOOK);
                //gridNBAdapter.add(notebook);
                Log.d(TAG, "onActivityResult.");
                local_db.insertNotebook(notebook);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                return;
            }
        }
    }

    private void setSearchView() {
        EditText searchET = (EditText) findViewById(R.id.searchET);

        searchET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                gridNBAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void setUpGrid() {
        GridView gridView = findViewById(R.id.gridView);
        int gridWidth = getResources().getDisplayMetrics().widthPixels;
        int imgWidth = gridWidth / NUM_OF_COL;
        gridView.setColumnWidth(imgWidth);

        gridNBAdapter = new GridNotebookAdapter(context, R.layout.layout_grid_notebook_view, "", myNotebooks);
        gridView.setAdapter(gridNBAdapter);
    }

    private class loadFromFirebase extends AsyncTask<Notebook, Void, Void> {
        @Override
        protected Void doInBackground(final Notebook... notebooks) {
            Log.d(TAG, "load data from the firebase");
            mFirestore.collection("users")
                    .document(String.valueOf(mAccount))
                    .collection("notebooks")
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
                                    Notebook notebook = new Notebook(doc.getString("name"),
                                            doc.getString("coverImage"), account, doc.getString("docID"), doc.getId(), doc.getString("createdDate"), doc.getString("modifiedDate"));
                                    local_db.insertNotebook(notebook);
                                }
                            }
                        }
                    });
            return null;
        }
    }
}
