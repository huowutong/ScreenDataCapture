package com.zailingtech.yunti.screendatacapture;

import com.zailingtech.greendao.gen.PackageInfoDao;

import java.util.List;

/**
 * @author LUTAO
 * @date 2017/03/07
 */

public class PackageDao {

    /**
     * 添加数据，如果有重复则覆盖
     *
     * @param packageInfo
     */
    public static void insert(PackageInfo packageInfo) {
        BaseApplication.getInstance().getDaoInstance().getPackageInfoDao().insertOrReplace(packageInfo);
    }

    /**
     * 删除数据
     *
     * @param id
     */
    public static void delete(long id) {
        BaseApplication.getInstance().getDaoInstance().getPackageInfoDao().deleteByKey(id);
    }

    /**
     * 更新数据
     *
     * @param packageInfo
     */
    public static void update(PackageInfo packageInfo) {
        BaseApplication.getInstance().getDaoInstance().getPackageInfoDao().update(packageInfo);
    }

    /**
     * 查询条件为hasUpload的数据
     * @param hasUpload
     * @return
     */
    public static List<PackageInfo> queryUploadFlag(boolean hasUpload) {
        return BaseApplication.getInstance().getDaoInstance().getPackageInfoDao().queryBuilder()
                .where(PackageInfoDao.Properties.HasUpload.eq(hasUpload))
                .orderDesc(PackageInfoDao.Properties.FileName).list();
    }

    /**
     * 查询条件为hasUpload的数据,并限制查询的数据个数
     * @param hasUpload
     * @param num 限制查询的数据个数
     * @return
     */
    public static List<PackageInfo> queryUploadFlag(boolean hasUpload, int num) {
        if (num < 0) {
            return null;
        } else if (num == 0) {
            return queryUploadFlag(hasUpload);
        } else {
            return BaseApplication.getInstance().getDaoInstance().getPackageInfoDao().queryBuilder()
                    .where(PackageInfoDao.Properties.HasUpload.eq(hasUpload))
                    .orderDesc(PackageInfoDao.Properties.FileName)
                    .limit(num).list();
        }
    }

    /**
     * 查询条件为fileName的数据
     * @param fileName
     * @return
     */
    public static List<PackageInfo> queryFileName(String fileName) {
        return BaseApplication.getInstance().getDaoInstance().getPackageInfoDao().queryBuilder()
                .where(PackageInfoDao.Properties.FileName.eq(fileName))
                .orderDesc(PackageInfoDao.Properties.FileName).list();
    }

    /**
     * 查询全部数据
     */
    public static List<PackageInfo> queryAll() {
        return BaseApplication.getInstance().getDaoInstance().getPackageInfoDao().loadAll();
    }

    public static void clearAll() {
        BaseApplication.getInstance().getDaoInstance().getPackageInfoDao().deleteAll();
    }
}
