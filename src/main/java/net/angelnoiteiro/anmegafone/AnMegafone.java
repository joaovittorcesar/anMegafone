package net.angelnoiteiro.anmegafone;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnMegafone extends JavaPlugin implements Listener {

    private Map<UUID, Long> cooldowns = new HashMap<>();
    private long defaultCooldown = 5 * 60 * 1000; // 5 minutos em milissegundos

    @Override
    public void onEnable() {
        getLogger().info("MegaFone iniciado com sucesso!");

        this.saveDefaultConfig();
        defaultCooldown = parseCooldownTime(this.getConfig().getString("temporizador.timer"));
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        getCommand("megafone").setExecutor(this);
        getCommand("anmegafone").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("MegaFone desativado com sucesso!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("anmegafone")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    return reloadPlugin(sender);
                }
            }
        }

        if (cmd.getName().equalsIgnoreCase("megafone")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Este comando só pode ser usado por jogadores.");
                return true;
            }

            Player player = (Player) sender;

            if (sender.hasPermission("anmegafone.use")) {
                if (args.length == 0) {
                    sender.sendMessage(getConfigMessage("prefix") + getConfigMessage("messages.no_args"));
                    return true;
                }

                if (!checkCooldown(player) && !sender.hasPermission("anmegafone.bypass")) {
                    long remainingTime = getRemainingTime(player);
                    sender.sendMessage(getConfigMessage("prefix") + getConfigMessage("messages.wait").replace("{time}", formatTime(remainingTime)));
                    return true;
                }

                String message = String.join(" ", args);
                broadcastMessage(sender, message);
                setCooldown(player);
            } else {
                sender.sendMessage(getConfigMessage("prefix") + getConfigMessage("messages.no_permission"));
            }
            return true;
        }
        return true;
    }

    private boolean reloadPlugin(CommandSender sender) {
        if (!sender.hasPermission("anmegafone.reload")) {
            sender.sendMessage(getConfigMessage("prefix") + getConfigMessage("messages.no_permission"));
            return true;
        }

        this.reloadConfig();
        defaultCooldown = parseCooldownTime(this.getConfig().getString("temporizador.timer"));
        sender.sendMessage(getConfigMessage("prefix") + getConfigMessage("messages.reload"));
        return true;
    }

    private boolean checkCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (cooldowns.containsKey(uuid)) {
            long currentTime = System.currentTimeMillis();
            long lastUseTime = cooldowns.get(uuid);
            return currentTime >= lastUseTime + defaultCooldown;
        }
        return true;
    }

    private void setCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    private long getRemainingTime(Player player) {
        UUID uuid = player.getUniqueId();
        long lastUseTime = cooldowns.get(uuid);
        long elapsedTime = System.currentTimeMillis() - lastUseTime;
        return defaultCooldown - elapsedTime;
    }

    private void broadcastMessage(CommandSender sender, String message) {
        String broadcastPrefix = getConfigMessage("messages.broadcast_prefix");
        String playerName = sender.getName();
        String formattedMessage = String.format("%s - %s: %s", broadcastPrefix, playerName, message);

        Bukkit.broadcastMessage("                                                                ");
        Bukkit.broadcastMessage(formattedMessage);
        Bukkit.broadcastMessage("                                                                ");
    }

    private long parseCooldownTime(String cooldownString) {
        long cooldownTime = defaultCooldown;

        try {
            if (cooldownString != null && !cooldownString.isEmpty()) {
                if (cooldownString.endsWith("s")) {
                    cooldownTime = Long.parseLong(cooldownString.substring(0, cooldownString.length() - 1)) * 1000;
                } else if (cooldownString.endsWith("m")) {
                    cooldownTime = Long.parseLong(cooldownString.substring(0, cooldownString.length() - 1)) * 60 * 1000;
                } else if (cooldownString.endsWith("h")) {
                    cooldownTime = Long.parseLong(cooldownString.substring(0, cooldownString.length() - 1)) * 60 * 60 * 1000;
                } else if (cooldownString.endsWith("d")) {
                    cooldownTime = Long.parseLong(cooldownString.substring(0, cooldownString.length() - 1)) * 24 * 60 * 60 * 1000;
                }
            }
        } catch (NumberFormatException e) {
            getLogger().warning("§cConfiguração inválida para o temporizador. Usando valor padrão de 5 minutos.");
        }
        return cooldownTime;
    }

    private String formatTime(long timeMillis) {
        long seconds = timeMillis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String getConfigMessage(String path) {
        return getConfig().getString(path).replace("&", "§");
    }
}
