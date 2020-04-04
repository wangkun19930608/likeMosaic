package cn.bearever.mosaicserver.match;

import java.util.*;

public class MatchManager {
    private static volatile MatchManager instance;
    private final Object LOCK_UID;
    private final Object LOCK_CHANNEL;
    /**
     * 申请匹配的用户列表
     */
    private Map<String, String> mPostUidMap;
    /**
     * 已经匹配的记录
     */
    private Map<String, String> mChannelMap;
    private static final String PRE_KEY = "luoming";
    private String mTime;
    private long count = 0;
    private Base64.Encoder mBase64;


    public static MatchManager getInstance() {
        if (instance == null) {
            synchronized (MatchManager.class) {
                if (instance == null) {
                    instance = new MatchManager();
                }
            }
        }
        return instance;
    }

    private MatchManager() {
        LOCK_UID = new Object();
        LOCK_CHANNEL = new Object();
        mPostUidMap = new HashMap<>();
        mChannelMap = new HashMap<>();
    }

    /**
     * 加入匹配队列
     *
     * @param uid
     */
    public void add(String uid) {
        synchronized (LOCK_UID) {
            mPostUidMap.put(uid, uid);
            setupChannel();
        }
    }

    /**
     * 为等待中的用户分配频道
     */
    private void setupChannel() {
        if (mBase64 == null) {
            mBase64 = Base64.getMimeEncoder();
        }
        Set<Map.Entry<String, String>> entries = mPostUidMap.entrySet();

        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        while (entries.size() >= 2) {

            Map.Entry<String, String> entry1 = iterator.next();
            Map.Entry<String, String> entry2 = iterator.next();

            count++;
            mTime = PRE_KEY + count;
            String channel = mBase64.encodeToString(mTime.getBytes());

            synchronized (LOCK_CHANNEL) {
                mChannelMap.put(entry1.getKey(), channel);
                mChannelMap.put(entry2.getKey(), channel);
            }
            entries.remove(entry1);
            entries.remove(entry2);
        }
    }

    public void remove(String uid) {
        synchronized (LOCK_UID) {
            MatchData data = new MatchData();
            data.uid = uid;
            mPostUidMap.remove(data);
        }
    }

    /**
     * 获取用户加入的频道
     *
     * @param uid 用户id
     * @return 频道号
     */
    public String getChannel(String uid) {
        synchronized (LOCK_CHANNEL) {
            return mChannelMap.remove(uid);
        }
    }

    private static class MatchData {
        private String uid;
        private String channel;

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof MatchData) {
                return this.uid.equals(((MatchData) obj).uid);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.uid);
        }
    }

    public interface MatchCallback {
        void onMatch(String uid, String channel);
    }
}
