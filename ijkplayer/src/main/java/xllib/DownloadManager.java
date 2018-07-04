package xllib;

import android.content.Context;

import com.xunlei.downloadlib.XLDownloadManager;
import com.xunlei.downloadlib.XLTaskHelper;

/**
 * Created by kingt on 2018/2/2.
 */

public class DownloadManager {
    private Context context;

    private DownloadManager(){

    }

    private static volatile DownloadManager instance = null;

    public static DownloadManager instance() {
        if (instance == null) {
            synchronized (DownloadManager.class) {
                if (instance == null) {
                    instance = new DownloadManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context){
        this.context = context;
        this.downloadTask = new DownloadTask(this.context);
        XLTaskHelper.init(this.context);
    }

    private DownloadTask downloadTask;
    public DownloadTask taskInstance(){
        return this.downloadTask;
    }
}
