package com.tanxe.buggoff.notake.CaptureTable;

import android.accounts.Account;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity(tableName = "capture_table")
public class Capture implements Parcelable {
    private static final String TAG = "Capture";
    private static FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    @ColumnInfo(name = "NotebookID")
    private String notebookID;
    @ColumnInfo(name = "notebookName")
    private String notebookName;
    @ColumnInfo(name = "comments")
    private String comments;
    @ColumnInfo(name = "imagePath")
    private String imagePath;
    @ColumnInfo(name = "tags")
    private ArrayList<String> tags = new ArrayList<String>();
    @ColumnInfo(name = "account")
    private Account account;
    @ColumnInfo(name = "docID")
    private String docID;
    @ColumnInfo(name = "date")
    private Date date;
    @ColumnInfo(name = "imageID")
    private String imageID;
    private static final int PARCELABLE_FLAG = 111;

    @Ignore
    public Capture(String id, String notebookID, String notebookName, String comments,
                   String imagePath, ArrayList<String> tags, Date date, Account account,
                   String docID, String imageID) {
        this.id = id;
        this.notebookName = notebookName;
        this.notebookID = notebookID;
        this.comments = comments;
        this.imagePath = imagePath;
        this.tags = tags;
        this.date = date;
        this.account = account;
        this.docID = docID;
        this.imageID = imageID;
    }

    public Capture(String notebookID, String notebookName, String imagePath, Date date, Account account, String docID) {
        this.id = UUID.randomUUID().toString();
        this.notebookName = notebookName;
        this.notebookID = notebookID;
        this.imagePath = imagePath;
        this.date = date;
        this.account = account;
        this.docID = docID;
    }

    @Ignore
    protected Capture(Parcel in) {
        id = in.readString();
        notebookName = in.readString();
        notebookID = in.readString();
        comments = in.readString();
        imagePath = in.readString();
        tags = (ArrayList<String>) in.readSerializable();
        date = (Date) in.readSerializable();
        account = in.readParcelable(getClass().getClassLoader());
        imageID = in.readString();
        docID = in.readString();
    }

    public static final Creator<Capture> CREATOR = new Creator<Capture>() {
        @Override
        public Capture createFromParcel(Parcel in) {
            return new Capture(in);
        }

        @Override
        public Capture[] newArray(int size) {
            return new Capture[size];
        }
    };

    public String getId() {
        return id;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        Log.d(TAG, "setComments");
        this.comments = comments;
        new UpdateCaptureOnFirebase().execute();
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTags(ArrayList<String> tags) {
        Log.d(TAG, "setTags");
        this.tags = tags;
        new UpdateCaptureOnFirebase().execute();
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void addTags(String tag) {
        tags.add(tag);
        new UpdateCaptureOnFirebase().execute();
    }

    public void setNotebookID(String notebookID) {
        notebookID = notebookID;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }


    public String getNotebookID() {
        return notebookID;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(notebookName);
        parcel.writeString(notebookID);
        parcel.writeString(comments);
        parcel.writeString(imagePath);
        parcel.writeSerializable(tags);
        parcel.writeSerializable(date);
        parcel.writeParcelable(account, PARCELABLE_FLAG);
        parcel.writeString(imageID);
        parcel.writeString(docID);
    }

    public String getNotebookName() {
        return notebookName;
    }

    public void setNotebookName(String notebookName) {
        this.notebookName = notebookName;
    }

    public String getImageID() {
        return imageID;
    }

    public void setImageID(String imageID) {
        this.imageID = imageID;
    }

    public String getDocID() {
        return docID;
    }

    public void setDocID(String docID) {
        Log.d(TAG, "setDocID");
        this.docID = docID;
        new UpdateCaptureOnFirebase().execute();
    }

    private class UpdateCaptureOnFirebase extends AsyncTask<Capture, Void, Void> {
        @Override
        protected Void doInBackground(Capture... captures) {
            Log.d(TAG, "update capture from firebase");
            HashMap<String, Object> captureMap = (HashMap<String, Object>) toMap();

            mFirestore.collection("users")
                    .document(String.valueOf(getAccount()))
                    .collection("notebooks").document(getNotebookID())
                    .collection("Captures").document(getId())
                    .update(captureMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "capture successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Write failed
                            // ...
                            Log.w(TAG, "Error updating capture", e);
                        }
                    });
            return null;
        }
    }

    private Map<String, Object> toMap() {
        Log.d(TAG, "map capture");
        HashMap<String, Object> captureMap = new HashMap<>();
        captureMap.put("ID", getId());
        captureMap.put("comments", getComments());
        captureMap.put("tags", getTags());
        captureMap.put("date", getDate());
        captureMap.put("imagePath", getImagePath());
        captureMap.put("notebookID", getNotebookID());
        captureMap.put("docID", getDocID());
        captureMap.put("account", getAccount());
        captureMap.put("imageID", getImageID());
        captureMap.put("notebookName", getNotebookName());

        return captureMap;
    }
}
