package com.timet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeCommand {

    private static final Pattern PATTERN_H_MIN = Pattern.compile("(?i)^(\\d{1,2})h(\\d{1,2})(?:m|min)?(?::(\\d{1,2})s?)?$");
    private static final Pattern PATTERN_COLON = Pattern.compile("^(\\d{1,2}):(\\d{1,2})(?::(\\d{1,2}))?$");
    private static final Pattern PATTERN_HOUR_ONLY = Pattern.compile("(?i)^(\\d{1,2})h?$");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("timet")
                .requires(source -> source.hasPermission(0))
                .executes(TimeCommand::showInfo)
                .then(Commands.literal("on")
                    .executes(TimeCommand::enableSync))
                .then(Commands.literal("off")
                    .executes(TimeCommand::disableSync))
                .then(Commands.literal("sync")
                    .executes(TimeCommand::resetSync))
                .then(Commands.literal("reset")
                    .executes(TimeCommand::resetSync))
                .then(Commands.literal("status")
                    .executes(TimeCommand::showStatus))
                .then(Commands.literal("set")
                    .then(Commands.argument("heure", StringArgumentType.greedyString())
                        .executes(TimeCommand::setTimeArg)))
        );
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean enabled = TimeConfig.get().enabled;

        source.sendSuccess(() -> Component.literal("§8--- §6§lTimeT §7(Synchronisation Temps Réel) §8---"), false);
        source.sendSuccess(() -> Component.literal("§7Statut : " + (enabled ? "§a§lACTIF §7(Temps Réel)" : "§c§lINACTIF §7(Cycle Minecraft classique)")), false);
        source.sendSuccess(() -> Component.literal("§7Heure Réelle du PC : §e" + TimeManager.formatPcTime()), false);
        source.sendSuccess(() -> Component.literal("§7Heure en Jeu : §b" + TimeManager.formatEffectiveTime()), false);
        source.sendSuccess(() -> Component.literal("§7Décalage : §f" + TimeManager.getOffsetDisplay()), false);
        source.sendSuccess(() -> Component.literal("§8---------------------------------"), false);
        source.sendSuccess(() -> Component.literal("§e/timet on §7: Activer la synchronisation temps réel"), false);
        source.sendSuccess(() -> Component.literal("§e/timet off §7: Désactiver la synchronisation"), false);
        source.sendSuccess(() -> Component.literal("§e/timet set <heure> §7: Régler l'heure (ex: §b/timet set 15h21§7)"), false);
        source.sendSuccess(() -> Component.literal("§e/timet sync §7: Recoller directement à l'heure du PC"), false);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        return showInfo(context);
    }

    private static int enableSync(CommandContext<CommandSourceStack> context) {
        TimeManager.setEnabled(true);
        context.getSource().sendSuccess(() -> Component.literal(
            "§8[§6TimeT§8] §aSynchronisation de l'heure du PC §2§lACTIVÉE§a ! L'heure du monde suit désormais le temps réel (§e"
                + TimeManager.formatEffectiveTime() + "§a)."
        ), true);
        return 1;
    }

    private static int disableSync(CommandContext<CommandSourceStack> context) {
        TimeManager.setEnabled(false);
        context.getSource().sendSuccess(() -> Component.literal(
            "§8[§6TimeT§8] §cSynchronisation §4§lDÉSACTIVÉE§c. Le cycle jour/nuit classique de Minecraft (20 min) est rétabli."
        ), true);
        return 1;
    }

    private static int resetSync(CommandContext<CommandSourceStack> context) {
        TimeManager.resetOffset();
        context.getSource().sendSuccess(() -> Component.literal(
            "§8[§6TimeT§8] §aDécalage réinitialisé ! L'heure est maintenant exactement calée sur votre PC (§e"
                + TimeManager.formatPcTime() + "§a)."
        ), true);
        return 1;
    }

    private static int setTimeArg(CommandContext<CommandSourceStack> context) {
        String input = StringArgumentType.getString(context, "heure").trim();
        ParsedTime parsed = parseTime(input);

        if (parsed == null) {
            context.getSource().sendFailure(Component.literal(
                "§8[§6TimeT§8] §cFormat d'heure invalide : '§e" + input + "§c'. Formats acceptés : §b15h21§c, §b15:21§c, §b8h30§c, §b15h§c, §b22§c."
            ));
            return 0;
        }

        TimeManager.setTime(parsed.hour, parsed.minute, parsed.second);
        String formatted = String.format("%02dh%02d", parsed.hour, parsed.minute);

        context.getSource().sendSuccess(() -> Component.literal(
            "§8[§6TimeT§8] §aL'heure du monde a été réglée sur §e§l" + formatted + "§a (Synchronisation temps réel active, décalage : §b"
                + TimeManager.getOffsetDisplay() + "§a)."
        ), true);
        return 1;
    }

    private static ParsedTime parseTime(String input) {
        if (input == null || input.isEmpty()) return null;

        // Test format 15h21 ou 15h21min ou 15h21:30
        Matcher matcher = PATTERN_H_MIN.matcher(input);
        if (matcher.matches()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            int second = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            if (isValidTime(hour, minute, second)) {
                return new ParsedTime(hour, minute, second);
            }
        }

        // Test format 15:21 ou 15:21:30
        matcher = PATTERN_COLON.matcher(input);
        if (matcher.matches()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            int second = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            if (isValidTime(hour, minute, second)) {
                return new ParsedTime(hour, minute, second);
            }
        }

        // Test format 15h ou 15
        matcher = PATTERN_HOUR_ONLY.matcher(input);
        if (matcher.matches()) {
            int hour = Integer.parseInt(matcher.group(1));
            if (isValidTime(hour, 0, 0)) {
                return new ParsedTime(hour, 0, 0);
            }
        }

        return null;
    }

    private static boolean isValidTime(int hour, int minute, int second) {
        return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59 && second >= 0 && second <= 59;
    }

    private record ParsedTime(int hour, int minute, int second) {}
}
