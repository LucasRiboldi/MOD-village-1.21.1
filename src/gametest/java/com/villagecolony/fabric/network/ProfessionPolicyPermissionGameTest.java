package com.villagecolony.fabric.network;

import com.villagecolony.core.worker.model.ProfessionPolicy;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.data.save.ProfessionPolicySavedData;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.EnumMap;

/**
 * Mudar as regras de profissão exige operador nível 2 — AUD-11.
 *
 * <p>É a única escrita que o cliente manda ao servidor (Mod Menu). Sem
 * permissão, o pedido é ignorado e o mundo continua com as regras que tinha;
 * com permissão, elas são trocadas.
 */
public class ProfessionPolicyPermissionGameTest implements FabricGameTest {

    private static ProfessionPolicySet minerDisabled() {
        EnumMap<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(ProfessionType.class);
        ProfessionPolicySet.defaults().policies().forEach(policies::put);
        policies.put(ProfessionType.MINER, new ProfessionPolicy(false, 0, ProfessionPolicy.AUTOMATIC_RADIUS));
        return new ProfessionPolicySet(policies, ProfessionPolicySet.defaults().hiringOrder());
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "profession_policy_permission")
    public void aPlayerWithoutPermissionCannotChangeTheRules(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        ProfessionPolicySavedData saved = ProfessionPolicySavedData.get(server);
        ProfessionPolicySet before = saved.policies();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();

        try {
            context.assertFalse(player.hasPermissionLevel(2), "o jogador de teste já era operador");

            ProfessionPolicyNetworking.applyUpdate(player, new ProfessionPolicyUpdatePayload(minerDisabled()));

            context.assertTrue(saved.policies().policyOf(ProfessionType.MINER).enabled(),
                    "um jogador sem permissão desligou o mineiro do mundo");
        } finally {
            saved.replace(before);
            server.getPlayerManager().remove(player);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "profession_policy_permission")
    public void anOperatorChangesTheRules(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        ProfessionPolicySavedData saved = ProfessionPolicySavedData.get(server);
        ProfessionPolicySet before = saved.policies();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        // O TestServer registra operador com o nível dele (zero); o nível da
        // permissão vem explícito, como num ops.json de servidor de verdade.
        server.getPlayerManager().getOpList().add(new OperatorEntry(player.getGameProfile(), 4, false));

        try {
            context.assertTrue(player.hasPermissionLevel(2), "o operador de teste não tem nível 2");

            ProfessionPolicyNetworking.applyUpdate(player, new ProfessionPolicyUpdatePayload(minerDisabled()));

            context.assertFalse(saved.policies().policyOf(ProfessionType.MINER).enabled(),
                    "o operador não conseguiu mudar a regra do mineiro");
        } finally {
            saved.replace(before);
            server.getPlayerManager().removeFromOperators(player.getGameProfile());
            server.getPlayerManager().remove(player);
        }

        context.complete();
    }
}
