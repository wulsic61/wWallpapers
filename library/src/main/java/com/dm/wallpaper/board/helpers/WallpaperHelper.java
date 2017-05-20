package com.dm.wallpaper.board.helpers;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.danimahardhika.android.helpers.core.ColorHelper;
import com.danimahardhika.android.helpers.core.FileHelper;
import com.danimahardhika.android.helpers.core.WindowHelper;
import com.danimahardhika.cafebar.CafeBar;
import com.danimahardhika.cafebar.CafeBarDuration;
import com.danimahardhika.cafebar.CafeBarTheme;
import com.dm.wallpaper.board.R;
import com.dm.wallpaper.board.activities.WallpaperBoardActivity;
import com.dm.wallpaper.board.preferences.Preferences;
import com.dm.wallpaper.board.utils.ImageConfig;
import com.dm.wallpaper.board.utils.LogUtil;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.danimahardhika.android.helpers.permission.PermissionHelper.isStorageGranted;

/*
 * Wallpaper Board
 *
 * Copyright (c) 2017 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class WallpaperHelper {

    public static final String IMAGE_EXTENSION = ".jpeg";

    public static File getDefaultWallpapersDirectory(@NonNull Context context) {
        try {
            if (Preferences.get(context).getWallsDirectory().length() == 0) {
                return new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES) +"/"+
                        context.getResources().getString(R.string.app_name));
            }
            return new File(Preferences.get(context).getWallsDirectory());
        } catch (Exception e) {
            return new File(context.getFilesDir().toString() +"/Pictures/"+
                    context.getResources().getString(R.string.app_name));
        }
    }

    public static String getThumbnailUrl(@NonNull Context context, String url, String thumbUrl) {
        if (thumbUrl.equals(url) && WallpaperBoardActivity.sRszIoAvailable) {
            if (thumbUrl.contains("drive.google.com"))
                return thumbUrl;
            return getRszIoThumbnailUrl(context, url);
        }
        return thumbUrl;
    }

    private static String getRszIoThumbnailUrl(@NonNull Context context, String url) {
        String rszIoUrl = url.replaceFirst("https://|http://", "");
        ImageSize imageSize = ImageConfig.getThumbnailSize(context);
        return "https://rsz.io/" +rszIoUrl+ "?height=" +imageSize.getWidth();
    }

    private static String getWallpaperUri(@NonNull Context context, String url, String filename) {
        if (isStorageGranted(context)) {
            File directory = getDefaultWallpapersDirectory(context);
            if (new File(directory + File.separator + filename).exists()) {
                return "file://" + directory + File.separator + filename;
            }
        }
        return url;
    }

    public static void downloadWallpaper(@NonNull Context context, @ColorInt int color,
                                         String link, String name) {

        File cache = ImageLoader.getInstance().getDiskCache().get(link);
        if (cache != null) {
            File target = new File(getDefaultWallpapersDirectory(context).toString()
                    + File.separator + name + WallpaperHelper.IMAGE_EXTENSION);

            if (com.danimahardhika.android.helpers.core.FileHelper.copy(cache, target)) {
                wallpaperSaved(context, color, target);

                context.sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(
                        new File(target.toString()))));
                return;
            }
        }

        new AsyncTask<Void, Integer, Boolean>() {

            MaterialDialog dialog;
            HttpURLConnection connection;
            File output;
            File file;
            int fileLength;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                output = WallpaperHelper.getDefaultWallpapersDirectory(context);
                file = new File(output.toString() + File.separator + name + WallpaperHelper.IMAGE_EXTENSION);

                MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
                builder.content(R.string.wallpaper_downloading)
                        .typeface("Font-Medium.ttf", "Font-Regular.ttf")
                        .widgetColor(color)
                        .progress(true, 0);
                dialog = builder.build();
                dialog.setOnDismissListener(dialog1 -> {
                    try {
                        if (connection != null) connection.disconnect();
                    } catch (Exception ignored) {}
                    cancel(true);
                });
                dialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                while (!isCancelled()) {
                    try {
                        Thread.sleep(1);
                        if (!output.exists())
                            if (!output.mkdirs())
                                return false;

                        URL url = new URL(link);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(15000);

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            fileLength = connection.getContentLength();
                            InputStream stream = connection.getInputStream();
                            OutputStream output = new FileOutputStream(file.toString());

                            byte data[] = new byte[1024];
                            long total = 0;
                            int count;
                            while ((count = stream.read(data)) != -1) {
                                total += count;
                                if (fileLength > 0)
                                    publishProgress((int) (total * 100 / fileLength));
                                output.write(data, 0, count);
                            }

                            output.flush();
                            output.close();
                            stream.close();
                            return true;
                        }
                    } catch (Exception e) {
                        LogUtil.e(Log.getStackTraceString(e));
                        return false;
                    }
                }
                return false;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                int downloaded = fileLength / 1014;
                String size = String.valueOf(values[0] * fileLength/1024/100) + " KB" +
                        String.valueOf(fileLength == 0 ? "" : "/" + downloaded + " KB");
                String downloading = context.getResources().getString(
                        R.string.wallpaper_downloading);
                String text = downloading +"\n"+ size + "";
                dialog.setContent(text);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                if (((AppCompatActivity) context).isFinishing()) return;

                if (file != null) file.delete();

                Toast.makeText(context,
                        context.getResources().getString(R.string.wallpaper_download_cancelled),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                dialog.dismiss();
                if (aBoolean) {
                    context.sendBroadcast(new Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(
                            new File(file.toString()))));

                    wallpaperSaved(context, color, file);
                } else {
                    Toast.makeText(context,
                            context.getResources().getString(R.string.wallpaper_download_failed),
                            Toast.LENGTH_LONG).show();
                }
            }

        }.execute();
    }

    private static void wallpaperSaved(@Nullable Context context, @ColorInt int color, @NonNull File file) {
        if (context == null) return;

        String downloaded = context.getResources().getString(
                R.string.wallpaper_downloaded);

        if (Preferences.get(context).getWallsDirectory().length() == 0)
            Preferences.get(context).setWallsDirectory(file.getParent());

        CafeBar.builder(context)
                .theme(new CafeBarTheme.Custom(ColorHelper.getAttributeColor(context, R.attr.card_background)))
                .duration(CafeBarDuration.MEDIUM.getDuration())
                .fitSystemWindow()
                .maxLines(4)
                .typeface("Font-Regular.ttf", "Font-Bold.ttf")
                .content(downloaded + " " + file.toString())
                .icon(R.drawable.ic_toolbar_download)
                .neutralText(R.string.open)
                .neutralColor(color)
                .onNeutral(cafeBar -> {
                    Uri uri = FileHelper.getUriFromFile(context, context.getPackageName(), file);
                    if (uri == null) return;

                    context.startActivity(new Intent()
                            .setAction(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "image/*")
                            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));

                    cafeBar.dismiss();
                })
                .show();
    }

    private static ImageSize getTargetSize(@NonNull Context context) {
        Point point = WindowHelper.getScreenSize(context);
        int targetHeight = point.y;
        int targetWidth = point.x;

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            targetHeight = point.x;
            targetWidth = point.y;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            int statusBarHeight = WindowHelper.getStatusBarHeight(context);
            int navBarHeight = WindowHelper.getNavigationBarHeight(context);
            targetHeight += (statusBarHeight + navBarHeight);
        }
        return new ImageSize(targetWidth, targetHeight);
    }

    @Nullable
    private static RectF getScaledRectF(@Nullable RectF rectF, float heightFactor, float widthFactor) {
        if (rectF == null) return null;

        RectF scaledRectF = new RectF(rectF);
        scaledRectF.top *= heightFactor;
        scaledRectF.bottom *= heightFactor;
        scaledRectF.left *= widthFactor;
        scaledRectF.right *= widthFactor;
        return scaledRectF;
    }

    public static void applyWallpaper(@NonNull Context context, @Nullable RectF rectF,
                                      @ColorInt int color, String url, String name) {
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.widgetColor(color)
                .typeface("Font-Medium.ttf", "Font-Regular.ttf")
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .content(R.string.wallpaper_applying);
        final MaterialDialog dialog = builder.build();

        String imageUri = getWallpaperUri(context, url, name + WallpaperHelper.IMAGE_EXTENSION);

        ImageSize imageSize = getTargetSize(context);
        LogUtil.d("target bitmap: " +imageSize.getWidth() +" x "+ imageSize.getHeight());

        if (rectF != null && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            Point point = WindowHelper.getScreenSize(context);
            int height = point.y - WindowHelper.getStatusBarHeight(context) - WindowHelper.getNavigationBarHeight(context);
            float heightFactor = (float) imageSize.getHeight() / (float) height;
            rectF = getScaledRectF(rectF, heightFactor, 1f);
        }

        prepareBitmap(context, dialog, imageUri, rectF, imageSize);
    }

    private static void prepareBitmap(Context context, MaterialDialog dialog, String imageUri,
                                      RectF rectF, ImageSize imageSize) {
        new AsyncTask<Void, Void, Boolean>() {

            ImageSize adjustedSize = imageSize;
            RectF adjustedRecF = rectF;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (dialog.isShowing()) return;

                dialog.setCancelable(false);
                dialog.setContent(R.string.wallpaper_loading);
                dialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    Thread.sleep(1);
                    int height = 0;
                    int width = 0;
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;

                    if (imageUri.contains("file://")) {
                        BitmapFactory.decodeStream(new FileInputStream(imageUri.replace("file://", "")), null, options);

                        height = options.outHeight;
                        width = options.outWidth;
                    } else {
                        URL url = new URL(imageUri);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(15000);

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            InputStream stream = connection.getInputStream();
                            BitmapFactory.decodeStream(stream, null, options);

                            height = options.outHeight;
                            width = options.outWidth;
                        }
                    }

                    LogUtil.d("original bitmap: " + width + " x " + height);
                    if (rectF != null) {
                        LogUtil.d("original recF: " +rectF);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        float scaleFactor = (float) height / (float) imageSize.getHeight();
                        if (scaleFactor > 1f) {
                            adjustedSize = new ImageSize(width, height);
                            adjustedRecF = getScaledRectF(rectF, scaleFactor, scaleFactor);

                            LogUtil.d("adjusted bitmap: " + adjustedSize.getWidth() + " x " + adjustedSize.getHeight());

                            if (adjustedRecF != null) {
                                LogUtil.d("adjusted recF: " + adjustedRecF);
                            }
                        }
                    }
                    return true;
                } catch (Exception e) {
                    LogUtil.e(Log.getStackTraceString(e));
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                loadBitmap(context, dialog, 1, imageUri, adjustedRecF, adjustedSize);
            }
        }.execute();
    }

    private static void loadBitmap(Context context, MaterialDialog dialog, int call, String imageUri,
                                   RectF rectF, ImageSize imageSize) {
        final AsyncTask<Bitmap, Void, Boolean> setWallpaper = getWallpaperAsync(
                context, dialog, rectF);

        dialog.setCancelable(true);
        dialog.setOnDismissListener(dialogInterface -> {
            ImageLoader.getInstance().stop();
            setWallpaper.cancel(true);
        });

        ImageLoader.getInstance().handleSlowNetwork(true);
        ImageLoader.getInstance().loadImage(imageUri, imageSize,
                ImageConfig.getWallpaperOptions(), new SimpleImageLoadingListener() {

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        if (failReason.getType() == FailReason.FailType.OUT_OF_MEMORY) {
                            if (call <= 5) {
                                double scaleFactor = 1 - (0.1 * call);
                                int scaledWidth = Double.valueOf(imageSize.getWidth() * scaleFactor).intValue();
                                int scaledHeight = Double.valueOf(imageSize.getHeight() * scaleFactor).intValue();

                                RectF scaledRecF = getScaledRectF(rectF, (float) scaleFactor, (float) scaleFactor);
                                loadBitmap(context, dialog, (call + 1), imageUri, scaledRecF, new ImageSize(scaledWidth, scaledHeight));
                                return;
                            }
                        }

                        dialog.dismiss();
                        String message = context.getResources().getString(R.string.wallpaper_apply_failed);
                        message = message +": "+ failReason.getType().toString();
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        LogUtil.d("loaded bitmap: " +loadedImage.getWidth() +" x "+ loadedImage.getHeight());
                        dialog.setContent(R.string.wallpaper_applying);
                        setWallpaper.execute(loadedImage);
                    }

                    @Override
                    public void onLoadingCancelled(String imageUri, View view) {
                        dialog.dismiss();
                        Toast.makeText(context, R.string.wallpaper_apply_failed,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static AsyncTask<Bitmap, Void, Boolean> getWallpaperAsync(@NonNull Context context, MaterialDialog dialog,
                                                                      RectF rectF) {
        return new AsyncTask<Bitmap, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Bitmap... bitmaps) {
                while (!isCancelled()) {
                    try {
                        Thread.sleep(1);
                        WallpaperManager manager = WallpaperManager.getInstance(context);
                        if (bitmaps[0] != null) {
                            Bitmap bitmap = bitmaps[0];

                            if (Preferences.get(context).isWallpaperCrop() && rectF != null) {
                                Point point = WindowHelper.getScreenSize(context);

                                int targetWidth = Double.valueOf(
                                        ((double) bitmaps[0].getHeight() / (double) point.y)
                                                * (double) point.x).intValue();

                                bitmap = Bitmap.createBitmap(
                                        targetWidth,
                                        bitmaps[0].getHeight(),
                                        bitmaps[0].getConfig());
                                Paint paint = new Paint();
                                paint.setFilterBitmap(true);
                                paint.setAntiAlias(true);
                                paint.setDither(true);

                                Canvas canvas = new Canvas(bitmap);
                                canvas.drawBitmap(bitmaps[0], null, rectF, paint);
                            }

                            LogUtil.d("generated bitmap: " +bitmap.getWidth() +" x "+ bitmap.getHeight());

                            if (Preferences.get(context).isApplyLockscreen() &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
                            } else {
                                manager.setBitmap(bitmap);
                            }
                            return true;
                        }
                        return false;
                    } catch (Exception | OutOfMemoryError e) {
                        LogUtil.e(Log.getStackTraceString(e));
                        return false;
                    }
                }
                return false;
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                Toast.makeText(context, R.string.wallpaper_apply_cancelled,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                dialog.dismiss();
                if (aBoolean) {
                    CafeBar.builder(context)
                            .theme(new CafeBarTheme.Custom(ColorHelper.getAttributeColor(
                                    context, R.attr.card_background)))
                            .fitSystemWindow()
                            .content(R.string.wallpaper_applied)
                            .build().show();
                } else {
                    Toast.makeText(context, R.string.wallpaper_apply_failed,
                            Toast.LENGTH_LONG).show();
                }
            }
        };
    }
}
