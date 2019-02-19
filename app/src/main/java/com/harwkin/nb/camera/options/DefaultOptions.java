package com.harwkin.nb.camera.options;

import android.content.Context;
import android.net.Uri;

import com.harwkin.nb.camera.FileConstants;
import com.harwkin.nb.camera.FileUtil;
import com.harwkin.nb.camera.type.OpenType;
import com.quanyan.base.util.MD5Utils;

import java.io.File;

public class DefaultOptions {
    public static final int X = 1;
    public static final int Y = 1;
    public static final int width = 300;
    public static final int height = 300;
    public static final int maxSelect = 5;
    public static final OpenType OPEN_TYPE = OpenType.OPEN_CAMERA;
    public static final String IMG_PREFIX_DEFAULT = "IMG";
    public static final String IMG_TEMP_DEFAULT = "TMP";
    public static final String THUMBNAIL_FILE_FORMAT = MD5Utils.toMD5("_300x300");
    public static float GROWSMALLSCALE = 0.15f;
    public static float WIDESMALLSCALE = 10.5f;
    private volatile static DefaultOptions options;

    public void setFormat(String format) {
        this.format = format;
    }

    private String format = ".jpg";
    private Context mContext;

    private DefaultOptions(Context context) {
        this.mContext = context;
    }

    public static DefaultOptions getInstance(Context context) {
        if (options == null) {
            synchronized (DefaultOptions.class) {
                if (options == null)
                    options = new DefaultOptions(context);
            }
        }
        return options;
    }

    public int getMaxWidth() {
        return dip2px(300);
    }

    public int getMaxHeight() {
        return dip2px(300);
    }

    public int getSmallWidth() {
        return dip2px(150);
    }

    public int getSmallHeigth() {
        return dip2px(150);
    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private int dip2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public File getDefaultTempFile() {
        StringBuffer buffer = new StringBuffer(
                FileConstants.getImageDirPath(mContext));
        FileUtil.mkdirs(buffer.toString());
        buffer.append(MD5Utils.toMD5(IMG_TEMP_DEFAULT));
        buffer.append(MD5Utils.toMD5(System.currentTimeMillis() + "") + format);
        return FileUtil.createFile(buffer.toString());
    }

    public File getDefaultThumbnailFile(String tempfile) {
        String replace = tempfile + THUMBNAIL_FILE_FORMAT;
        return FileUtil.createFile(replace);
    }

    public Uri getDefaultFileUri() {
        StringBuffer buffer = new StringBuffer(
                FileConstants.getImageDirPath(mContext));
        FileUtil.mkdirs(buffer.toString());
        buffer.append(MD5Utils.toMD5(IMG_PREFIX_DEFAULT));
        buffer.append(MD5Utils.toMD5(System.currentTimeMillis() + "") + format);
        return Uri.fromFile(FileUtil.createFile(buffer.toString()));
    }

    public Uri getDefaultGifFileUri() {
        StringBuffer buffer = new StringBuffer(
                FileConstants.getImageDirPath(mContext));
        FileUtil.mkdirs(buffer.toString());
        buffer.append(MD5Utils.toMD5(IMG_PREFIX_DEFAULT));
        buffer.append(MD5Utils.toMD5(System.currentTimeMillis() + "") + ".gif");
        return Uri.fromFile(FileUtil.createFile(buffer.toString()));
    }
}
