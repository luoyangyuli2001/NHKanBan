package com.lyyl.nhkanban.common.network;

/**
 * 看板顶部 Tab。也用作任务列表查询的过滤维度。
 *
 * <p>
 * 通过 ordinal 序列化到网络包,新增项必须追加在末尾。中间插入会让旧客户端
 * 把所有后续 Tab 都解析错位。
 */
public enum ViewTab {

    /** 全服 GLOBAL 任务 */
    PUBLIC,
    /** 我发布的 + 我领取的 */
    MINE,
    /** DIRECT 给我的、@我的 */
    INBOX,
    /** 已完成历史 */
    ARCHIVE;

    private static final ViewTab[] VALUES = values();

    public static ViewTab fromOrdinal(int o) {
        if (o < 0 || o >= VALUES.length) {
            return PUBLIC;
        }
        return VALUES[o];
    }
}
