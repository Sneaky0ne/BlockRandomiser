//Плагин был разработан SneakyOne w/ TSRSE
package mc.sneakyone.yt.com.sneakyoneplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SneakyOnePlugin extends JavaPlugin implements Listener, Runnable {

    //Ищем нужного нам игрока
    private final String targetPlayerNick = "PlayerName";

    //Добавляем нужные нам переменные
    private int range;
    private int currentBlockAmount, targetBlockAmount, multiplier;
    private int ticks, ticksDesc;
    private int waveEvery, passedSets;
    private boolean fillAir, reachedMaxTicks = false;

    private int threadID;

    //Создаем список заполняемых матерьялов
    ArrayList<Material> blockList = new ArrayList<Material>();

    //Метод, активируемый при включении плагина
    @Override
    public void onEnable()
    {
        //Создание Конфига
        File config = new File(getDataFolder() + File.separator + "config.yml");
        if (!config.exists())
        {
            getLogger().info("Creating new config...");
            getConfig().options().copyDefaults(true);
            saveDefaultConfig();
        }
        //Создание Файла блоков
        File userBlockList = new File(getDataFolder() + File.separator + "blocks.yaml");
        if (!userBlockList.exists())
        {
            getLogger().info("Creating new blocks config...");
            try {userBlockList.createNewFile();}
            catch (IOException e) { e.printStackTrace();}
        }
        FileConfiguration getBlocks = YamlConfiguration.loadConfiguration(userBlockList);
        List<String> blockNamesList = getBlocks.getStringList("blocks");
        for (String s : blockNamesList) blockList.add(Material.valueOf(s));

        //Получаем данные с конфига
        multiplier = getConfig().getInt("Multiplier");
        targetBlockAmount = getConfig().getInt("Blocks");
        ticks = getConfig().getInt("StartingTicks");
        ticksDesc = getConfig().getInt("DescendingTicks");
        waveEvery = getConfig().getInt("WaveEvery");
        fillAir = getConfig().getBoolean("FillAir");
        range = getConfig().getInt("Range");

        Bukkit.getPluginManager().registerEvents(this,this);
        getCommand("startfill").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
                Player p = Bukkit.getPlayer(targetPlayerNick);
                if (p != null)
                {
                    if (threadID != 0)
                    {
                        Bukkit.getScheduler().cancelTask(threadID);
                        p.sendMessage(ChatColor.GOLD + "[SO:PG] " + ChatColor.RED + "Плагин деактивирован!");
                        threadID = 0;
                        return true;
                    }
                    threadID = Bukkit.getScheduler().scheduleSyncRepeatingTask(SneakyOnePlugin.this, SneakyOnePlugin.this, 0, ticks);
                    p.sendMessage(ChatColor.GOLD + "[SO:PG] " + ChatColor.GREEN + "Плагин активирован!");
                    p.sendMessage(ChatColor.GOLD + "[SO:PG] Первая волна!\n" + ChatColor.GRAY + "Сечас меняется " + targetBlockAmount + " блока(ов) каждые " + ticks + " тиков!\nНовая волна через " + waveEvery + " замен!");
                    return true;
                }
                return  false;
            }
        });

        getLogger().info("Activated!");
    }

    //Метод, активируемый при выключении плагина
    @Override
    public void onDisable()
    {
        getLogger().info("Deactivated!");
    }

    //Постоянный метод
    @Override
    public void run()
    {
        Player p = Bukkit.getPlayer(targetPlayerNick);
        if(p == null) return;

        if(passedSets == waveEvery)
        {
            //Проверяем множитель
            if (multiplier == 0 || reachedMaxTicks && !getConfig().getBoolean("SavageMod"))
                targetBlockAmount++;
            else if(targetBlockAmount != range*range*40)
                targetBlockAmount = targetBlockAmount * multiplier;
            else
                targetBlockAmount = range*range*40;

            ticks -= ticksDesc;
            if (ticks < 20)
            {
                ticks = 20;
                if (!reachedMaxTicks){
                    p.sendMessage(ChatColor.GOLD + "[SO:PG]" + ChatColor.RESET + ChatColor.GOLD + ChatColor.MAGIC + " Новая волна!\n" + ChatColor.DARK_RED + ChatColor.MAGIC + "Теперь меняется " + targetBlockAmount + " блока(ов) каждые " + ticks + " тиков!\n"+ ChatColor.RESET + ChatColor.RED + " ПОЛНЫЙ ХАОС БЛОКИ СТАВЯТСЯ КАЖДУЮ СЕКУНДУ, НОВЫХ ВОЛ БОЛЬШЕ НЕТ, НО КОЛИЧЕСТВО БЛОКОВ БУДЕТ УВЕЛИЧИВАТЬСЯ");
                    reachedMaxTicks = true;
                }
            }
            else
                p.sendMessage(ChatColor.GOLD + "[SO:PG] Новая волна!\n" + ChatColor.GRAY + "Теперь меняется " + targetBlockAmount + " блока(ов) каждые " + ticks + " тиков!\nНовая волна через " + waveEvery + " замен!");

            passedSets = 0;

            Bukkit.getScheduler().cancelTask(threadID);
            threadID = Bukkit.getScheduler().scheduleSyncRepeatingTask(SneakyOnePlugin.this, SneakyOnePlugin.this, 0, ticks);
            return;
        }

        while (targetBlockAmount > currentBlockAmount)
        {
            Block newBlock;
            Random rnd = new Random();

            Location pLocation = p.getLocation();
            Double X = pLocation.getX();
            Double Y = pLocation.getY();
            Double Z = pLocation.getZ();

            int nX = X.intValue() - range + rnd.nextInt(range * 2);
            int nY = Y.intValue() - 10 + rnd.nextInt(30);
            int nZ = Z.intValue() - range + rnd.nextInt(range * 2);

            Location newLocation = p.getLocation();
            newLocation.set(nX, nY, nZ);
            newBlock = newLocation.getBlock();

            //Проверка на флаг заполнения воздуха
            if (!fillAir && (newLocation.getBlock().getType() == Material.AIR || newLocation.getBlock().getType() == Material.VOID_AIR || newLocation.getBlock().getType() == Material.BEDROCK))
            {
                run();
                return;
            }

            newBlock.setType(blockList.get(rnd.nextInt(blockList.size())));

            if(getConfig().getBoolean("DebugMode"))
                p.sendMessage(ChatColor.GRAY + "[SO:PG | DEBUG] Был выставлен новый блок на координатах" + "\nX: " + nX + " | Y: " + nY + " | Z: " + nZ + "\n");
            currentBlockAmount++;
        }
        passedSets++;
        currentBlockAmount = 0;
    }
}