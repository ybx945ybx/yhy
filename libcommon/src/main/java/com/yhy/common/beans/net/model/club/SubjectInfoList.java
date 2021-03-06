// Auto Generated.  DO NOT EDIT!
package com.yhy.common.beans.net.model.club;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SubjectInfoList implements Serializable{

    private static final long serialVersionUID = 5350528336449901489L;
    /**
     * 当前页码
     */
    public int pageNo;
      
    /**
     * 是否有下一页
     */
    public boolean hasNext;
      
    /**
     * 话题列表
     */
    public List<SubjectInfo> subjectInfoList;
    /**
     * 用户信息
     */
    public SnsUserInfo userInfo;
      
    /**
     * 反序列化函数，用于从json字符串反序列化本类型实例
     */
    public static SubjectInfoList deserialize(String json) throws JSONException {
        if (json != null && !json.isEmpty()) {
            return deserialize(new JSONObject(json));
        }
        return null;
    }
    
    /**
     * 反序列化函数，用于从json节点对象反序列化本类型实例
     */
    public static SubjectInfoList deserialize(JSONObject json) throws JSONException {
        if (json != null && json != JSONObject.NULL && json.length() > 0) {
            SubjectInfoList result = new SubjectInfoList();
            
            // 当前页码
            result.pageNo = json.optInt("pageNo");
            // 是否有下一页
            result.hasNext = json.optBoolean("hasNext");
            // 话题列表
            JSONArray subjectInfoListArray = json.optJSONArray("subjectInfoList");
            if (subjectInfoListArray != null) {
                int len = subjectInfoListArray.length();
                result.subjectInfoList = new ArrayList<SubjectInfo>(len);
                for (int i = 0; i < len; i++) {
                        JSONObject jo = subjectInfoListArray.optJSONObject(i);
                    if (jo != null && jo != JSONObject.NULL && jo.length() > 0) {
                        result.subjectInfoList.add(SubjectInfo.deserialize(jo));
                    }
                }
            }
      
            // 用户信息
            result.userInfo = SnsUserInfo.deserialize(json.optJSONObject("userInfo"));
            return result;
        }
        return null;
    }
    
    /*
     * 序列化函数，用于从对象生成数据字典
     */
    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        
        // 当前页码
        json.put("pageNo", this.pageNo);
          
        // 是否有下一页
        json.put("hasNext", this.hasNext);
          
        // 话题列表 
        if (this.subjectInfoList != null) {
            JSONArray subjectInfoListArray = new JSONArray();
            for (SubjectInfo value : this.subjectInfoList)
            {
                if (value != null) {
                    subjectInfoListArray.put(value.serialize());
                }
            }
            json.put("subjectInfoList", subjectInfoListArray);
        }
      
        // 用户信息
        if (this.userInfo != null) { json.put("userInfo", this.userInfo.serialize()); }
          
        return json;
    }
}
  