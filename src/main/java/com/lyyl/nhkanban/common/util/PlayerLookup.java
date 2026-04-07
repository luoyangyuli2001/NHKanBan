package com.lyyl.nhkanban.common.util;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.mojang.authlib.GameProfile;

/**
 * 把玩家用户名解析成 UUID。在线玩家优先,离线玩家走 server profile cache 兜底。
 *
 * <p>
 * cache 命中需要该玩家曾经登入过本服务器。从未登入过的离线玩家拿不到 UUID,
 * 直接返回 null;调用方应理解为"目标玩家不存在"并拒绝相关写操作。
 *
 * <p>
 * profile cache 的方法是 1.7.10 deobf 名(func_152655_a),GTNH 的 MCP 映射
 * 应当一致。如果将来 GTNH 切了映射版本编译失败,这里集中改一处即可。
 */
public final class PlayerLookup {

    private PlayerLookup() {}

    public static UUID resolveByName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return null;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return null;

        // 1. 在线玩家优先(大小写不敏感)
        for (Object obj : server.getConfigurationManager().playerEntityList) {
            if (obj instanceof EntityPlayerMP) {
                EntityPlayerMP p = (EntityPlayerMP) obj;
                if (trimmed.equalsIgnoreCase(p.getCommandSenderName())) {
                    return p.getUniqueID();
                }
            }
        }

        // 2. 离线玩家:检查 player data 文件是否真实存在,
        // 避免离线服务器把任意字符串"成功解析"成生成的 offline UUID
        if (!hasPlayerDataFile(server, trimmed)) {
            return null;
        }

        try {
            GameProfile profile = server.func_152358_ax()
                .func_152655_a(trimmed);
            if (profile != null && profile.getId() != null) {
                return profile.getId();
            }
        } catch (Throwable ignored) {}

        return null;
    }

    /**
     * 1.7.10 离线服务器把曾登入过的玩家存为 <world>/players/<name>.dat。
     * 在线服务器存在 <world>/playerdata/<uuid>.dat。两个目录都查一下,
     * 任一存在就说明这个名字真实对应过一个玩过的玩家。
     */
    private static boolean hasPlayerDataFile(MinecraftServer server, String name) {
        try {
            net.minecraft.world.WorldServer overworld = server.worldServerForDimension(0);
            if (overworld == null) return false;
            java.io.File worldDir = overworld.getSaveHandler()
                .getWorldDirectory();
            if (worldDir == null) return false;

            // 离线服:players/<name>.dat
            java.io.File offlineFile = new java.io.File(new java.io.File(worldDir, "players"), name + ".dat");
            if (offlineFile.isFile()) return true;

            // 在线服:playerdata/<uuid>.dat,需要先用 cache 查到 UUID 再确认
            java.io.File onlineDir = new java.io.File(worldDir, "playerdata");
            if (onlineDir.isDirectory()) {
                GameProfile profile = server.func_152358_ax()
                    .func_152655_a(name);
                if (profile != null && profile.getId() != null) {
                    java.io.File onlineFile = new java.io.File(onlineDir, profile.getId() + ".dat");
                    if (onlineFile.isFile()) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
