package net.mack.boringmods.client.options;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ModOptions {
    public static final String MOD_ID = "BoringMods";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new Gson();

    private File configFile;

    public int excavateMaxBlocks = 64;
    public int excavateRange = 8;
    public int tunnelLong = 16;
    public int tunnelWidth = 2;
    public int tunnelHeight = 3;
    public boolean lightOverlayEnabled = false;
    public int lightOverlayRange = 12;

    ModOptions() {
        this.configFile = new File(FabricLoader.getInstance().getConfigDirectory(), "options.txt");

    }

    public void write() {
        try {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.configFile), StandardCharsets.UTF_8));
            Throwable throwedError;

            try {

            } catch (Throwable throwed) {
                LOGGER.error("", throwed);
                throwedError = throwed;
                throw throwed;
            } finally {

            }
        } catch (Exception ex) {
            LOGGER.error("Failed to save mod options.", ex);
        }
    }
}
