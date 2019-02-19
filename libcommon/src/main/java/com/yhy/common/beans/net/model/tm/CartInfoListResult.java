package com.yhy.common.beans.net.model.tm;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with Android Studio.
 * Title:CartInfoListResult
 * Description:
 * Copyright:Copyright (c) 2016
 * Company:quanyan
 * Author:鲍杰
 * Date:2016-10-25
 * Time:9:45
 * Version 1.1.0
 */


public class CartInfoListResult implements Serializable {
    private static final long serialVersionUID = -5777935767347326600L;
    /**
     * 商家信息列表
     */
    public List<ShopInfoResult> shopInfoResultList;
    /**
     * 购物车勾选状态
     */
    public boolean cartCheck;

    /**
     * 总金额
     */
    public long actualTotalFee;

    /**
     * 积分可抵金额
     */
    public long pointTotalFee;

    /**
     * 勾选商品总数量
     */
    public int count;

    /**
     * 反序列化函数，用于从json字符串反序列化本类型实例
     */
    public static CartInfoListResult deserialize(String json) throws JSONException {
        if (json != null && !json.isEmpty()) {
            return deserialize(new JSONObject(json));
        }
        return null;
    }

    /**
     * 反序列化函数，用于从json节点对象反序列化本类型实例
     */
    public static CartInfoListResult deserialize(JSONObject json) throws JSONException {
        if (json != null && json != JSONObject.NULL && json.length() > 0) {
            CartInfoListResult result = new CartInfoListResult();

            // 商家信息列表
            JSONArray shopInfoResultListArray = json.optJSONArray("shopInfoResultList");
            if (shopInfoResultListArray != null) {
                int len = shopInfoResultListArray.length();
                result.shopInfoResultList = new ArrayList<ShopInfoResult>(len);
                for (int i = 0; i < len; i++) {
                    JSONObject jo = shopInfoResultListArray.optJSONObject(i);
                    if (jo != null && jo != JSONObject.NULL && jo.length() > 0) {
                        result.shopInfoResultList.add(ShopInfoResult.deserialize(jo));
                    }
                }
            }

            // 购物车勾选状态
            result.cartCheck = json.optBoolean("cartCheck");

            // 总金额
            result.actualTotalFee = json.optLong("actualTotalFee");
            // 积分可抵金额
            result.pointTotalFee = json.optLong("pointTotalFee");
            // 勾选商品总数量
            result.count = json.optInt("count");

            return result;
        }
        return null;
    }

    /*
     * 序列化函数，用于从对象生成数据字典
     */
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();

        // 商家信息列表
        if (this.shopInfoResultList != null) {
            JSONArray shopInfoResultListArray = new JSONArray();
            for (ShopInfoResult value : this.shopInfoResultList)
            {
                if (value != null) {
                    shopInfoResultListArray.put(value.serialize());
                }
            }
            json.put("shopInfoResultList", shopInfoResultListArray);
        }

        // 购物车勾选状态
        json.put("cartCheck", this.cartCheck);

        // 总金额
        json.put("actualTotalFee", this.actualTotalFee);

        // 积分可抵金额
        json.put("pointTotalFee", this.pointTotalFee);

        // 勾选商品总数量
        json.put("count", this.count);


        return json;
    }
}
