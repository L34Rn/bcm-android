package com.bcm.messenger.common.grouprepository.events;

import com.bcm.messenger.common.AccountContext;
import com.bcm.messenger.common.grouprepository.model.AmeGroupMessageDetail;

import java.util.List;

/**
 * 
 */
public class MessageEvent {
    public final AccountContext accountContext;
    public final long gid;
    public final long indexId;
    public final EventType type;
    public final List<Long> indexIdList;
    public final List<AmeGroupMessageDetail> targetList;

    public MessageEvent(AccountContext accountContext, long gid, long indexId, EventType type) {
        this.accountContext = accountContext;
        this.gid = gid;
        this.indexId = indexId;
        this.type = type;
        this.indexIdList = null;
        this.targetList = null;
    }

    public MessageEvent(AccountContext accountContext, long gid, EventType type, List<AmeGroupMessageDetail> list) {
        this.accountContext = accountContext;
        this.gid = gid;
        this.indexId = 0L;
        this.type = type;
        this.indexIdList = null;
        this.targetList = list;
    }

    public MessageEvent(AccountContext accountContext, long gid, List<Long> indexIdList) {
        this.accountContext = accountContext;
        this.gid = gid;
        this.indexId = 0L;
        this.indexIdList = indexIdList;
        this.type = EventType.DELETE_MESSAGES;
        this.targetList = null;
    }

    public MessageEvent(AccountContext accountContext, long gid, long indexId, EventType type, List<AmeGroupMessageDetail> list) {
        this.accountContext = accountContext;
        this.gid = gid;
        this.indexId = indexId;
        this.type = type;
        this.indexIdList = null;
        this.targetList = list;
    }

    public enum EventType {
        SEND_MESSAGE_INSERT,
        RECEIVE_MESSAGE_INSERT,
        SEND_MESSAGE_UPDATE,
        RECEIVE_MESSAGE_UPDATE,
        DELETE_ONE_MESSAGE,
        ATTACHMENT_DOWNLOAD_SUCCESS,
        FETCH_MESSAGE_SUCCESS,
        DELETE_MESSAGES,
        THUMBNAIL_DOWNLOAD_SUCCESS
    }

}
