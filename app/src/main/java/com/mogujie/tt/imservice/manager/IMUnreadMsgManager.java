package com.mogujie.tt.imservice.manager;


import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;

import com.mogujie.tt.imservice.event.UnreadEvent;
import com.mogujie.tt.protobuf.IMBaseDefine;
import com.mogujie.tt.protobuf.IMMessage;
import com.mogujie.tt.protobuf.helper.EntityChangeEngine;
import com.mogujie.tt.protobuf.helper.Java2ProtoBuf;
import com.mogujie.tt.protobuf.helper.ProtoBuf2JavaBean;
import com.mogujie.tt.utils.Logger;
import com.quanyan.yhy.database.DBManager;
import com.yhy.common.beans.im.NotificationMessageEntity;
import com.yhy.common.beans.im.entity.MessageEntity;
import com.yhy.common.beans.im.entity.UnreadEntity;
import com.yhy.common.constants.DBConstant;
import com.yhy.common.constants.NotificationConstants;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * 未读消息相关的处理，归属于messageEvent中
 * 可以理解为MessageManager的又一次拆分
 * 为session提供未读支持。
 * DB 中不保存
 */
public class IMUnreadMsgManager extends IMManager {
    private Logger logger = Logger.getLogger(IMUnreadMsgManager.class);
    private static IMUnreadMsgManager inst = new IMUnreadMsgManager();

    public static IMUnreadMsgManager instance() {
        return inst;
    }

    private IMSocketManager imSocketManager = IMSocketManager.instance();
    private IMLoginManager loginManager = IMLoginManager.instance();

    /**
     * key=> sessionKey
     */
    private ConcurrentHashMap<String, UnreadEntity> unreadMsgMap = new ConcurrentHashMap<>();
    private int totalUnreadCount = 0;

    private boolean unreadListReady = false;

    @Override
    public void doOnStart() {
    }


    // 未读消息控制器，本地是不存状态的
    public void onNormalLoginOk() {
        unreadMsgMap.clear();
        reqUnreadMsgContactList();
    }

    public void onLocalNetOk() {
        unreadMsgMap.clear();
        reqUnreadMsgContactList();
    }

    @Override
    public void reset() {
        unreadListReady = false;
        unreadMsgMap.clear();
        EventBus.getDefault().post(new UnreadEvent(UnreadEvent.Event.SESSION_READED_UNREAD_MSG));
    }

    /**
     * 继承该方法实现自身的事件驱动
     *
     * @param event
     */
    public synchronized void triggerEvent(UnreadEvent event) {
        switch (event.event) {
            case UNREAD_MSG_LIST_OK:
                unreadListReady = true;
                break;
        }

        EventBus.getDefault().post(event);
    }

    /**-------------------------------分割线----------------------------------*/
    /**
     * 请求未读消息列表
     */
    private void reqUnreadMsgContactList() {
        logger.i("unread#1reqUnreadMsgContactList");
        long loginId = IMLoginManager.instance().getLoginId();
        IMMessage.IMUnreadMsgCntReq unreadMsgCntReq = IMMessage.IMUnreadMsgCntReq
                .newBuilder()
                .setUserId(loginId)
                .build();
        int sid = IMBaseDefine.ServiceID.SID_MSG_VALUE;
        int cid = IMBaseDefine.MessageCmdID.CID_MSG_UNREAD_CNT_REQUEST_VALUE;
        imSocketManager.sendRequest(unreadMsgCntReq, sid, cid);
    }

    public void onRepUnreadMsgContactList(IMMessage.IMUnreadMsgCntRsp unreadMsgCntRsp) {
        logger.i("unread#2onRepUnreadMsgContactList");
        totalUnreadCount = unreadMsgCntRsp.getTotalCnt();
        List<IMBaseDefine.UnreadInfo> unreadInfoList = unreadMsgCntRsp.getUnreadinfoListList();
        logger.i("unread#unreadMsgCnt:%d, unreadMsgInfoCnt:%d", unreadInfoList.size(), totalUnreadCount);

        for (IMBaseDefine.UnreadInfo unreadInfo : unreadInfoList) {
            UnreadEntity unreadEntity = ProtoBuf2JavaBean.getUnreadEntity(unreadInfo);
            //屏蔽的设定
//            addIsForbidden(unreadEntity);
            if (unreadEntity == null) continue;
            unreadMsgMap.put(unreadEntity.getSessionKey(), unreadEntity);
        }
        setNotificationUnreadMsg();
        triggerEvent(new UnreadEvent(UnreadEvent.Event.UNREAD_MSG_LIST_OK));
    }

    private void setNotificationUnreadMsg() {
        addLocalNotificationUnread(NotificationConstants.BIZ_TYPE_TRANSACTION);
        addLocalNotificationUnread(NotificationConstants.BIZ_TYPE_INTERACTION);
    }

    private void addLocalNotificationUnread(int bizType) {
        long unReadCount = (int) DBManager.getInstance(ctx).getNotificationUnReadCount(bizType);
        if (unReadCount == 0) {
            return;
        }
        UnreadEntity unreadEntity = new UnreadEntity();
        unreadEntity.setUnReadCnt((int) unReadCount);
        unreadEntity.setPeerId(bizType);
        unreadEntity.setSessionType(DBConstant.SESSION_TYPE_NOTIFICATION);
        unreadEntity.buildSessionKey();

        unreadEntity.setLatestMsgData(null);
        unreadEntity.setLaststMsgId(0);

        /**放入manager 状态中*/
        unreadMsgMap.put(unreadEntity.getSessionKey(), unreadEntity);

    }

    /**
     * 回话是否已经被设定为屏蔽
     * @param unreadEntity
     */
//    private void addIsForbidden(UnreadEntity unreadEntity){
//        if(unreadEntity.getSessionType() == DBConstant.SESSION_TYPE_GROUP){
//            GroupEntity groupEntity= IMGroupManager.instance().findGroup(unreadEntity.getPeerId());
//            if(groupEntity !=null && groupEntity.getStatus() == DBConstant.GROUP_STATUS_SHIELD){
//                unreadEntity.setForbidden(true);
//            }
//        }
//    }

    /**
     * 设定未读回话为屏蔽回话 仅限于群组 todo
     */
    public void setForbidden(String sessionKey, boolean isFor) {
        UnreadEntity unreadEntity = unreadMsgMap.get(sessionKey);
        if (unreadEntity != null) {
            unreadEntity.setForbidden(isFor);
        }
    }

    public void add(MessageEntity msg) {
        //更新session list中的msg信息
        //更新未读消息计数
        if (msg == null) {
            logger.d("unread#unreadMgr#add msg is null!");
            return;
        }
        // isFirst场景:出现一条未读消息，出现小红点，需要触发 [免打扰的情况下]
        boolean isFirst = false;
        logger.d("unread#unreadMgr#add unread msg:%s", msg);
        UnreadEntity unreadEntity;
        long loginId = IMLoginManager.instance().getLoginId();
        String sessionKey = msg.getSessionKey();
        boolean isSend = msg.isSend(loginId);
        if (isSend || msg.getMsgType() == DBConstant.MSG_TYPE_CONSULT_NOTIFY) {
//            IMNotificationManager.instance().cancelSessionNotifications(String.valueOf(msg.getSessionType()));
            return;
        }

        if (unreadMsgMap.containsKey(sessionKey)) {
            unreadEntity = unreadMsgMap.get(sessionKey);
            // 判断最后一条msgId是否相同
            if (unreadEntity.getLaststMsgId() == msg.getMsgId()) {
                return;
            }
            unreadEntity.setUnReadCnt(unreadEntity.getUnReadCnt() + 1);
        } else {
            isFirst = true;
            unreadEntity = new UnreadEntity();
            unreadEntity.setUnReadCnt(1);
            unreadEntity.setPeerId(msg.getPeerId(isSend));
            unreadEntity.setSessionType(msg.getSessionType());
            unreadEntity.setServiceId(msg.getServiceId());
            unreadEntity.buildSessionKey();
        }

        unreadEntity.setLatestMsgData(msg.getMessageDisplay());
        unreadEntity.setLaststMsgId(msg.getMsgId());
//        addIsForbidden(unreadEntity);

        /**放入manager 状态中*/
        unreadMsgMap.put(unreadEntity.getSessionKey(), unreadEntity);

        /**没有被屏蔽才会发送广播*/
        if (!unreadEntity.isForbidden() || isFirst) {
            UnreadEvent unreadEvent = new UnreadEvent();
            unreadEvent.event = UnreadEvent.Event.UNREAD_MSG_RECEIVED;
            unreadEvent.entity = unreadEntity;
            triggerEvent(unreadEvent);
        }
    }

    public void add(NotificationMessageEntity msg) {
        //更新session list中的msg信息
        //更新未读消息计数
        if (msg == null) {
            logger.d("unread#unreadMgr#add msg is null!");
            return;
        }
        // isFirst场景:出现一条未读消息，出现小红点，需要触发 [免打扰的情况下]
        boolean isFirst = false;
        logger.d("unread#unreadMgr#add unread msg:%s", msg);
        UnreadEntity unreadEntity;
        String sessionKey = msg.getSessionKey();

        if (unreadMsgMap.containsKey(sessionKey)) {
            unreadEntity = unreadMsgMap.get(sessionKey);
            // 判断最后一条msgId是否相同
            if (unreadEntity.getLaststMsgId() == msg.getMessageId()) {
                return;
            }
            unreadEntity.setUnReadCnt(unreadEntity.getUnReadCnt() + 1);
        } else {
            isFirst = true;
            unreadEntity = new UnreadEntity();
            unreadEntity.setUnReadCnt(1);
            unreadEntity.setPeerId(msg.getPeerId());
            unreadEntity.setSessionType(msg.getSessionType());
            unreadEntity.buildSessionKey();
        }
        unreadEntity.setNeedNotify(msg.needNotification);
        unreadEntity.setLatestMsgData(msg.getMessage());
        unreadEntity.setLaststMsgId((int) msg.getMessageId());
        unreadEntity.setTitle(msg.getNotiTitle());
        unreadEntity.setNtfCode(msg.getNtfOperationCode());
        unreadEntity.setNtfVaule(msg.getNtfOperationVaule());

        /**放入manager 状态中*/
        unreadMsgMap.put(unreadEntity.getSessionKey(), unreadEntity);

        /**没有被屏蔽才会发送广播*/
        if (!unreadEntity.isForbidden() || isFirst) {
            UnreadEvent unreadEvent = new UnreadEvent();

            unreadEvent.event = UnreadEvent.Event.UNREAD_MSG_RECEIVED;
            unreadEvent.entity = unreadEntity;
            triggerEvent(unreadEvent);
        }
    }

    public void ackReadMsg(MessageEntity entity) {
        logger.d("chat#ackReadMsg -> msg:%s", entity);
        long loginId = loginManager.getLoginId();
        if (entity.isSend(loginId)) return;
        IMBaseDefine.SessionType sessionType;
        IMBaseDefine.ExtInfo extInfo = null;
        if (Java2ProtoBuf.isNewSessionType(entity.getSessionType())) {
            sessionType = IMBaseDefine.SessionType.SESSION_TYPE_SINGLE;
            extInfo = IMBaseDefine.ExtInfo.newBuilder().setSessionType(Java2ProtoBuf.getProtoSessionTypeNew(entity.getSessionType())).build();
        } else {
            sessionType = Java2ProtoBuf.getProtoSessionType(entity.getSessionType());
        }
        IMMessage.IMMsgDataReadAck readAck;
        IMMessage.IMMsgDataReadAck.Builder builder = IMMessage.IMMsgDataReadAck.newBuilder()
                .setMsgId(entity.getMsgId())
                .setSessionId(entity.getPeerId(false))
                .setSessionType(sessionType)
                .setUserId(loginId).setMsgItem(entity.getServiceId());
        if (extInfo == null) {
            readAck = builder.build();
        } else {
            readAck = builder.setExtInfo(extInfo).build();
        }
        int sid = IMBaseDefine.ServiceID.SID_MSG_VALUE;
        int cid = IMBaseDefine.MessageCmdID.CID_MSG_READ_ACK_VALUE;
        imSocketManager.sendRequest(readAck, sid, cid);
    }

    public void ackReadMsg(UnreadEntity unreadEntity) {
        logger.d("chat#ackReadMsg -> msg:%s", unreadEntity);
        long loginId = loginManager.getLoginId();
        IMBaseDefine.SessionType sessionType;
        IMBaseDefine.ExtInfo extInfo = null;
        if (Java2ProtoBuf.isNewSessionType(unreadEntity.getSessionType())) {
            sessionType = IMBaseDefine.SessionType.SESSION_TYPE_SINGLE;
            extInfo = IMBaseDefine.ExtInfo.newBuilder().setSessionType(Java2ProtoBuf.getProtoSessionTypeNew(unreadEntity.getSessionType())).build();
        } else {
            sessionType = Java2ProtoBuf.getProtoSessionType(unreadEntity.getSessionType());
        }
        IMMessage.IMMsgDataReadAck readAck;
        IMMessage.IMMsgDataReadAck.Builder builder = IMMessage.IMMsgDataReadAck.newBuilder()
                .setMsgId(unreadEntity.getLaststMsgId())
                .setSessionId(unreadEntity.getPeerId())
                .setSessionType(sessionType)
                .setUserId(loginId).setMsgItem(unreadEntity.getServiceId());
        if (extInfo == null) {
            readAck = builder.build();
        } else {
            readAck = builder.setExtInfo(extInfo).build();
        }
        int sid = IMBaseDefine.ServiceID.SID_MSG_VALUE;
        int cid = IMBaseDefine.MessageCmdID.CID_MSG_READ_ACK_VALUE;
        imSocketManager.sendRequest(readAck, sid, cid);
    }


    /**
     * 服务端主动发送已读通知
     *
     * @param readNotify
     */
    public void onNotifyRead(IMMessage.IMMsgDataReadNotify readNotify) {
        logger.d("chat#onNotifyRead");
        //发送此信令的用户id
        long trigerId = readNotify.getUserId();
        long loginId = IMLoginManager.instance().getLoginId();
        if (trigerId != loginId) {
            logger.i("onNotifyRead# trigerId:%s,loginId:%s not Equal", trigerId, loginId);
            return;
        }
        //现在的逻辑是msgId之后的 全部都是已读的
        // 不做复杂判断了，简单处理
        int msgId = readNotify.getMsgId();
        long peerId = readNotify.getSessionId();
        long serviceId = readNotify.getMsgItem();
        int sessionType = -1;
        if (readNotify.getExtInfo() == null) {
            sessionType = ProtoBuf2JavaBean.getJavaSessionType(readNotify.getSessionType());
        } else {
            sessionType = ProtoBuf2JavaBean.getJavaSessionTypeNew(readNotify.getExtInfo().getSessionType());
        }
        if (sessionType == -1) {
            return;
        }
        String sessionKey = EntityChangeEngine.getSessionKey(peerId, sessionType, serviceId);

        // 通知栏也要去除掉
        NotificationManager notifyMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notifyMgr == null) {
            return;
        }
        int notificationId = IMNotificationManager.instance().getSessionNotificationId(sessionKey);
        notifyMgr.cancel(notificationId);

        UnreadEntity unreadSession = findUnread(sessionKey);
        if (unreadSession != null && unreadSession.getLaststMsgId() <= msgId) {
            // 清空会话session
            logger.d("chat#onNotifyRead# unreadSession onLoginOut");
            readUnreadSession(sessionKey);
        }
    }

    /**
     * 备注: 先获取最后一条消息
     * 1. 清除回话内的未读计数
     * 2. 发送最后一条msgId的已读确认
     *
     * @param sessionKey
     */
    public void readUnreadSession(String sessionKey, boolean needAck) {
        logger.d("unread#readUnreadSession# sessionKey:%s", sessionKey);
        if (unreadMsgMap.containsKey(sessionKey)) {
            UnreadEntity entity = unreadMsgMap.remove(sessionKey);
            if (needAck) ackReadMsg(entity);
            triggerEvent(new UnreadEvent(UnreadEvent.Event.SESSION_READED_UNREAD_MSG));
        }
    }

    public void readUnreadSession(String sessionKey) {
        readUnreadSession(sessionKey, true);
    }


    public UnreadEntity findUnread(String sessionKey) {
        logger.d("unread#findUnread# buddyId:%s", sessionKey);
        if (TextUtils.isEmpty(sessionKey) || unreadMsgMap.size() <= 0) {
            logger.i("unread#findUnread# no unread info");
            return null;
        }
        if (unreadMsgMap.containsKey(sessionKey)) {
            return unreadMsgMap.get(sessionKey);
        }
        return null;
    }

    /**
     * ----------------实体set/get-------------------------------
     */
    public ConcurrentHashMap<String, UnreadEntity> getUnreadMsgMap() {
        return unreadMsgMap;
    }

    public int getTotalUnreadCount() {
        int count = 0;
        for (UnreadEntity entity : unreadMsgMap.values()) {
            if (!entity.isForbidden()) {
                count = count + entity.getUnReadCnt();
            }
        }
        return count;
    }

    public boolean isUnreadListReady() {
        return unreadListReady;
    }
}