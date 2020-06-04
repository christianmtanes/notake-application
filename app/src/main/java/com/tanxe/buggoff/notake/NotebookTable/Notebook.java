package com.tanxe.buggoff.notake.NotebookTable;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity(tableName = "notebook_table")

public class Notebook implements Parcelable {
    @ColumnInfo(name = "id")
    @NonNull
    @PrimaryKey
    private String id;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "coverImage")
    private String coverImage;
    @ColumnInfo(name = "docID")
    private String docID;
//    @ColumnInfo(name = "imageType")
//    private String imageType;
    @ColumnInfo(name = "account")
    private Account account;
    @ColumnInfo(name = "createdDate")
    private String createdDate;
    @ColumnInfo(name = "modifiedDate")
    private String modifiedDate;

    public Notebook(@NonNull String name, @NonNull String coverImage, Account account) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.coverImage = coverImage;
        this.account = account;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        this.createdDate = date;
        this.modifiedDate = date;
    }

    @Ignore
    public Notebook(@NonNull String name, @NonNull String coverImage, Account account, String docID, String id, String createdDate, String modifiedDate) {
        this.id = id;
        this.name = name;
        this.coverImage = coverImage;
        this.account = account;
        this.docID = docID;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }

    @Ignore
    protected Notebook(Parcel in) {
        name = in.readString();
        coverImage = in.readString();
        docID = in.readString();
        id = Objects.requireNonNull(in.readString());
        account = in.readParcelable(getClass().getClassLoader());
        createdDate = in.readString();
        modifiedDate = in.readString();
    }

    public static final Creator<Notebook> CREATOR = new Creator<Notebook>() {
        @Override
        public Notebook createFromParcel(Parcel in) {
            return new Notebook(in);
        }

        @Override
        public Notebook[] newArray(int size) {
            return new Notebook[size];
        }
    };

    public String getName() {
        return name;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getDocID() {
        return docID;
    }

    public void setDocID(String docID) {
        this.docID = docID;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(coverImage);
        parcel.writeString(docID);
        parcel.writeString(id);
        parcel.writeParcelable(account, 111);
        parcel.writeString(createdDate);
        parcel.writeString(modifiedDate);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
}
