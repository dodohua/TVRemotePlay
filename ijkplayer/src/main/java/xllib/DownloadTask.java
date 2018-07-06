package xllib;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.xunlei.downloadlib.XLTaskHelper;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfo;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import player.XLVideoPlayActivity;

/**
 * Created by kingt on 2018/2/2.
 */

public class DownloadTask {
    private Context context;
    public interface DownloadTaskBaseDirGetter{
        public abstract File getBaseDir();
    }

    private final static String TAG = "DownloadTask";
    private static File defaultBaseDir = new File(Environment.getExternalStorageDirectory(), "xlplayer");
    private static DownloadTaskBaseDirGetter downloadTaskBaseDirGetter;
    public static void setBaseDirGetter(DownloadTaskBaseDirGetter baseDirGetter){
        DownloadTask.downloadTaskBaseDirGetter = baseDirGetter;
    }
    private File getBaseDir(){
        DownloadTaskBaseDirGetter baseDirGetter = DownloadTask.downloadTaskBaseDirGetter;
        if(baseDirGetter != null){
            return baseDirGetter.getBaseDir();
        }else {
            return defaultBaseDir;
        }
    }

    public DownloadTask(Context context){
        this.context = context;
    }

    private String url;
    private String name;
    private long taskId;
    private String localSavePath;
    private boolean mIsLiveMedia;
    private boolean isNetworkDownloadTask;
    private boolean isLocalMedia;
    private TorrentInfo torrentInfo;
    private int[] torrentMediaIndexs = null;
    private int[] torrentUnmediaIndexs = null;
    private int currentPlayMediaIndex = 0;
    private List<PlayListItem> playList = new ArrayList<>();

    public boolean isLiveMedia(){
        return this.mIsLiveMedia;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        //删除旧任务及文件
        this.stopTask();

        this.url = url;
        this.playList.clear();
        this.mIsLiveMedia = FileUtils.isLiveMedia(this.url);
        this.isNetworkDownloadTask = !this.mIsLiveMedia && FileUtils.isNetworkDownloadTask(this.url);
        this.name = this.mIsLiveMedia ? FileUtils.getWebMediaFileName(this.url) :
                     this.isNetworkDownloadTask ? XLTaskHelper.instance(this.context).getFileName(this.url) : FileUtils.getFileName(this.url);
        this.localSavePath = getBaseDir().toString() + "/";
        this.isLocalMedia = !this.mIsLiveMedia && !this.isNetworkDownloadTask && FileUtils.isMediaFile(this.name);
        this.torrentInfo = null;
        this.torrentMediaIndexs = null;
        this.torrentUnmediaIndexs = null;
        this.currentPlayMediaIndex = 0;
        if(this.isLocalMedia){
            playList.add(new PlayListItem(this.name, 0, new File(this.getUrl()).length()));
        }else if(this.mIsLiveMedia || this.isNetworkDownloadTask){
            playList.add(new PlayListItem(this.name, 0, 0L));
        } else if (".torrent".equals(FileUtils.getFileExt(this.name))) {
            this.torrentInfo = XLTaskHelper.instance(this.context).getTorrentInfo(this.url);
            this.initTorrentIndexs();
        }
    }

    private void initTorrentIndexs(){
        this.currentPlayMediaIndex = -1;
        if(this.torrentInfo != null  && this.torrentInfo.mSubFileInfo != null){
            ArrayList<Integer> mediaIndexs = new ArrayList<>();
            ArrayList<Integer> unmediaIndexs = new ArrayList<>();
            for (int i = 0; i < torrentInfo.mSubFileInfo.length; i++) {
                TorrentFileInfo torrentFileInfo = torrentInfo.mSubFileInfo[i];
                if(FileUtils.isMediaFile(torrentFileInfo.mFileName)){
                    mediaIndexs.add(torrentFileInfo.mFileIndex);
                    playList.add(new PlayListItem(
                            TextUtils.isEmpty(torrentFileInfo.mSubPath) ? torrentFileInfo.mFileName :
                                    torrentFileInfo.mSubPath + "/" + torrentFileInfo.mFileName,
                            torrentFileInfo.mFileIndex, torrentFileInfo.mFileSize));
                }else{
                    unmediaIndexs.add(torrentFileInfo.mFileIndex);
                }
            }
            this.torrentMediaIndexs = new int[mediaIndexs.size()];
            this.torrentUnmediaIndexs = new int[unmediaIndexs.size()];
            for(int i=0; i<mediaIndexs.size(); i++)this.torrentMediaIndexs[i] = mediaIndexs.get(i);
            for(int i=0; i<unmediaIndexs.size(); i++)this.torrentUnmediaIndexs[i] = unmediaIndexs.get(i);
            this.currentPlayMediaIndex = this.torrentMediaIndexs.length > 0 ? this.torrentMediaIndexs[0] : -1;
        }
    }

    private int[] getTorrentDeselectedIndexs(){
        int[] indexs = new int[this.torrentUnmediaIndexs.length + this.torrentMediaIndexs.length -1];
        int offset = 0;
        for(int idx : this.torrentMediaIndexs){
            if(idx != this.currentPlayMediaIndex) {
                indexs[offset++] = idx;
            }
        }
        for(int idx : this.torrentUnmediaIndexs){
            indexs[offset ++] = idx;
        }
        return indexs;
    }

    public List<PlayListItem> getPlayList(){
        return this.playList;
    }

    public String getPlayUrl(){
        String urlMd5 = FileUtils.getMD5Str(this.url);
        String videoSavePath = localSavePath+urlMd5+"/";
        if(this.isLocalMedia || this.mIsLiveMedia){
            return this.getUrl();
        }else if(this.taskId != 0L){
            if(this.isNetworkDownloadTask){
                String uri = XLTaskHelper.instance(this.context).getLoclUrl(videoSavePath + this.name);
                try {
                    uri = URLDecoder.decode(uri, "utf-8");
                }catch (Exception e){
                    e.printStackTrace();
                }
                return uri;
            }else if(this.torrentInfo != null && this.currentPlayMediaIndex != -1){
                for(PlayListItem item : getPlayList()){
                    if(item.getIndex() == this.currentPlayMediaIndex){
                        String uri = XLTaskHelper.instance(this.context).getLoclUrl(videoSavePath+ item.getName());
                        try {
                            uri = URLDecoder.decode(uri, "utf-8");
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                        return uri;
                    }
                }
            }
        }
        return null;
    }
    //发通知
    public Handler mHandler;
    private void sendReplay(){
       if (isMagnet){
           XLTaskInfo taskInfo = XLTaskHelper.instance(this.context).getTaskInfo(taskId);
           String urlMd5 = FileUtils.getMD5Str(this.url);
           String videoSavePath = localSavePath+urlMd5+"/";
           String torrentPath = videoSavePath+XLTaskHelper.instance(this.context).getFileName(this.url);
           Log.e(TAG, "sendReplay:种子路径"+torrentPath);
           this.setUrl(torrentPath);
           this.startTask();
       }
       mHandler.sendEmptyMessageDelayed(XLVideoPlayActivity.MESSAGE_RESTART_PLAY, 1500);
    }
    public boolean changePlayItem(int index){
        if(this.torrentInfo != null && index != this.currentPlayMediaIndex){
            this.currentPlayMediaIndex = index;
            if(this.taskId != 0L){
                this.stopTask();
                return this.startTask();
            }
            return true;
        }
        return false;
    }
    public PlayListItem getCurrentPlayItem(){
        for(PlayListItem item : this.getPlayList()){
            if(item.getIndex() == this.currentPlayMediaIndex) return item;
        }
        return null;
    }

    Handler downloHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 0) {
                if (loopCount>10){
                    sendReplay();
                    return;
                }
                long taskId = (long) msg.obj;
                XLTaskInfo taskInfo = XLTaskHelper.instance(DownloadTask.this.context).getTaskInfo(taskId);
                if (taskInfo.mFileSize>0)
                {
                    if (taskInfo.mFileSize == taskInfo.mDownloadSize)
                    {
                        sendReplay();
                        return;
                    }
                }
                Log.e(TAG, "handleMessage: taskInfo.mFileSize"+taskInfo.mFileSize +"taskInfo.mDownloadSize"+taskInfo.mDownloadSize);

                loopCount++;
                downloHandler.sendMessageDelayed(downloHandler.obtainMessage(0,taskId),1000);
            }
        }
    };

    private boolean isMagnet = false;
    private int loopCount = 0;
    public boolean startTask(){
        if(TextUtils.isEmpty(this.url) || this.taskId != 0L){
            return false;
        }
        File theDir = new File(localSavePath);
        if (!theDir.exists()) {
            try{
                theDir.mkdir();
            }
            catch(SecurityException se){
                se.printStackTrace();
            }
        }
        String urlMd5 = FileUtils.getMD5Str(this.url);
        String videoSavePath = localSavePath+urlMd5+"/";
        theDir = new File(videoSavePath);
        if (!theDir.exists()) {
            try{
                theDir.mkdir();
            }
            catch(SecurityException se){
                se.printStackTrace();
            }
        }
        if(this.isNetworkDownloadTask){
            isMagnet = false;
            if(this.url.toLowerCase().startsWith("magnet:?")){
                isMagnet = true;
                try {
                    Log.e(TAG, "magnet: 磁力链接");
                    taskId = XLTaskHelper.instance(this.context).addMagentTask(this.url, videoSavePath, null);
                    Log.e(TAG, "startTask: taskId"+taskId);
                    if (taskId!=-1){
                        loopCount = 0;
                        downloHandler.sendMessageDelayed(downloHandler.obtainMessage(0,taskId),1000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "startTask: 失败");
                }
            }else {
                try {
                    taskId = XLTaskHelper.instance(this.context).addThunderTask(this.url, videoSavePath, null);
                } catch (Exception e) {
                    Log.e(TAG, "startTask: 失败");
                }
            }
        }else if(this.torrentInfo != null) {
            if(this.currentPlayMediaIndex != -1) {
                try {
                    taskId = XLTaskHelper.instance(this.context).addTorrentTask(this.url, videoSavePath, this.getTorrentDeselectedIndexs());
                } catch (Exception e) {
                    Log.e(TAG, "startTask: 失败");
                }
            }
        }else {
            taskId = this.isLocalMedia || this.mIsLiveMedia ? -9999L : 0L;
        }
        Log.d(TAG, "startTask(" + this.url + "), taskId = " + taskId + ", index = " + currentPlayMediaIndex);
        boolean result = (taskId != 0L&&taskId != -1);
        if (!isMagnet&&result)
        {
            //通知播放
            sendReplay();
        }
        return  taskId != 0L;
    }

    public void stopTask(){
        loopCount = 0;
        if(this.taskId != 0L){
            if(!this.isLocalMedia && !this.mIsLiveMedia) {
                String urlMd5 = FileUtils.getMD5Str(this.url);
                String videoSavePath = localSavePath+urlMd5+"/";
                XLTaskHelper.instance(this.context).deleteTask(this.taskId, videoSavePath);
            }
            Log.d(TAG, "stopTask(" + this.url + "), taskId = " + taskId);
            this.taskId = 0L;
        }
    }
    public XLTaskInfo getTaskInfo(){
        return this.taskId == 0L || this.isLocalMedia || this.mIsLiveMedia ? null : XLTaskHelper.instance(this.context).getTaskInfo(this.taskId);
    }

    public String getName() {
        return name;
    }
}
