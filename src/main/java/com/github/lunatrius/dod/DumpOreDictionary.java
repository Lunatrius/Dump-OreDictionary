package com.github.lunatrius.dod;

import com.github.lunatrius.dod.reference.Reference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION)
public class DumpOreDictionary {
    public static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();
    public static final FMLControlledNamespacedRegistry<Item> ITEM_REGISTRY = GameData.getItemRegistry();

    public static final String DELIMITER = ",";
    public static final String NEWLINE = "\n";

    @NetworkCheckHandler
    public boolean checkModList(final Map<String, String> versions, final Side side) {
        return true;
    }

    @Mod.EventHandler
    public void postInit(final FMLPostInitializationEvent event) {
        final Map<String, List<OreEntry>> dictionary = getDictionary();

        final File dumpDirectory = new File(getDataDirectory(), "dumps");
        if (!dumpDirectory.exists()) {
            if (!dumpDirectory.mkdirs()) {
                Reference.logger.warn("Could not create directory [{}]!", dumpDirectory.getAbsolutePath());
            }
        }

        final File fileJson = new File(dumpDirectory, "OreDictionary.json");
        final File fileCsv = new File(dumpDirectory, "OreDictionary.csv");

        final String dictionaryJson = getDictionaryJson(dictionary);
        final String dictionaryCSV = getDictionaryCsv(dictionary);

        try {
            writeToFile(fileJson, dictionaryJson);
            writeToFile(fileCsv, dictionaryCSV);
        } catch (final IOException e) {
            Reference.logger.error("Failed to write to files!", e);
        }
    }

    private Map<String, List<OreEntry>> getDictionary() {
        final Map<String, List<OreEntry>> dictionary = new LinkedHashMap<String, List<OreEntry>>();

        final String[] names = OreDictionary.getOreNames();
        Arrays.sort(names);

        for (final String name : names) {
            final ArrayList<OreEntry> list = new ArrayList<OreEntry>();

            for (final ItemStack itemStack : OreDictionary.getOres(name)) {
                final String stackName = getName(itemStack.getItem());
                final int meta = itemStack.getItemDamage();
                final String displayName = meta != OreDictionary.WILDCARD_VALUE ? itemStack.getItem().getItemStackDisplayName(itemStack) : "*";
                final OreEntry entry = new OreEntry(stackName, meta, displayName);
                list.add(entry);
            }

            dictionary.put(name, list);
        }

        return dictionary;
    }

    private String getName(final Item item) {
        if (item instanceof ItemBlock) {
            return String.valueOf(BLOCK_REGISTRY.getNameForObject(((ItemBlock) item).field_150939_a));
        }

        return String.valueOf(ITEM_REGISTRY.getNameForObject(item));
    }

    private String getDictionaryJson(Map<String, List<OreEntry>> dictionary) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(dictionary);
    }

    private String getDictionaryCsv(Map<String, List<OreEntry>> dictionary) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ore Name").append(DELIMITER).append("Item ID").append(DELIMITER).append("Meta").append(DELIMITER).append("Display name").append(NEWLINE);
        for (final Map.Entry<String, List<OreEntry>> entry : dictionary.entrySet()) {
            for (OreEntry oreEntry : entry.getValue()) {
                stringBuilder.append(entry.getKey());
                stringBuilder.append(DELIMITER);
                stringBuilder.append(oreEntry.name);
                stringBuilder.append(DELIMITER);
                stringBuilder.append(oreEntry.meta);
                stringBuilder.append(DELIMITER);
                stringBuilder.append(oreEntry.displayName);
                stringBuilder.append(NEWLINE);
            }
        }
        return stringBuilder.toString();
    }

    private File getDataDirectory() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return Minecraft.getMinecraft().mcDataDir;
        }

        return new File(".");
    }

    private void writeToFile(final File file, final String content) throws IOException {
        final FileWriter fileWriter = new FileWriter(file);
        final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        try {
            bufferedWriter.write(content);
        } finally {
            bufferedWriter.close();
        }
    }
}
