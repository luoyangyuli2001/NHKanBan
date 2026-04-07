# NHKanBan

[English](README_EN.md) | 中文

一个为 **GregTech: New Horizons**(Minecraft 1.7.10)开发的社区任务板 Mod。

NHKanBan 提供看板风格的任务管理界面,玩家可以在服务器内手动创建、领取、指派和追踪任务。

本 Mod 定位为纯协作沟通工具。

> **状态:早期开发中**。尚不可用,API 与功能仍可能变动。

## 计划功能

- 看板风格 GUI,四列布局:待领取 / 进行中 / 待审核 / 已完成
- 任务作用域:全服 / 队伍 / 私信
- 子任务、优先级、截止时间、评论
- 任务板方块 + 便携任务终端道具
- HUD 提示已领取任务与临近截止
- 基于 [ModularUI2](https://github.com/GTNewHorizons/ModularUI2) 构建

## 环境要求

- Minecraft 1.7.10
- Forge 10.13.4.1614
- ModularUI2
- 
## 中文输入说明

由于 Minecraft 1.7.10 与 GLFW 的 IME 兼容性限制,在 NHKanban 的输入框中
无法直接通过输入法输入中文。你可以选择：

1. **剪贴板粘贴**:在外部输入法中打好,Ctrl+C → Ctrl+V 粘贴到表单
2. **安装 InputFix mod**:GTNH 社区推荐的中文输入修复 mod
3. **使用指令**:`/nhkb create -t 中文标题` 经聊天输入。

## 协议

NHKanBan 采用 **GNU Lesser General Public License v3.0**(LGPL-3.0)协议开源。
详见 [LICENSE](LICENSE)。
