package com.lyyl.nhkanban.common.command;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.lyyl.nhkanban.common.storage.TaskRepository;
import com.lyyl.nhkanban.common.task.Task;
import com.lyyl.nhkanban.common.task.TaskPriority;
import com.lyyl.nhkanban.common.task.TaskScope;
import com.lyyl.nhkanban.common.task.TaskStatus;
import com.lyyl.nhkanban.common.task.TaskType;

public class KanbanCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "nhkb";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/nhkb <create|list|info|claim|submit|review|cancel> ...";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                "create",
                "list",
                "info",
                "claim",
                "submit",
                "review",
                "cancel");
        }
        if (args.length >= 2 && "create".equalsIgnoreCase(args[0])) {
            String last = args[args.length - 1];
            if (last.startsWith("-")) {
                return getListOfStringsMatchingLastWord(args, "-t", "-d", "-p", "-s", "-c", "-e");
            }
            if (args.length >= 3) {
                String prev = args[args.length - 2];
                if ("-p".equalsIgnoreCase(prev)) {
                    return getListOfStringsMatchingLastWord(args, "LOW", "NORMAL", "HIGH", "URGENT");
                }
                if ("-s".equalsIgnoreCase(prev)) {
                    return getListOfStringsMatchingLastWord(args, "GLOBAL", "TEAM", "DIRECT");
                }
            }
        }
        if (args.length == 3 && "review".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "approve", "reject");
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }
        String sub = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        if ("create".equals(sub)) {
            handleCreate(sender, rest);
        } else if ("list".equals(sub)) {
            handleList(sender, rest);
        } else if ("info".equals(sub)) {
            handleInfo(sender, rest);
        } else if ("claim".equals(sub)) {
            handleClaim(sender, rest);
        } else if ("submit".equals(sub)) {
            handleSubmit(sender, rest);
        } else if ("review".equals(sub)) {
            handleReview(sender, rest);
        } else if ("cancel".equals(sub)) {
            handleCancel(sender, rest);
        } else {
            sendUsage(sender);
        }
    }

    // --- 子命令实现 ---

    private void handleCreate(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            reply(sender, EnumChatFormatting.RED + "Only players can create tasks.");
            return;
        }
        if (args.length == 0) {
            sendCreateUsage(sender);
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;

        String title = null;
        String description = "";
        TaskPriority priority = TaskPriority.NORMAL;
        TaskScope scope = TaskScope.GLOBAL;
        int maxClaimers = 1;
        Long deadline = null;

        int i = 0;
        while (i < args.length) {
            String token = args[i];
            if ("-t".equalsIgnoreCase(token) || "--title".equalsIgnoreCase(token)) {
                Greedy g = readGreedy(args, i + 1);
                if (g.value.isEmpty()) {
                    reply(sender, EnumChatFormatting.RED + "Missing value for " + token);
                    return;
                }
                title = g.value;
                i = g.nextIndex;
            } else if ("-d".equalsIgnoreCase(token) || "--desc".equalsIgnoreCase(token)) {
                Greedy g = readGreedy(args, i + 1);
                if (g.value.isEmpty()) {
                    reply(sender, EnumChatFormatting.RED + "Missing value for " + token);
                    return;
                }
                description = g.value;
                i = g.nextIndex;
            } else if ("-p".equalsIgnoreCase(token) || "--priority".equalsIgnoreCase(token)) {
                if (i + 1 >= args.length) {
                    reply(sender, EnumChatFormatting.RED + "Missing value for " + token);
                    return;
                }
                try {
                    priority = TaskPriority.valueOf(args[i + 1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    reply(
                        sender,
                        EnumChatFormatting.RED + "Invalid priority: " + args[i + 1] + " (LOW/NORMAL/HIGH/URGENT)");
                    return;
                }
                i += 2;
            } else if ("-s".equalsIgnoreCase(token) || "--scope".equalsIgnoreCase(token)) {
                if (i + 1 >= args.length) {
                    reply(sender, EnumChatFormatting.RED + "Missing value for " + token);
                    return;
                }
                try {
                    scope = TaskScope.valueOf(args[i + 1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    reply(sender, EnumChatFormatting.RED + "Invalid scope: " + args[i + 1] + " (GLOBAL/TEAM/DIRECT)");
                    return;
                }
                i += 2;
            } else if ("-c".equalsIgnoreCase(token) || "--claimers".equalsIgnoreCase(token)) {
                if (i + 1 >= args.length) {
                    reply(sender, EnumChatFormatting.RED + "Missing value for " + token);
                    return;
                }
                try {
                    maxClaimers = Integer.parseInt(args[i + 1]);
                    if (maxClaimers < 1) {
                        reply(sender, EnumChatFormatting.RED + "Max claimers must be >= 1");
                        return;
                    }
                } catch (NumberFormatException e) {
                    reply(sender, EnumChatFormatting.RED + "Invalid number: " + args[i + 1]);
                    return;
                }
                i += 2;
            } else if ("-e".equalsIgnoreCase(token) || "--deadline".equalsIgnoreCase(token)) {
                if (i + 1 >= args.length) {
                    reply(sender, EnumChatFormatting.RED + "Missing value for " + token);
                    return;
                }
                try {
                    long hours = Long.parseLong(args[i + 1]);
                    if (hours <= 0) {
                        reply(sender, EnumChatFormatting.RED + "Deadline hours must be > 0");
                        return;
                    }
                    deadline = System.currentTimeMillis() + hours * 3600_000L;
                } catch (NumberFormatException e) {
                    reply(sender, EnumChatFormatting.RED + "Invalid hours: " + args[i + 1]);
                    return;
                }
                i += 2;
            } else {
                reply(sender, EnumChatFormatting.RED + "Unknown argument: " + token);
                sendCreateUsage(sender);
                return;
            }
        }

        if (title == null || title.isEmpty()) {
            reply(sender, EnumChatFormatting.RED + "Missing title. Use -t <title>");
            sendCreateUsage(sender);
            return;
        }
        if (title.length() > Task.MAX_TITLE_LENGTH) {
            reply(sender, EnumChatFormatting.RED + "Title too long (max " + Task.MAX_TITLE_LENGTH + ")");
            return;
        }
        if (description.length() > Task.MAX_DESCRIPTION_LENGTH) {
            reply(sender, EnumChatFormatting.RED + "Description too long (max " + Task.MAX_DESCRIPTION_LENGTH + ")");
            return;
        }

        Task task = Task.create(player.getUniqueID(), title, description, scope, TaskType.OPEN, priority, maxClaimers);
        task.setDeadline(deadline);
        TaskRepository.get()
            .put(task);

        reply(sender, EnumChatFormatting.GREEN + "Task created: " + EnumChatFormatting.WHITE + shortId(task.getUuid()));
        reply(sender, EnumChatFormatting.GRAY + "  title:    " + EnumChatFormatting.WHITE + title);
        if (!description.isEmpty()) {
            reply(sender, EnumChatFormatting.GRAY + "  desc:     " + EnumChatFormatting.WHITE + description);
        }
        reply(sender, EnumChatFormatting.GRAY + "  priority: " + EnumChatFormatting.WHITE + priority);
        reply(sender, EnumChatFormatting.GRAY + "  scope:    " + EnumChatFormatting.WHITE + scope);
        reply(sender, EnumChatFormatting.GRAY + "  claimers: " + EnumChatFormatting.WHITE + "0/" + maxClaimers);
        if (deadline != null) {
            long h = (deadline - System.currentTimeMillis()) / 3600_000L;
            reply(sender, EnumChatFormatting.GRAY + "  deadline: " + EnumChatFormatting.WHITE + "in " + h + "h");
        }
    }

    private void handleList(ICommandSender sender, String[] args) {
        List<Task> all = TaskRepository.get()
            .all();
        if (all.isEmpty()) {
            reply(sender, EnumChatFormatting.GRAY + "No tasks.");
            return;
        }
        reply(sender, EnumChatFormatting.YELLOW + "=== Tasks (" + all.size() + ") ===");
        for (Task t : all) {
            EnumChatFormatting statusColor = colorForStatus(t.getStatus());
            reply(
                sender,
                EnumChatFormatting.WHITE + shortId(t.getUuid())
                    + " "
                    + statusColor
                    + "["
                    + t.getStatus()
                    + "]"
                    + EnumChatFormatting.RESET
                    + " "
                    + t.getTitle());
        }
    }

    private void handleInfo(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            reply(sender, EnumChatFormatting.RED + "Missing task id.");
            reply(sender, EnumChatFormatting.GRAY + "Usage: /nhkb info <id>");
            return;
        }
        Task t = findByShortId(args[0]);
        if (t == null) {
            reply(sender, EnumChatFormatting.RED + "Task not found: " + args[0]);
            return;
        }
        reply(sender, EnumChatFormatting.YELLOW + "=== Task " + shortId(t.getUuid()) + " ===");
        reply(sender, EnumChatFormatting.GRAY + "Title:    " + EnumChatFormatting.WHITE + t.getTitle());
        if (t.getDescription() != null && !t.getDescription()
            .isEmpty()) {
            reply(sender, EnumChatFormatting.GRAY + "Desc:     " + EnumChatFormatting.WHITE + t.getDescription());
        }
        reply(sender, EnumChatFormatting.GRAY + "Status:   " + colorForStatus(t.getStatus()) + t.getStatus());
        reply(sender, EnumChatFormatting.GRAY + "Priority: " + EnumChatFormatting.WHITE + t.getPriority());
        reply(sender, EnumChatFormatting.GRAY + "Scope:    " + EnumChatFormatting.WHITE + t.getScope());
        reply(
            sender,
            EnumChatFormatting.GRAY + "Author:   " + EnumChatFormatting.WHITE + resolvePlayerName(t.getAuthor()));
        reply(sender, EnumChatFormatting.GRAY + "Version:  " + EnumChatFormatting.WHITE + t.getVersion());
        StringBuilder claimerNames = new StringBuilder();
        for (UUID c : t.getClaimers()) {
            if (claimerNames.length() > 0) claimerNames.append(", ");
            claimerNames.append(resolvePlayerName(c));
        }
        reply(
            sender,
            EnumChatFormatting.GRAY + "Claimers: "
                + EnumChatFormatting.WHITE
                + t.getClaimers()
                    .size()
                + "/"
                + t.getMaxClaimers()
                + (claimerNames.length() > 0 ? " (" + claimerNames + ")" : ""));
        if (t.getDeadline() != null) {
            long remainHours = (t.getDeadline() - System.currentTimeMillis()) / 3600_000L;
            reply(
                sender,
                EnumChatFormatting.GRAY + "Deadline: " + EnumChatFormatting.WHITE + "in " + remainHours + "h");
        }
    }

    private void handleClaim(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            reply(sender, EnumChatFormatting.RED + "Only players can claim tasks.");
            return;
        }
        if (args.length == 0) {
            reply(sender, EnumChatFormatting.RED + "Missing task id.");
            reply(sender, EnumChatFormatting.GRAY + "Usage: /nhkb claim <id>");
            return;
        }
        Task task = findByShortId(args[0]);
        if (task == null) {
            reply(sender, EnumChatFormatting.RED + "Task not found: " + args[0]);
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        UUID playerId = player.getUniqueID();

        // 业务条件原子判断(对应方案第九节第三层)
        synchronized (task) {
            if (task.getStatus()
                .isTerminal()) {
                reply(sender, EnumChatFormatting.RED + "Task is in terminal state: " + task.getStatus());
                return;
            }
            if (task.getStatus() != TaskStatus.OPEN && task.getStatus() != TaskStatus.CLAIMED) {
                reply(sender, EnumChatFormatting.RED + "Task cannot be claimed in status: " + task.getStatus());
                return;
            }
            if (task.getClaimers()
                .contains(playerId)) {
                reply(sender, EnumChatFormatting.YELLOW + "You have already claimed this task.");
                return;
            }
            if (task.getClaimers()
                .size() >= task.getMaxClaimers()) {
                reply(
                    sender,
                    EnumChatFormatting.RED + "Task is full ("
                        + task.getClaimers()
                            .size()
                        + "/"
                        + task.getMaxClaimers()
                        + ")");
                return;
            }
            task.getClaimers()
                .add(playerId);
            if (task.getClaimers()
                .size() >= task.getMaxClaimers()) {
                task.setStatus(TaskStatus.CLAIMED);
            }
            task.bumpVersion();
        }
        TaskRepository.get()
            .put(task);

        reply(sender, EnumChatFormatting.GREEN + "Task claimed: " + shortId(task.getUuid()));
        reply(
            sender,
            EnumChatFormatting.GRAY + "  claimers: "
                + EnumChatFormatting.WHITE
                + task.getClaimers()
                    .size()
                + "/"
                + task.getMaxClaimers()
                + EnumChatFormatting.GRAY
                + "  status: "
                + colorForStatus(task.getStatus())
                + task.getStatus());
    }

    private void handleSubmit(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            reply(sender, EnumChatFormatting.RED + "Only players can submit tasks.");
            return;
        }
        if (args.length == 0) {
            reply(sender, EnumChatFormatting.RED + "Missing task id.");
            reply(sender, EnumChatFormatting.GRAY + "Usage: /nhkb submit <id>");
            return;
        }
        Task task = findByShortId(args[0]);
        if (task == null) {
            reply(sender, EnumChatFormatting.RED + "Task not found: " + args[0]);
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        UUID playerId = player.getUniqueID();

        synchronized (task) {
            if (task.getStatus()
                .isTerminal()) {
                reply(sender, EnumChatFormatting.RED + "Task is in terminal state: " + task.getStatus());
                return;
            }
            if (!task.getClaimers()
                .contains(playerId)) {
                reply(sender, EnumChatFormatting.RED + "You must claim the task before submitting.");
                return;
            }
            if (task.getStatus() == TaskStatus.SUBMITTED) {
                reply(sender, EnumChatFormatting.YELLOW + "Task is already submitted, waiting for review.");
                return;
            }
            task.setStatus(TaskStatus.SUBMITTED);
            task.bumpVersion();
        }
        TaskRepository.get()
            .put(task);

        reply(sender, EnumChatFormatting.GREEN + "Task submitted: " + shortId(task.getUuid()));
        reply(
            sender,
            EnumChatFormatting.GRAY + "  Waiting for author "
                + EnumChatFormatting.WHITE
                + resolvePlayerName(task.getAuthor())
                + EnumChatFormatting.GRAY
                + " to review.");
    }

    private void handleReview(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            reply(sender, EnumChatFormatting.RED + "Usage: /nhkb review <id> <approve|reject>");
            return;
        }
        Task task = findByShortId(args[0]);
        if (task == null) {
            reply(sender, EnumChatFormatting.RED + "Task not found: " + args[0]);
            return;
        }

        String decision = args[1].toLowerCase();
        boolean approve;
        if ("approve".equals(decision) || "ok".equals(decision) || "yes".equals(decision)) {
            approve = true;
        } else if ("reject".equals(decision) || "no".equals(decision)) {
            approve = false;
        } else {
            reply(sender, EnumChatFormatting.RED + "Invalid decision: " + args[1] + " (use approve|reject)");
            return;
        }

        // 权限:发布者或 OP
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            boolean isAuthor = player.getUniqueID()
                .equals(task.getAuthor());
            boolean isOp = player.canCommandSenderUseCommand(2, "nhkb");
            if (!isAuthor && !isOp) {
                reply(sender, EnumChatFormatting.RED + "Only the task author or OP can review.");
                return;
            }
        }

        synchronized (task) {
            if (task.getStatus()
                .isTerminal()) {
                reply(sender, EnumChatFormatting.RED + "Task is in terminal state: " + task.getStatus());
                return;
            }
            if (task.getStatus() != TaskStatus.SUBMITTED) {
                reply(
                    sender,
                    EnumChatFormatting.RED + "Task is not awaiting review (status: " + task.getStatus() + ")");
                return;
            }
            if (approve) {
                task.setStatus(TaskStatus.COMPLETED);
            } else {
                task.setStatus(TaskStatus.CLAIMED);
            }
            task.bumpVersion();
        }
        TaskRepository.get()
            .put(task);

        if (approve) {
            TaskRepository.get()
                .archive(task.getUuid());
            reply(sender, EnumChatFormatting.GREEN + "Task approved and archived: " + shortId(task.getUuid()));
        } else {
            reply(
                sender,
                EnumChatFormatting.YELLOW + "Task rejected, returned to claimers: " + shortId(task.getUuid()));
        }
    }

    private void handleCancel(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            reply(sender, EnumChatFormatting.RED + "Missing task id.");
            reply(sender, EnumChatFormatting.GRAY + "Usage: /nhkb cancel <id>");
            return;
        }
        Task task = findByShortId(args[0]);
        if (task == null) {
            reply(sender, EnumChatFormatting.RED + "Task not found: " + args[0]);
            return;
        }

        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            boolean isAuthor = player.getUniqueID()
                .equals(task.getAuthor());
            boolean isOp = player.canCommandSenderUseCommand(2, "nhkb");
            if (!isAuthor && !isOp) {
                reply(sender, EnumChatFormatting.RED + "You can only cancel your own tasks.");
                return;
            }
        }

        if (task.getStatus()
            .isTerminal()) {
            reply(sender, EnumChatFormatting.RED + "Task is already in terminal state: " + task.getStatus());
            return;
        }

        task.setStatus(TaskStatus.CANCELLED);
        task.bumpVersion();
        TaskRepository.get()
            .put(task);
        TaskRepository.get()
            .archive(task.getUuid());

        reply(sender, EnumChatFormatting.GREEN + "Task cancelled: " + shortId(task.getUuid()));
    }

    // --- 工具方法 ---

    private void sendUsage(ICommandSender sender) {
        reply(sender, EnumChatFormatting.YELLOW + "=== NHKanBan Commands ===");
        reply(sender, EnumChatFormatting.AQUA + "/nhkb create -t <title> [flags...]");
        reply(sender, EnumChatFormatting.GRAY + "  Create a new task. Run with no args for full flag list.");
        reply(sender, EnumChatFormatting.AQUA + "/nhkb list");
        reply(sender, EnumChatFormatting.GRAY + "  List all active tasks.");
        reply(sender, EnumChatFormatting.AQUA + "/nhkb info <id>");
        reply(sender, EnumChatFormatting.GRAY + "  Show full details of a task.");
        reply(sender, EnumChatFormatting.AQUA + "/nhkb claim <id>");
        reply(sender, EnumChatFormatting.GRAY + "  Claim a task to start working on it.");
        reply(sender, EnumChatFormatting.AQUA + "/nhkb submit <id>");
        reply(sender, EnumChatFormatting.GRAY + "  Submit a claimed task for review.");
        reply(sender, EnumChatFormatting.AQUA + "/nhkb review <id> <approve|reject>");
        reply(sender, EnumChatFormatting.GRAY + "  Author/OP reviews a submitted task.");
        reply(sender, EnumChatFormatting.AQUA + "/nhkb cancel <id>");
        reply(sender, EnumChatFormatting.GRAY + "  Cancel a task (author or OP only).");
    }

    private void sendCreateUsage(ICommandSender sender) {
        reply(sender, EnumChatFormatting.YELLOW + "Usage: /nhkb create -t <title> [flags...]");
        reply(sender, EnumChatFormatting.GRAY + "Required:");
        reply(
            sender,
            EnumChatFormatting.GRAY + "  -t, --title     <title>                (max "
                + Task.MAX_TITLE_LENGTH
                + " chars, multi-word ok)");
        reply(sender, EnumChatFormatting.GRAY + "Optional:");
        reply(
            sender,
            EnumChatFormatting.GRAY + "  -d, --desc      <description>          (max "
                + Task.MAX_DESCRIPTION_LENGTH
                + " chars, multi-word ok)");
        reply(sender, EnumChatFormatting.GRAY + "  -p, --priority  LOW|NORMAL|HIGH|URGENT (default NORMAL)");
        reply(sender, EnumChatFormatting.GRAY + "  -s, --scope     GLOBAL|TEAM|DIRECT     (default GLOBAL)");
        reply(sender, EnumChatFormatting.GRAY + "  -c, --claimers  <number>               (default 1)");
        reply(sender, EnumChatFormatting.GRAY + "  -e, --deadline  <hours from now>");
        reply(sender, EnumChatFormatting.GRAY + "Note: -t and -d consume all words until the next -flag.");
        reply(sender, EnumChatFormatting.GRAY + "Examples:");
        reply(sender, EnumChatFormatting.GRAY + "  /nhkb create -t Build a fusion reactor");
        reply(sender, EnumChatFormatting.GRAY + "  /nhkb create -t Build reactor -p HIGH -c 3");
        reply(sender, EnumChatFormatting.GRAY + "  /nhkb create -t 大型工程 -d 需要五人协作完成 -e 48 -p URGENT -s TEAM -c 5");
    }

    private void reply(ICommandSender sender, String msg) {
        sender.addChatMessage(new ChatComponentText(msg));
    }

    private String shortId(UUID uuid) {
        return uuid.toString()
            .substring(0, 8);
    }

    /** UUID 解析为玩家名,在线优先,离线降级为短 UUID */
    private String resolvePlayerName(UUID uuid) {
        if (uuid == null) return "?";
        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
        if (server != null) {
            for (Object obj : server.getConfigurationManager().playerEntityList) {
                if (obj instanceof EntityPlayerMP) {
                    EntityPlayerMP p = (EntityPlayerMP) obj;
                    if (uuid.equals(p.getUniqueID())) {
                        return p.getCommandSenderName();
                    }
                }
            }
            try {
                com.mojang.authlib.GameProfile profile = server.func_152358_ax()
                    .func_152652_a(uuid);
                if (profile != null && profile.getName() != null) {
                    return profile.getName();
                }
            } catch (Throwable ignored) {}
        }
        return uuid.toString()
            .substring(0, 8);
    }

    private Task findByShortId(String shortId) {
        String prefix = shortId.toLowerCase();
        for (Task t : TaskRepository.get()
            .all()) {
            if (t.getUuid()
                .toString()
                .toLowerCase()
                .startsWith(prefix)) {
                return t;
            }
        }
        return null;
    }

    private EnumChatFormatting colorForStatus(TaskStatus status) {
        switch (status) {
            case OPEN:
                return EnumChatFormatting.AQUA;
            case CLAIMED:
                return EnumChatFormatting.YELLOW;
            case SUBMITTED:
                return EnumChatFormatting.GOLD;
            case COMPLETED:
                return EnumChatFormatting.GREEN;
            case CANCELLED:
                return EnumChatFormatting.DARK_GRAY;
            case EXPIRED:
                return EnumChatFormatting.RED;
            default:
                return EnumChatFormatting.WHITE;
        }
    }

    // --- 贪婪解析:从 start 开始,吞 token 直到下一个 -flag 或末尾 ---

    private static class Greedy {

        final String value;
        final int nextIndex;

        Greedy(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }

    /** 已知的 flag token,这些会终止贪婪吞噬 */
    private boolean isFlagToken(String s) {
        if (s == null || s.length() < 2 || s.charAt(0) != '-') return false;
        if (s.length() == 2) {
            char c = Character.toLowerCase(s.charAt(1));
            return c == 't' || c == 'd' || c == 'p' || c == 's' || c == 'c' || c == 'e';
        }
        return s.startsWith("--");
    }

    private Greedy readGreedy(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < args.length) {
            String tok = args[i];
            if (isFlagToken(tok)) break;
            if (sb.length() > 0) sb.append(' ');
            sb.append(tok);
            i++;
        }
        return new Greedy(sb.toString(), i);
    }
}
