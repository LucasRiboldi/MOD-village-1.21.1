package com.villagecolony.client.modmenu;

import com.villagecolony.client.network.ClientProfessionPolicies;
import com.villagecolony.core.worker.model.ProfessionPolicy;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Controles que se aplicam de fato à profissão selecionada. */
final class ProfessionDetailScreen extends Screen {
    private final Screen parent;
    private final ProfessionType type;
    private ProfessionPolicySet renderedPolicies;

    ProfessionDetailScreen(Screen parent, ProfessionType type) {
        super(ProfessionMenuScreen.professionName(type));
        this.parent = parent;
        this.type = type;
    }

    @Override protected void init() { ClientProfessionPolicies.request(); rebuild(); }

    @Override public void tick() {
        ProfessionPolicySet current = ClientProfessionPolicies.current().orElse(null);
        if (current != renderedPolicies) rebuild();
    }

    private void rebuild() {
        clearChildren();
        renderedPolicies = ClientProfessionPolicies.current().orElse(null);
        if (renderedPolicies == null) return;
        ProfessionPolicy policy = renderedPolicies.policyOf(type);
        int left = width / 2 - 100;
        int top = 52;
        addDrawableChild(ButtonWidget.builder(enabledLabel(policy),
                button -> replace(new ProfessionPolicy(!policy.enabled(), policy.maximumWorkers(), policy.searchRadius())))
                .dimensions(left, top, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> replace(new ProfessionPolicy(policy.enabled(),
                Math.max(0, policy.maximumWorkers() - 1), policy.searchRadius())))
                .dimensions(left, top + 26, 20, 20).build());
        ButtonWidget maximum = addDrawableChild(ButtonWidget.builder(maximumLabel(policy), button -> { })
                .dimensions(left + 24, top + 26, 152, 20).build());
        maximum.active = false;
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> replace(new ProfessionPolicy(policy.enabled(),
                Math.min(ProfessionPolicy.MAXIMUM_WORKERS_LIMIT, policy.maximumWorkers() + 1), policy.searchRadius())))
                .dimensions(left + 180, top + 26, 20, 20).build());

        int next = top + 52;
        if (ProfessionPolicySet.supportsSearchRadius(type)) {
            addDrawableChild(ButtonWidget.builder(radiusLabel(policy), button -> replace(new ProfessionPolicy(policy.enabled(),
                    policy.maximumWorkers(), nextRadius(policy.searchRadius())))).dimensions(left, next, 200, 20).build());
            next += 26;
        }
        int priority = renderedPolicies.hiringOrder().indexOf(type) + 1;
        ButtonWidget priorityLabel = addDrawableChild(ButtonWidget.builder(
                Text.translatable("text.villagecolony.professions.priority", priority), button -> { })
                .dimensions(left, next, 200, 20).build());
        priorityLabel.active = false;
        ButtonWidget up = addDrawableChild(ButtonWidget.builder(Text.translatable("text.villagecolony.professions.move_up"),
                button -> move(-1)).dimensions(left, next + 26, 98, 20).build());
        up.active = priority > 1;
        ButtonWidget down = addDrawableChild(ButtonWidget.builder(Text.translatable("text.villagecolony.professions.move_down"),
                button -> move(1)).dimensions(left + 102, next + 26, 98, 20).build());
        down.active = priority < ProfessionType.values().length;
        addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, button -> close())
                .dimensions(left, next + 58, 200, 20).build());
    }

    private void replace(ProfessionPolicy policy) {
        EnumMap<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(renderedPolicies.policies());
        policies.put(type, policy);
        submit(new ProfessionPolicySet(policies, renderedPolicies.hiringOrder()));
    }

    private void move(int direction) {
        List<ProfessionType> order = new ArrayList<>(renderedPolicies.hiringOrder());
        int index = order.indexOf(type);
        int next = index + direction;
        if (next < 0 || next >= order.size()) return;
        order.set(index, order.get(next));
        order.set(next, type);
        submit(new ProfessionPolicySet(renderedPolicies.policies(), order));
    }

    private void submit(ProfessionPolicySet policies) { ClientProfessionPolicies.update(policies); rebuild(); }
    @Override public void close() { client.setScreen(parent); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFFFF);
        if (renderedPolicies == null) context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("text.villagecolony.professions.loading"), width / 2, 52, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private static Text maximumLabel(ProfessionPolicy policy) {
        return policy.maximumWorkers() == ProfessionPolicy.UNLIMITED
                ? Text.translatable("text.villagecolony.professions.maximum.unlimited")
                : Text.translatable("text.villagecolony.professions.maximum", policy.maximumWorkers());
    }
    private static Text enabledLabel(ProfessionPolicy policy) {
        return Text.translatable(policy.enabled()
                ? "text.villagecolony.professions.enabled"
                : "text.villagecolony.professions.disabled");
    }
    private static Text radiusLabel(ProfessionPolicy policy) {
        return policy.searchRadius() == ProfessionPolicy.AUTOMATIC_RADIUS
                ? Text.translatable("text.villagecolony.professions.radius.automatic")
                : Text.translatable("text.villagecolony.professions.radius", policy.searchRadius());
    }
    private static int nextRadius(int radius) {
        if (radius == ProfessionPolicy.AUTOMATIC_RADIUS) return ProfessionPolicy.MINIMUM_SEARCH_RADIUS;
        if (radius >= ProfessionPolicy.MAXIMUM_SEARCH_RADIUS) return ProfessionPolicy.AUTOMATIC_RADIUS;
        return radius + 16;
    }
}
