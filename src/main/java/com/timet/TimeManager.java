package com.timet;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeManager {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH'h'mm:ss");
    private static final DateTimeFormatter SHORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH'h'mm");
    private static long lastDayTimeTicks = -1;

    public static LocalTime getPcTime() {
        return LocalTime.now();
    }

    public static LocalTime getEffectiveTime() {
        LocalTime pcNow = LocalTime.now();
        long offset = TimeConfig.get().offsetSeconds;
        if (offset != 0) {
            return pcNow.plusSeconds(offset);
        }
        return pcNow;
    }

    /**
     * Convertit une heure (00:00:00 à 23:59:59) en ticks Minecraft (0 à 23999).
     * 06:00:00 -> 0 ticks (lever du soleil)
     * 12:00:00 -> 6000 ticks (midi)
     * 18:00:00 -> 12000 ticks (coucher du soleil)
     * 00:00:00 -> 18000 ticks (minuit)
     */
    public static long calculateDayTimeTicks(LocalTime time) {
        double totalSeconds = time.toSecondOfDay() + (time.getNano() / 1_000_000_000.0);
        double ticks = ((totalSeconds / 86400.0) * 24000.0 + 18000.0) % 24000.0;
        return (long) Math.floor(ticks);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!TimeConfig.get().enabled) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }

        LocalTime effectiveTime = getEffectiveTime();
        long timeOfDayTicks = calculateDayTimeTicks(effectiveTime);

        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension() == Level.OVERWORLD || level.dimensionType().hasSkyLight()) {
                long currentDayTime = level.getDayTime();
                long currentDay = currentDayTime / 24000L;

                // Si l'heure passe 06:00 (tick 23999 -> tick 0), on incrémente le jour
                if (lastDayTimeTicks >= 0 && timeOfDayTicks < lastDayTimeTicks && (lastDayTimeTicks - timeOfDayTicks) > 12000) {
                    currentDay++;
                }

                long targetDayTime = currentDay * 24000L + timeOfDayTicks;
                if (currentDayTime != targetDayTime) {
                    level.setDayTime(targetDayTime);
                }
            }
        }
        lastDayTimeTicks = timeOfDayTicks;
    }

    public static void setTime(int hour, int minute, int second) {
        LocalTime now = LocalTime.now();
        int targetSeconds = hour * 3600 + minute * 60 + second;
        int currentSeconds = now.toSecondOfDay();
        long offset = (long) targetSeconds - currentSeconds;

        TimeConfig config = TimeConfig.get();
        config.offsetSeconds = offset;
        config.enabled = true;
        TimeConfig.save();
    }

    public static void setEnabled(boolean enabled) {
        TimeConfig config = TimeConfig.get();
        config.enabled = enabled;
        TimeConfig.save();
    }

    public static void resetOffset() {
        TimeConfig config = TimeConfig.get();
        config.offsetSeconds = 0;
        config.enabled = true;
        TimeConfig.save();
    }

    public static String formatPcTime() {
        return getPcTime().format(TIME_FORMATTER);
    }

    public static String formatEffectiveTime() {
        return getEffectiveTime().format(TIME_FORMATTER);
    }

    public static String formatShortEffectiveTime() {
        return getEffectiveTime().format(SHORT_TIME_FORMATTER);
    }

    public static String getOffsetDisplay() {
        long offset = TimeConfig.get().offsetSeconds;
        if (offset == 0) {
            return "Aucun (Synchronisé sur le PC)";
        }
        long absOffset = Math.abs(offset);
        long hours = absOffset / 3600;
        long minutes = (absOffset % 3600) / 60;
        long seconds = absOffset % 60;
        String sign = offset > 0 ? "+" : "-";
        return String.format("%s%02dh%02dm%02ds", sign, hours, minutes, seconds);
    }
}
