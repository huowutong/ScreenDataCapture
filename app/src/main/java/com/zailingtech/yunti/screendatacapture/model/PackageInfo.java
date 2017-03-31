package com.zailingtech.yunti.screendatacapture.model;

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
    private long uploadDate;
    private boolean hasUpload;

    @Generated(hash = 694046598)
    public PackageInfo(Long id, String fileName, long uploadDate,
            boolean hasUpload) {
        this.id = id;
        this.fileName = fileName;
        this.uploadDate = uploadDate;
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

    public long getUploadDate() {
        return this.uploadDate;
    }

    public void setUploadDate(long uploadDate) {
        this.uploadDate = uploadDate;
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
                ", uploadDate=" + uploadDate +
                ", hasUpload=" + hasUpload +
                '}';
    }
}

