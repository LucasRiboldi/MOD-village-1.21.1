package com.villagecolony.client.modmenu;

import com.villagecolony.client.network.ClientProfessionPolicies;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Locale;

/** Tela inicial do menu: uma entrada para cada uma das oito profissões reais. */
final class ProfessionMenuScreen extends Screen {
    private final Screen parent;
    private ProfessionPolicySet renderedPolicies;

    ProfessionMenuScreen(Screen parent) {
        super(Text.translatable("text.villagecolony.professions"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ClientProfessionPolicies.request();
        rebuild();
    }

    @Override
    public void tick() {
        ProfessionPolicySet current = ClientProfessionPolicies.current().orElse(null);
        if (current != renderedPolicies) rebuild();
    }

    private void rebuild() {
        clearChildren();
        renderedPolicies = ClientProfessionPolicies.current().orElse(null);
        if (renderedPolicies == null) return;
        int left = width / 2 - 100;
        int top = 48;
        int index = 0;
        for (ProfessionType type : ProfessionType.values()) {
            addDrawableChild(ButtonWidget.builder(professionName(type), button ->
                    client.setScreen(new ProfessionDetailScreen(this, type)))
                    .dimensions(left, top + index++ * 24, 200, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, button -> close())
                .dimensions(left, top + index * 24 + 8, 200, 20).build());
    }

    @Override public void close() { client.setScreen(parent); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFFFF);
        if (renderedPolicies == null) context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("text.villagecolony.professions.loading"), width / 2, 52, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    static Text professionName(ProfessionType type) {
        return Text.translatable("text.villagecolony.profession." + type.name().toLowerCase(Locale.ROOT));
    }

}
