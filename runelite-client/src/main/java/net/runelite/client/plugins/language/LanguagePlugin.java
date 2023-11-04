/*
 * Copyright (c) 2018, Lotto <https://github.com/devLotto>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.language;

import com.google.api.client.json.Json;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;

import java.io.FileNotFoundException;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Client;

import com.google.gson.Gson;
import net.runelite.api.NpcID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.NullNpcID;
import net.runelite.api.Player;
import net.runelite.api.events.MenuOpened;

import net.runelite.client.callback.Hooks;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import com.google.cloud.translate.Translation;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translation;
import com.google.cloud.translate.testing.RemoteTranslateHelper;

enum TranslationType {
    TARGET,
    OPTION,
    DIALOG,
}

@PluginDescriptor(
        name = "Language",
        description = "Translates in game text",
        tags = {"languages"},
        enabledByDefault = true
)
// TODO: Tooltip
// TODO: Quest list
// TODO: Bottom tabs (all, public, etc)
// TODO: Capitalize first letter.
public class LanguagePlugin extends Plugin
{
    private static Translate translate;

    private final String sourceLanguage = "en";
    private final String destinationLanguage = "es";
    private final String filePath = new File("").getAbsolutePath();
    private JsonObject translationsCache;

    @Inject
    private Client client;

    @Inject
    private Hooks hooks;

    @Inject
    private NpcUtil npcUtil;


    @Override
    protected void startUp()
    {
        RemoteTranslateHelper helper = RemoteTranslateHelper.create();
        translate = helper.getOptions().getService();

        populateCache();
    }

    @Override
    protected void shutDown()
    {
        writeCache();
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        MenuEntry[] entries = event.getMenuEntries();
        for (int idx = entries.length - 1; idx >= 0; --idx)
        {
            MenuEntry entry = entries[idx];
            translateMenuOption(entry);
            maybeTranslateMenuTarget(entry);
        }
    }


    private void translateMenuOption(MenuEntry entry)
    {
        String translatedOption = translateText(entry.getOption(), "to ", TranslationType.OPTION);
        entry.setOption(translatedOption);
    }

    private void maybeTranslateMenuTarget(MenuEntry entry)
    {
        if (!shouldTranslateTarget(entry)) {
            return;
        }

        String translatedTarget = translateText(entry.getTarget(), "a ", TranslationType.TARGET);
        entry.setTarget(translatedTarget);
    }

    private boolean shouldTranslateTarget(MenuEntry entry) {
        MenuAction action = entry.getType();
        switch (action) {
            // Don't translate players
            case PLAYER_FIRST_OPTION:
            case PLAYER_SECOND_OPTION:
            case PLAYER_THIRD_OPTION:
            case PLAYER_FOURTH_OPTION:
            case PLAYER_FIFTH_OPTION:
            case PLAYER_SIXTH_OPTION:
            case PLAYER_SEVENTH_OPTION:
            case PLAYER_EIGHTH_OPTION:
                return false;
            default:
                return true;
        }
    }

    private void populateCache() {
        Gson gson = new Gson();
        try {
            BufferedReader bufferedReader  = new BufferedReader(new FileReader(filePath + "\\runelite-client\\src\\main\\java\\net\\runelite\\client\\plugins\\language\\translations.json"));
            translationsCache = gson.fromJson(bufferedReader, JsonObject.class);
            System.out.println("Translation cache loaded with " + translationsCache.toString());
        }
        catch (FileNotFoundException ex) {
            System.out.println("Unable to open cache files. " + ex.getMessage());
        }
    }

    private void writeCache() {
        try {
            FileWriter fw = new FileWriter(filePath + "\\runelite-client\\src\\main\\java\\net\\runelite\\client\\plugins\\language\\translations.json", false);
            fw.write(translationsCache.toString());
            fw.flush();
        }
        catch (Exception e) {
            System.out.println("Unable to write to file: " + e.getMessage());
        }
    }

    // TODO: Wrap in promise.
    private String translateText (String sourceText, String prefix, TranslationType type) {

        JsonObject cache = this.translationsCache.getAsJsonObject(type.name());


        if (cache.has(sourceText)) {
            System.out.println("Cache already contained: " + sourceText);
            return cache.get(sourceText).getAsString();
        }

        int closingColorIndex = sourceText.indexOf(">");
        boolean isColoredText = closingColorIndex != -1;

        if (isColoredText) {

        }


        Translation translation =
                translate.translate(
                        sourceText,
                        Translate.TranslateOption.sourceLanguage(sourceLanguage),
                        Translate.TranslateOption.targetLanguage(destinationLanguage));

        String translatedText = StringUtils.capitalize(translation.getTranslatedText());
        cache.addProperty(sourceText, translatedText);

        return translatedText;
    }
}