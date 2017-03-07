package com.zailingtech.yunti.screendatacapture;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.zailingtech.greendao.gen.DaoMaster;
import com.zailingtech.greendao.gen.DaoSession;

/**
 * @author LUTAO
 * @date 2017/03/07
 */

public class BaseApplication extends Application {

    private static DaoSession daoSession;
    private static String ScreenID;

    public static String getScreenID() {
        return ScreenID;
    }

    public static void setScreenID(String screenID) {
        ScreenID = screenID;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //配置数据库
        setupDatabase();
    }

    /**
     * 配置数据库
     */
    private void setupDatabase() {
        //创建数据库
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "packageinfo", null);
        //获取可写数据库
        SQLiteDatabase db = helper.getWritableDatabase();
        //获取数据库对象
        DaoMaster daoMaster = new DaoMaster(db);
        //获取Dao对象管理者
        daoSession = daoMaster.newSession();
    }

    public static DaoSession getDaoInstant() {
        return daoSession;
    }
}
