package com.alibaba.ihome.rollback.aop;

import com.alibaba.ihome.rollback.DbContainer;

import java.util.ArrayList;
import java.util.List;

public class TraceSign {
    private final static ThreadLocal<Boolean> INTERCEPT = new ThreadLocal<>();

    private final static ThreadLocal<DbContainer> DB_CONTAINER = new ThreadLocal<>();

    private final static ThreadLocal<String> TAG = new ThreadLocal<>();

    private final static ThreadLocal<String> EntryKey = new ThreadLocal<>();

    private final static ThreadLocal<Boolean> FROM_ROLLBACK = new ThreadLocal<>();

    private final static ThreadLocal<List<String>> REPO_KEY = new ThreadLocal<>();

    public static Boolean intercepted() {
        Boolean intercepted = INTERCEPT.get();
        if (intercepted == null) {
            return false;
        }
        return true;
    }

    public static void setIntercept() {
        INTERCEPT.set(true);
    }

    public static DbContainer getDbContainer() {
        DbContainer dbContainer = DB_CONTAINER.get();
        if (dbContainer == null) {
            return null;
        }
        return dbContainer;
    }

    public static void setDbContainer(DbContainer dbContainer) {
        DB_CONTAINER.set(dbContainer);
    }

    public static String getTag() {
        String tag = TAG.get();
        if (tag == null) {
            return null;
        }
        return tag;
    }

    public static void setTag(String tag) {
        TAG.set(tag);
    }

    public static String getEntryKey() {
        String key = EntryKey.get();
        if (key == null) {
            return null;
        }
        return key;
    }

    public static boolean getIsFromRollBack() {
        Boolean rollback = FROM_ROLLBACK.get();
        if (rollback == null) {
            return false;
        } else {
            return rollback;
        }
    }

    public static void setIsFromRollBack() {
        FROM_ROLLBACK.set(true);
    }

    public static void removeIsFromRollBack() {
        FROM_ROLLBACK.remove();
    }

    public static void setEntryKey(String key) {
        EntryKey.set(key);
    }

    public static void addRepoKey(String key) {
        if (REPO_KEY.get() == null) {
            REPO_KEY.set(new ArrayList<>());
        }

        REPO_KEY.get().add(key);
    }

    public static boolean containsRepoKey(String key) {
        if (REPO_KEY.get() == null) {
            return false;
        }

        return REPO_KEY.get().contains(key);
    }

    public static void clear() {
        INTERCEPT.remove();
        EntryKey.remove();
        TAG.remove();
        DB_CONTAINER.remove();
        REPO_KEY.remove();
    }

}
