package com.example.q.pocketmusic.module.home.ask.publish;

import android.content.Context;
import android.text.TextUtils;

import com.example.q.pocketmusic.callback.ToastUpdateListener;
import com.example.q.pocketmusic.config.Constant;
import com.example.q.pocketmusic.model.bean.MyUser;
import com.example.q.pocketmusic.model.bean.ask.AskSongPost;
import com.example.q.pocketmusic.callback.IBaseView;
import com.example.q.pocketmusic.callback.ToastSaveListener;
import com.example.q.pocketmusic.module.common.BaseActivity;
import com.example.q.pocketmusic.module.common.BasePresenter;
import com.example.q.pocketmusic.util.CheckUserUtil;
import com.example.q.pocketmusic.util.LogUtils;
import com.example.q.pocketmusic.util.MyToast;

/**
 * Created by Cloud on 2016/11/14.
 */

public class AskSongPresenter extends BasePresenter {
    private IView activity;
    private Context context;

    public AskSongPresenter(IView activity, Context context) {
        this.activity = activity;
        this.context = context;
    }

    public void askForSong(String title, final String content, final MyUser user) {
        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(title)) {
            MyToast.showToast(context, "请将信息填完整~");
            return;
        }
        if (!CheckUserUtil.checkUserContribution((BaseActivity) context, Constant.REDUCE_CONTRIBUTION_ASK)) {
            MyToast.showToast(context,"贡献度不够哦~");
            return;
        }
        activity.showLoading(true);
        AskSongPost askSongPost = new AskSongPost(user, title, content);
        askSongPost.save(new ToastSaveListener<String>(context, activity) {
            @Override
            public void onSuccess(String s) {
                user.increment("contribution",-Constant.REDUCE_CONTRIBUTION_ASK);
                user.update(new ToastUpdateListener(context,activity) {
                    @Override
                    public void onSuccess() {
                        MyToast.showToast(context, Constant.REDUCE_CONTRIBUTION_BASE + Constant.REDUCE_CONTRIBUTION_ASK);
                        activity.showLoading(false);
                        activity.setAskResult(Constant.SUCCESS);
                        activity.finish();
                    }
                });
            }
        });


    }

    public interface IView extends IBaseView {
        void finish();

        void setAskResult(Integer success);
    }
}