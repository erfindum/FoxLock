package com.smartfoxitsolutions.foxlock.mediavault.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.smartfoxitsolutions.foxlock.AppLoaderActivity;
import com.smartfoxitsolutions.foxlock.AppLockModel;
import com.smartfoxitsolutions.foxlock.R;
import com.smartfoxitsolutions.foxlock.mediavault.MediaAlbumPickerActivity;
import com.smartfoxitsolutions.foxlock.mediavault.MediaVaultModel;
import com.smartfoxitsolutions.foxlock.mediavault.SelectedMediaModel;
import com.smartfoxitsolutions.foxlock.mediavault.VaultDbHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by RAAJA on 13-10-2016.
 */

public class MediaMoveInTask implements Runnable {

   private Context context;
   private ContentResolver contentResolver;
   private String albumBucketId, mediaType, currentVaultMediaFile, currentVaultThumbnailFile;
   private ArrayList<String> selectedMediaId, fileNames;
   private int viewWidth, viewHeight;
   private VaultDbHelper vaultDatabaseHelper;
   private SQLiteDatabase vaultDb;
   private Handler uiHandler;
   private ContentValues insertValues;
   private Message mssg;

    public MediaMoveInTask(Context ctxt, Handler.Callback callback) {
        this.context = ctxt;
        this.uiHandler = new Handler(Looper.getMainLooper(),callback);
        insertValues = new ContentValues();
    }

    void setTaskRequirements(String bucketId, String media){
        this.albumBucketId=bucketId;
        this.mediaType = media;
        this.selectedMediaId = SelectedMediaModel.getInstance().getSelectedMediaIdList();
        this.fileNames = SelectedMediaModel.getInstance().getSelectedMediaFileNameList();
        this.contentResolver = context.getContentResolver();
        SharedPreferences prefs = context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE);
        int viewWidthPixel = Math.round(context.getResources().getDimension(R.dimen.vault_album_thumbnail_width));
        int viewHeightPixel = Math.round(context.getResources().getDimension(R.dimen.vault_album_thumbnail_height));
        this.viewWidth = prefs.getInt(AppLoaderActivity.MEDIA_THUMBNAIL_WIDTH_KEY,viewWidthPixel);
        this.viewHeight = prefs.getInt(AppLoaderActivity.MEDIA_THUMBNAIL_HEIGHT_KEY,viewHeightPixel);
        String databasePath = Environment.getExternalStorageDirectory()+ File.separator
                +".lockup"+File.separator+"vault_db";
        vaultDatabaseHelper = new VaultDbHelper(context.getApplicationContext(),databasePath,null,1);
        vaultDb = vaultDatabaseHelper.getWritableDatabase();
    }

    private Uri getExternalUri(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return null;
    }

    private String[] getProjection(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return new String[]{MediaStore.Images.Media._ID,MediaStore.Images.Media.BUCKET_ID,MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                            ,MediaStore.Images.Media.DATA};

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return new String[]{MediaStore.Video.Media._ID,MediaStore.Video.Media.BUCKET_ID,MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                        ,MediaStore.Video.Media.DATA};

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.ALBUM_ID,MediaStore.Audio.Media.ALBUM
                        ,MediaStore.Audio.Media.DATA};
        }
        return null;
    }

    private String getSelection(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media._ID+" IN ("
                        + TextUtils.join(",", Collections.nCopies(selectedMediaId.size(),"?"))+")"
                        + " AND "+ MediaStore.Images.Media.BUCKET_ID+"=?";
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media._ID+" IN ("
                        + TextUtils.join(",", Collections.nCopies(selectedMediaId.size(),"?"))+")"
                        + " AND "+ MediaStore.Video.Media.BUCKET_ID+"=?";
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media._ID+" IN ("
                        + TextUtils.join(",", Collections.nCopies(selectedMediaId.size(),"?"))+")"
                        + " AND "+ MediaStore.Audio.Media.ALBUM_ID+"=?";
        }
        return null;
    }

    private String[] getSelectionArgs(){
        String[] selectionArgs = new String[selectedMediaId.size()+1];
        for(int i=0;i<selectedMediaId.size();i++){
            selectionArgs[i] = selectedMediaId.get(i);
        }
        selectionArgs[selectionArgs.length-1] = albumBucketId;
        return selectionArgs;
    }

    private String getBucketNameIndex(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.BUCKET_DISPLAY_NAME;
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.BUCKET_DISPLAY_NAME;
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.ALBUM;
        }
        return null;
    }

    private String getDataIndex(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media.DATA;
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media.DATA;
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media.DATA;
        }
        return null;
    }

    private String getVaultMediaType(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return "image";
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return "video";
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return "audio";
        }
        return null;
    }

    private String getOrderBy(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return MediaStore.Images.Media._ID +" ASC";
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return MediaStore.Video.Media._ID +" ASC";
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return MediaStore.Audio.Media._ID +" ASC";
        }
        return null;
    }

    private String getBucketId(String path){
        StringBuilder pathBucketId = new StringBuilder();
        try {
            byte[] pathBucketByte = path.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] pathDigest = md.digest(pathBucketByte);
            for (int i = 0; i < pathDigest.length; ++i) {
                pathBucketId.append(Integer.toHexString((pathDigest[i] & 0xFF) | 0x100).substring(1,3));
            }
        }catch (NoSuchAlgorithmException | UnsupportedEncodingException e){
            e.printStackTrace();
        }
        return pathBucketId.toString();
    }

    @Override
    public void run() {
        Cursor mediaCursor;
        String[] projection = getProjection(mediaType);
        mediaCursor = contentResolver.query(getExternalUri(mediaType),projection,getSelection(mediaType)
                ,getSelectionArgs(),getOrderBy(mediaType));
        if(mediaCursor !=null && mediaCursor.getCount()>0){
            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
            mssg.arg1 = 0;
            mssg.arg2 = mediaCursor.getCount();
            mssg.sendToTarget();
            try {
                moveMediaToVault(mediaCursor);
            }catch (IOException e){
                e.printStackTrace();
                SelectedMediaModel.getInstance().getSelectedMediaIdList().clear();
                SelectedMediaModel.getInstance().getSelectedMediaFileNameList().clear();
                File curentVaultFile = new File(currentVaultMediaFile);
                File curentThumbnailFile = new File(currentVaultThumbnailFile);
                if(curentVaultFile.exists()){
                    curentVaultFile.delete();
                }
                if(curentThumbnailFile.exists()){
                    curentThumbnailFile.delete();
                }
                mssg = uiHandler.obtainMessage();
                mssg.what = MediaMoveService.MEDIA_MOVE_COMPLETED;
                mssg.sendToTarget();
            }
        }
    }

   private void moveMediaToVault(Cursor cursor) throws IOException{
       boolean shouldPostInsufficientSpace = false;
        cursor.moveToFirst();
           do {
               int dataIndex = cursor.getColumnIndex(getDataIndex(mediaType));
               int bucketNameIndex = cursor.getColumnIndex(getBucketNameIndex(mediaType));
               String dataPath = cursor.getString(dataIndex);
               String uniqueBucketName = cursor.getString(bucketNameIndex);
               String uniqueBucketId = "";
               if(mediaType.equals(MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA)){
                   uniqueBucketId = getBucketId(uniqueBucketName);
               }else {
                   uniqueBucketId = getBucketId(dataPath.substring(0, dataPath.lastIndexOf(File.separator)));
               }
               String originalFileName = dataPath.substring(dataPath.lastIndexOf(File.separator) + 1, dataPath.lastIndexOf("."));
               String extension = dataPath.substring(dataPath.lastIndexOf(".") + 1);
               String destPathDummy = Environment.getExternalStorageDirectory() + File.separator
                       + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                       + uniqueBucketId + File.separator + originalFileName + "." + extension;
               String destPath = Environment.getExternalStorageDirectory() + File.separator
                       + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                       + uniqueBucketId + File.separator + fileNames.get(cursor.getPosition());
               String thumbnailPathDummy = Environment.getExternalStorageDirectory() + File.separator
                       + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                       + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                       + File.separator + originalFileName + "." + "jpg";
               String thumbnailPath = Environment.getExternalStorageDirectory() + File.separator
                       + ".lockup" + File.separator + getMediaFolder(mediaType) + File.separator
                       + uniqueBucketId + File.separator + ".thumbs" + File.separator + uniqueBucketId
                       + File.separator + fileNames.get(cursor.getPosition());
               currentVaultMediaFile = destPathDummy;
               currentVaultThumbnailFile = thumbnailPathDummy;

               boolean isFileSpaceAvailable = getFileSpaceAvailability(dataPath);

               boolean mediaCopied = false;
               if(isFileSpaceAvailable) {
                   mediaCopied = copyMediaFile(dataPath, destPathDummy);
               }else{
                   shouldPostInsufficientSpace = true;
                   break;
               }
               boolean thumbnailCopied = false;
               Log.d("VaultMedia", String.valueOf(mediaCopied) + " mediacopied");
               if (mediaCopied) {
                   Bitmap thumbnail = getThumbnail(dataPath);
                   Log.d("VaultMedia", String.valueOf(thumbnail == null));
                   File thumbnailFile = new File(thumbnailPathDummy);
                   if (!thumbnailFile.getParentFile().exists()) {
                       thumbnailFile.getParentFile().mkdirs();
                   }
                   boolean thumbFileCreated = false;
                   if (thumbnailFile.getParentFile().exists() && !thumbnailFile.exists()) {
                       thumbFileCreated = thumbnailFile.createNewFile();
                   } else {
                       thumbFileCreated = true;
                   }
                   if (thumbFileCreated && thumbnail != null) {
                       thumbnailCopied = thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(thumbnailPathDummy));
                       thumbnail.recycle();
                       Log.d("VaultMedia", String.valueOf(thumbnailCopied) + " thumbnail copied");
                   }
                   if (thumbFileCreated && thumbnail == null) {
                       thumbnailCopied = true;
                       Log.d("VaultMedia", String.valueOf(thumbnailCopied) + " thumbnail copied");
                   }
                   Log.d("VaultMedia", " ThumbnailCreated");
               }  else
               {
                   mssg = uiHandler.obtainMessage();
                   mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
                   mssg.arg1 = cursor.getPosition() + 1;
                   mssg.arg2 = cursor.getCount();
                   mssg.sendToTarget();
                   continue;
               }
               if (thumbnailCopied) {
                   boolean renamedFile = renameVaultMedia(destPathDummy,destPath);
                   boolean renamedThumbnail = false;
                   if(renamedFile) {
                       renamedThumbnail = renameVaultMedia(thumbnailPathDummy, thumbnailPath);
                       Log.d("VaultMedia",String.valueOf(renamedThumbnail) + " thumbnail renamed");
                   }else{
                       deleteFailedFiles(destPathDummy);
                   }
                   if(renamedThumbnail) {
                       insertValues.put(MediaVaultModel.ORIGINAL_MEDIA_PATH, dataPath);
                       insertValues.put(MediaVaultModel.VAULT_MEDIA_PATH, destPath);
                       insertValues.put(MediaVaultModel.ORIGINAL_FILE_NAME, originalFileName);
                       insertValues.put(MediaVaultModel.VAULT_FILE_NAME, fileNames.get(cursor.getPosition()));
                       insertValues.put(MediaVaultModel.VAULT_BUCKET_ID, uniqueBucketId);
                       insertValues.put(MediaVaultModel.VAULT_BUCKET_NAME, uniqueBucketName);
                       insertValues.put(MediaVaultModel.FILE_EXTENSION, extension);
                       insertValues.put(MediaVaultModel.MEDIA_TYPE, getVaultMediaType(mediaType));
                       insertValues.put(MediaVaultModel.TIME_STAMP, fileNames.get(cursor.getPosition()));
                       insertValues.put(MediaVaultModel.THUMBNAIL_PATH, thumbnailPath);
                       long insertedRow = 0;
                       if (vaultDb != null) {
                           insertedRow = vaultDb.insert(MediaVaultModel.TABLE_NAME, "NULL", insertValues);
                       }
                       deleteOriginalMedia(dataPath);
                      MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{dataPath}, null,
                              new MediaScannerConnection.OnScanCompletedListener() {
                          @Override
                          public void onScanCompleted(String path, Uri uri) {
                              Log.d("VaultMedia"," Scanned " + path);
                          }
                      });
                       Log.d("VaultMedia"," Inserted and deleted");
                   }
                   else{
                       deleteFailedFiles(thumbnailPathDummy);
                   }
                   mssg = uiHandler.obtainMessage();
                   mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
                   mssg.arg1 = cursor.getPosition() + 1;
                   mssg.arg2 = cursor.getCount();
                   mssg.sendToTarget();
               }else{
                   mssg = uiHandler.obtainMessage();
                   mssg.what = MediaMoveService.MEDIA_SUCCESSFULLY_MOVED;
                   mssg.arg1 = cursor.getPosition() + 1;
                   mssg.arg2 = cursor.getCount();
                   mssg.sendToTarget();
                   continue;
               }
               Log.d("VaultMedia", String.valueOf(uiHandler == null) + " uiHandler null?");
           } while (cursor.moveToNext());

            completeMoveTask(shouldPostInsufficientSpace);
    }

    boolean getFileSpaceAvailability(String path){
            StatFs fileStats = new StatFs(Environment.getExternalStorageDirectory().getPath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                File originalFile = new File(path);
                if (originalFile.exists()) {
                    if (fileStats.getAvailableBytes() > (originalFile.length() + 10_24_000)) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            } else {
                File originalFile = new File(path);
                if (originalFile.exists()) {
                    long availableBytes = fileStats.getAvailableBlocks() * fileStats.getBlockSize();
                    if (availableBytes > (originalFile.length() + 10_24_000)) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
    }

    private boolean copyMediaFile(String mediaPath, String destPath) throws IOException{
        BufferedInputStream buffInput = null;
        BufferedOutputStream buffOutput = null;
        boolean mediaCopied = false;
        try {
            File originalFile = new File(mediaPath);
            if(originalFile.exists()){
            buffInput = new BufferedInputStream(new FileInputStream(originalFile));
            }
            else{
                return false;
            }
            File file = new File(destPath);
            boolean destCreated = false;
            if(!file.getParentFile().exists()) {
                destCreated = file.getParentFile().mkdirs();
            }else{
                destCreated = true;
            }
            if(destCreated && file.exists()){
                buffOutput = new BufferedOutputStream(new FileOutputStream(destPath));
                byte[] buffer = new byte[1024];
                int length;
                while((length = buffInput.read(buffer))>0){
                    buffOutput.write(buffer,0,length);
                }
                return true;
            }
             if(destCreated && !file.exists()){
                boolean created = file.createNewFile();
                if(created) {
                    buffOutput = new BufferedOutputStream(new FileOutputStream(file));
                    byte[] buffer = new byte[1024];
                    int length;
                    while((length = buffInput.read(buffer))>0){
                        buffOutput.write(buffer,0,length);
                    }
                    return true;
                }
                 else{
                    return false;
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            if(buffInput!=null){
                buffInput.close();
            }
            if (buffOutput!=null){
                buffOutput.close();
            }
        }
        return mediaCopied;
    }

    private Bitmap getThumbnail(String path){
        switch (mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return ThumbnailUtils.extractThumbnail(getImageBitmap(path),viewWidth,viewHeight);

            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return getVideoBitmap(path);

            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return getAudioBitmap(path);
        }
        return null;
    }

    private Bitmap getImageBitmap(String path){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize = calculateImageSampleSize(options,viewWidth,viewHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path,options);
    }

    private int calculateImageSampleSize(BitmapFactory.Options options,int reqWidth, int reqHeight){
        int optionWidth = options.outWidth;
        int optionHeight = options.outHeight;
        int inSample = 1;
        if(optionWidth>reqWidth || optionHeight>reqHeight){
            int heightRatio= Math.round((float)optionHeight/ (float)reqHeight);
            int widthRatio= Math.round((float)optionWidth/ (float)reqWidth);
            inSample = widthRatio<heightRatio ? widthRatio : heightRatio;
        }
        return inSample;
    }

    private Bitmap getVideoBitmap(String path){
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        Bitmap bitmap;
        try {
            metadataRetriever.setDataSource(path);
            bitmap = metadataRetriever.getFrameAtTime(5000000);
        }finally {
            metadataRetriever.release();
        }
        return bitmap;
    }

    private Bitmap getAudioBitmap(String path){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        byte[] bitmapByteData;
        try{
            retriever.setDataSource(path);
            bitmapByteData = retriever.getEmbeddedPicture();
        }finally {
            retriever.release();
        }
        if(bitmapByteData!=null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bitmapByteData,0,bitmapByteData.length,options);
            options.inSampleSize = calculateImageSampleSize(options, viewWidth, viewHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeByteArray(bitmapByteData,0,bitmapByteData.length,options);
        }
        return null;
    }

    private String getMediaFolder(String mediaType){
        switch(mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return ".image";
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return ".video";
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return ".audio";
        }
        return null;
    }

    private boolean deleteOriginalMedia(String path){
        File deletePath = new File(path);
        if(deletePath.exists()){
            return deletePath.delete();
        }
        return false;
    }

    private void deleteFailedFiles(String deletePath){
        File deleteDummy = new File(deletePath);
        if(deleteDummy.exists()){
            deleteDummy.delete();
        }
    }

    private boolean renameVaultMedia(String path, String to){
        File renamePath = new File(path);
        File toPath = new File(to);
        Log.d("VaultMedia",renamePath.getAbsolutePath());
        Log.d("VaultMedia",toPath.getAbsolutePath());
        if(renamePath.exists() && toPath.getParentFile().exists()){
            return renamePath.renameTo(toPath);
        }
        return false;
    }
    private boolean sendScanBroadcast(){
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,Uri.parse("file://"+Environment.getExternalStorageDirectory())));
        return true;
    }

    private void completeMoveTask(boolean isInsufficientSpace){
        if(isInsufficientSpace){
            SelectedMediaModel.getInstance().getSelectedMediaIdList().clear();
            SelectedMediaModel.getInstance().getSelectedMediaFileNameList().clear();
            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MOVE_INSUFFICIENT_SPACE;
            mssg.sendToTarget();
        }else{
            SelectedMediaModel.getInstance().getSelectedMediaIdList().clear();
            SelectedMediaModel.getInstance().getSelectedMediaFileNameList().clear();
            mssg = uiHandler.obtainMessage();
            mssg.what = MediaMoveService.MEDIA_MOVE_COMPLETED;
            mssg.sendToTarget();
        }
    }

     void closeTask(){
        if(context!=null){
            context = null;
            contentResolver = null;
            vaultDb = null;
            vaultDatabaseHelper.close();
            uiHandler = null;
        }
    }
}
