package com.example.myapplication1;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.File;

public class ParcelableFile extends File implements Parcelable {

    public ParcelableFile(String path){
        super(path);
    }

    protected ParcelableFile(Parcel in){
        super(in.readString());
    }

    public String getPhotoAbsolutePath() {
        return super.getAbsolutePath();
    }


    public static final Creator<ParcelableFile> CREATOR=new Creator<ParcelableFile>(){

        @Override
        public ParcelableFile createFromParcel(Parcel in){
            return new ParcelableFile(in);
        }

        @Override
        public ParcelableFile[] newArray(int size){
            return new ParcelableFile[size];
        }


    };

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags){
        dest.writeString(getAbsolutePath());
    }
}