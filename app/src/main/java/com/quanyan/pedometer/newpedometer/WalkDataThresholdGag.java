package com.quanyan.pedometer.newpedometer;

import com.lidroid.xutils.db.annotation.Column;
import com.lidroid.xutils.db.annotation.Id;
import com.lidroid.xutils.db.annotation.NoAutoIncrement;
import com.lidroid.xutils.db.annotation.Table;

import java.io.Serializable;

/**
 * Created with Android Studio.
 * Title:WalkDataThresholdGag
 * Description:
 * Copyright:Copyright (c) 2016
 * Company:quanyan
 * Author:赵晓坡
 * Date:16/7/2
 * Time:12:15
 * Version 1.1.0
 */
@Table(name = "walkdata_threshold_gaps")
public class WalkDataThresholdGag implements Serializable {

    /** @Fields serialVersionUID: */
    private static final long serialVersionUID = 8818101589895754246L;

    @Column(column = "userId")
    public long userId;
    /**
     * 步数
     */
    @Column(column = "stepCount")
    public long stepCount;

    /**
     * 目标数
     */
    @Column(column = "targetStepCount")
    public long targetStepCount;

    /**
     * 里程数
     */
    @Column(column = "distance")
    public double distance;

    /**
     * 消耗的卡路里
     */
    @Column(column = "calories")
    public double calories;

    /**
     * 保存数据的时间
     */
    @Id
    @NoAutoIncrement
    @Column(column = "synTime")
    public long synTime;

    @Override
    public String toString() {
        return "WalkDataThresholdGag{" +
                "userId=" + userId +
                ", stepCount=" + stepCount +
                ", targetStepCount=" + targetStepCount +
                ", distance=" + distance +
                ", calories=" + calories +
                ", synTime=" + synTime +
                '}';
    }
}
