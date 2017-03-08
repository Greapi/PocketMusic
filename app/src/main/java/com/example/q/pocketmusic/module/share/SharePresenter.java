package com.example.q.pocketmusic.module.share;

import android.content.Context;
import android.database.SQLException;
import android.text.TextUtils;

import com.example.q.pocketmusic.callback.IBasePresenter;
import com.example.q.pocketmusic.callback.ToastQueryListListener;
import com.example.q.pocketmusic.callback.ToastSaveListener;
import com.example.q.pocketmusic.callback.ToastUpdateListener;
import com.example.q.pocketmusic.config.Constant;
import com.example.q.pocketmusic.model.bean.MyUser;
import com.example.q.pocketmusic.model.bean.local.Img;
import com.example.q.pocketmusic.model.bean.local.LocalSong;
import com.example.q.pocketmusic.model.bean.share.SharePic;
import com.example.q.pocketmusic.model.bean.share.ShareSong;
import com.example.q.pocketmusic.model.db.LocalSongDao;
import com.example.q.pocketmusic.callback.IBaseView;
import com.example.q.pocketmusic.module.common.BasePresenter;
import com.example.q.pocketmusic.util.LogUtils;
import com.example.q.pocketmusic.util.MyToast;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.ForeignCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.QueryListListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UploadBatchListener;
import cn.finalteam.galleryfinal.FunctionConfig;
import cn.finalteam.galleryfinal.GalleryFinal;
import cn.finalteam.galleryfinal.model.PhotoInfo;

/**
 * Created by Cloud on 2016/11/10.
 */

public class SharePresenter extends BasePresenter {
    private IView activity;
    private Context context;
    private int mNumberPic;//图片数量
    private String[] filePaths;//本地图片路径
    private MyUser user;

    public MyUser getUser() {
        return user;
    }

    public SharePresenter(IView activity, Context context, MyUser user) {
        this.activity = activity;
        this.context = context;
        this.user = user;
    }

    //打开图片浏览器，存储数量和本地路径
    public void openPicture() {
        FunctionConfig config = new FunctionConfig.Builder()
                .setMutiSelectMaxSize(8)
                .build();
        GalleryFinal.openGalleryMuti(1, config, new GalleryFinal.OnHanlderResultCallback() {
            @Override
            public void onHanlderSuccess(int reqeustCode, List<PhotoInfo> resultList) {
                //初始化
                mNumberPic = 0;
                int size = resultList.size();
                if (reqeustCode == 1) {
                    filePaths = new String[size];
                    //得到所有的本地曲谱路径
                    for (int i = 0; i < resultList.size(); i++) {
                        String url = resultList.get(i).getPhotoPath();
                        filePaths[i] = url;
                        mNumberPic++;
                        LogUtils.e(TAG, url);
                    }
                    activity.setSelectPicResult(mNumberPic, filePaths, null);
                }
            }

            @Override
            public void onHanlderFailure(int requestCode, String errorMsg) {

            }
        });
    }

    public void upLoad(final String name, final String author, final String content) {
        if ((!TextUtils.isEmpty(name)) && (!TextUtils.isEmpty(author)) && (!TextUtils.isEmpty(content)) && (mNumberPic > 0)) {
            //上传服务器,弹出Dialog，成功后finish，弹出Toast;
            activity.showLoading(true);
            //先批量上传图片
            BmobFile.uploadBatch(filePaths, new UploadBatchListener() {
                @Override
                public void onSuccess(List<BmobFile> list, List<String> list1) {
                    //文件成功之后再添加数据
                    if (filePaths.length == list1.size()) {
                        shareSong(name, content, list1);
                    }
                }

                @Override
                public void onProgress(int i, int i1, int i2, int i3) {
                    //批量上传中
                }

                @Override
                public void onError(int i, String s) {
                    MyToast.showToast(context, "上传图片失败了~~~~");
                }
            });
        } else {
            MyToast.showToast(context, "请添加完整信息");
        }
    }

    //上传/分享乐曲
    private void shareSong(String name, final String content, final List<String> list1) {
        final ShareSong shareSong = new ShareSong(user, name, content);
        activity.showLoading(true);
        //添加分享曲谱记录
        shareSong.save(new ToastSaveListener<String>(context, activity) {
            @Override
            public void onSuccess(String s) {
                List<BmobObject> sharePics = new ArrayList<>();
                for (int i = 0; i < list1.size(); i++) {
                    SharePic sharePic = new SharePic(shareSong, list1.get(i));
                    sharePics.add(sharePic);
                }
                //批量添加分享图片记录
                new BmobBatch().insertBatch(sharePics).doBatch(new ToastQueryListListener<BatchResult>(context, activity) {

                    @Override
                    public void onSuccess(List<BatchResult> list) {
                        user.increment("contribution", Constant.ADD_CONTRIBUTION_UPLOAD);//原子操作
                        user.update(new ToastUpdateListener(context, activity) {
                            @Override
                            public void onSuccess() {
                                MyToast.showToast(context, Constant.ADD_CONTRIBUTION_BASE+ (Constant.ADD_CONTRIBUTION_UPLOAD));
                                activity.showLoading(false);
                                activity.finish();
                            }
                        });
                    }
                });
            }
        });
    }


    public void getPicAndName(LocalSong localSong) {
        LocalSongDao localSongDao = new LocalSongDao(context);
        ForeignCollection<Img> imgs = localSongDao.findBySongId(localSong.getId()).getImgs();
        CloseableIterator<Img> iterator = imgs.closeableIterator();
        int i = 0;
        filePaths = new String[imgs.size()];
        try {
            while (iterator.hasNext()) {
                Img img = iterator.next();
                filePaths[i++] = img.getUrl();
            }
            mNumberPic = filePaths.length;
            activity.setSelectPicResult(filePaths.length, filePaths, localSong.getName());
        } finally {
            try {
                iterator.close();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    //查看图片
    public void checkPic(String path) {
        LogUtils.e(TAG, path);
        GalleryFinal.openEdit(2, path, new GalleryFinal.OnHanlderResultCallback() {
            @Override
            public void onHanlderSuccess(int requestCode, List<PhotoInfo> resultList) {

            }

            @Override
            public void onHanlderFailure(int requestCode, String errorMsg) {

            }
        });
    }


    public interface IView extends IBaseView {

        void finish();

        void setSelectPicResult(int mNumberPic, String[] filePaths, String name);

        void showLoading(boolean isShow);
    }
}