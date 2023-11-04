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

import org.apache.commons.lang3.StringUtils;
import com.google.gson.JsonObject;

import java.lang.Character;
import java.lang.StringBuilder;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Client;



import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.ChatMessage;

import net.runelite.client.callback.Hooks;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.api.ChatMessageType;
import net.runelite.api.MessageNode;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.eventbus.Subscribe;

import com.google.gson.Gson;
import com.google.cloud.translate.Translation;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.testing.RemoteTranslateHelper;

enum TranslationType {
    TARGET,
    OPTION,
    DIALOG,
    CHAT_MESSAGE,
}

@PluginDescriptor(
        name = "Language",
        description = "Translates in game text",
        tags = {"languages"},
        enabledByDefault = true
)
// TODO: Tooltip (top left and runelite)
// TODO: Chat box minus player messages
// TODO: Quest list
// TODO: Static widgets, Bottom tabs (all, public, etc), "Friends list",
// TODO: Subscribe to client shutdown.
// TODO: Somehow hold shift to translate back?
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

    @Inject
    private ChatMessageManager chatMessageManager;


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
        // First find all the player targets in the current menu.
        Set<String> playerSet = new HashSet<String>();

        for (int idx = entries.length - 1; idx >= 0; --idx)
        {
            MenuEntry entry = entries[idx];
            if (!shouldTranslateTarget(entry)) {
                playerSet.add(entry.getTarget());
            }
        }
        for (int idx = entries.length - 1; idx >= 0; --idx)
        {
            MenuEntry entry = entries[idx];
            translateMenuOption(entry);
            if (!playerSet.contains(entry.getTarget())) {
                System.out.println("Translating target because it was not a player" + entry.getTarget() + "it had option " + entry.getOption());
                translateMenuTarget(entry);
            }
        }
    }


    private void translateMenuOption(MenuEntry entry)
    {
        String translatedOption = translateText(entry.getOption(), "to ", TranslationType.OPTION);
        entry.setOption(translatedOption);
    }

    private void translateMenuTarget(MenuEntry entry)
    {
        String translatedTarget = translateText(entry.getTarget(), "a ", TranslationType.TARGET);
        entry.setTarget(translatedTarget);
    }

    private boolean shouldTranslateTarget(MenuEntry entry) {
        MenuAction action = entry.getType();
        System.out.println("entry action is " + action);
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

    private boolean shouldTranslateChatMessage(ChatMessageType messageType) {
        switch (messageType) {
            case GAMEMESSAGE:
            case TRADE_SENT:
            case TRADE:
            case NPC_EXAMINE:
            case WELCOME:
            case TRADEREQ:
            case ITEM_EXAMINE:
            case OBJECT_EXAMINE:
            case FRIENDNOTIFICATION:
            case IGNORENOTIFICATION:
            case CLAN_CHAT:
            case BROADCAST:
            case CLAN_MESSAGE:
            case LOGINLOGOUTNOTIFICATION:
            case DIALOG:
                return true;
            default:
                return false;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (!shouldTranslateChatMessage(event.getType())) {
            return;
        }

        String translatedText = translateText(event.getMessage(), "", TranslationType.CHAT_MESSAGE);

        final MessageNode messageNode = event.getMessageNode();
        messageNode.setRuneLiteFormatMessage(translatedText);
        chatMessageManager.update(messageNode);
        client.refreshChat();
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
        if (sourceText.length() == 0) {
            return "";
        }

        JsonObject cache = this.translationsCache.getAsJsonObject(type.name());

        if (cache.has(sourceText)) {
            System.out.println("English: " + sourceText);
            System.out.println("Spanish: " + cache.get(sourceText).getAsString());
            return cache.get(sourceText).getAsString();
        }

        int colorIndex = sourceText.indexOf(">");
        String textToTranslate = sourceText;
        String color = "";
        if (colorIndex != -1) {
            color = sourceText.substring(0, colorIndex+1);
            textToTranslate = textToTranslate.substring(colorIndex+1);
        }

        System.out.println("translating " + prefix + textToTranslate);
        Translation translation =
                translate.translate(
                        prefix + textToTranslate,
                        Translate.TranslateOption.sourceLanguage(sourceLanguage),
                        Translate.TranslateOption.targetLanguage(destinationLanguage));

        String formattedText = color + StringUtils.capitalize(translation.getTranslatedText());



        String translatedText = translation.getTranslatedText();

        System.out.println("got back " + formattedText);


//        // Capitalize the first non-color character
//        int charIndexToCapitalize = translation.getTranslatedText().indexOf('>') + 1;
//        System.out.println("Trying to message " + translatedText);
//        System.out.println("Trying to capitalize char at " + charIndexToCapitalize);
//        char charInUpperCase = Character.toUpperCase(translatedText.charAt(charIndexToCapitalize));
//        StringBuilder capitalizedTextBuilder = new StringBuilder(translatedText);
//        capitalizedTextBuilder.setCharAt(charIndexToCapitalize, charInUpperCase);
//        String finalText =  capitalizedTextBuilder.toString();
//
        // Add to the corresponding cache so we no longer translate this string
        cache.addProperty(sourceText, formattedText);

        System.out.println("English: " + sourceText);
        System.out.println("Spanish: " +formattedText);

        return formattedText;
    }
}