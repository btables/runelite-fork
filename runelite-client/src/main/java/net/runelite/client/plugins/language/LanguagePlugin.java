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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.MenuEntry;
import net.runelite.api.Client;

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

@PluginDescriptor(
        name = "Language",
        description = "Translates in game text",
        tags = {"languages"},
        enabledByDefault = true
)
public class LanguagePlugin extends Plugin
{

    private static Translate translate;

    private final String sourceLanguage = "en";
    private final String destinationLanguage = "es";

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
    }

    @Override
    protected void shutDown()
    {
        // TODO: Write Cache.
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        MenuEntry[] entries = event.getMenuEntries();
        for (int idx = entries.length - 1; idx >= 0; --idx)
        {
            MenuEntry entry = entries[idx];
            String translatedOption = translateText(entry.getOption());
            entry.setOption(translatedOption);
        }
    }

    private String translateText (String sourceText) {
        Translation translation =
                translate.translate(
                        sourceText,
                        Translate.TranslateOption.sourceLanguage(sourceLanguage),
                        Translate.TranslateOption.targetLanguage(destinationLanguage));

        return translation.getTranslatedText();
    }



}
