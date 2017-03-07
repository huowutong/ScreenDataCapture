package com.zailingtech.yunti.screendatacapture;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * @author LUTAO
 * @date 2017/03/06
 */

@Entity
public class PackageInfo {
    @Id(autoincrement = true)
    private Long id;
    private String fileName;
    private boolean hasUpload;
    @Generated(hash = 909655719)
    public PackageInfo(Long id, String fileName, boolean hasUpload) {
        this.id = id;
        this.fileName = fileName;
        this.hasUpload = hasUpload;
    }
    @Generated(hash = 1854842808)
    public PackageInfo() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getFileName() {
        return this.fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public boolean getHasUpload() {
        return this.hasUpload;
    }
    public void setHasUpload(boolean hasUpload) {
        this.hasUpload = hasUpload;
    }

    @Override
    public String toString() {
        return "PackageInfo{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", hasUpload=" + hasUpload +
                '}';
    }
}

