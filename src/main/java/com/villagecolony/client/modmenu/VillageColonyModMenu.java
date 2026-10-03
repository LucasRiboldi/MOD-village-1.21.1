package com.villagecolony.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Integra a tela de políticas ao botão de configuração do Mod Menu. */
public final class VillageColonyModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ProfessionMenuScreen::new;
    }
}
