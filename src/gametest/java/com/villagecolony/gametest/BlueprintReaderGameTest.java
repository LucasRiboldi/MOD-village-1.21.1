package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.BlueprintAccess;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Set;

/**
 * A casa do jogo vira projeto da colônia — TASK-031.
 *
 * <p>Gametest e não teste de unidade porque é fronteira pura: só um
 * servidor de verdade tem o gerenciador de estruturas carregado com os
 * arquivos do Vanilla.
 *
 * <p>As afirmações são frouxas de propósito onde o Vanilla pode mudar.
 * Um teste que exigisse "esta casa tem 143 blocos" quebraria na próxima
 * versão do jogo sem que nada do mod estivesse errado. O que ele afirma
 * é o que precisa ser verdade para a obra existir: a casa foi lida, tem
 * tamanho de casa, é feita de coisas que a colônia produz, e não traz
 * andaime do gerador.
 */
public class BlueprintReaderGameTest implements FabricGameTest {

    /** Menos blocos que isto não é casa nenhuma. */
    private static final int PLAUSIBLE_HOUSE = 50;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void thePlainsHouseIsReadFromTheGame(TestContext context) {
        Blueprint house = read(context);

        context.assertTrue(
                house.blockCount() >= PLAUSIBLE_HOUSE,
                "a casa veio com " + house.blockCount() + " blocos — menos que uma casa");

        context.assertTrue(
                house.size().x() > 1 && house.size().y() > 1 && house.size().z() > 1,
                "a casa não tem três dimensões: " + house.size());

        context.complete();
    }

    /**
     * A casa é feita de tábua, que é o que o fabricante produz.
     *
     * <p>É a ligação que dá motivo à Fase 9: até aqui a tábua era feita
     * porque cabia no baú.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void theHouseIsMadeOfWhatTheColonyProduces(TestContext context) {
        Map<ResourceId, Integer> materials = read(context).materials();

        ResourceId planks = MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS);

        context.assertTrue(
                materials.containsKey(planks),
                "a casa de planície não pede tábua de carvalho — pede " + materials.keySet());

        context.assertTrue(
                materials.get(planks) > 0,
                "a conta de tábua veio zerada");

        context.complete();
    }

    /**
     * Nada de ar nem de bloco de encaixe do gerador.
     *
     * <p>A casa Vanilla é peça de jigsaw. Colocar o que ela traz de
     * andaime deixaria blocos de comando na vila do jogador — e o ar
     * ocuparia o projeto inteiro dizendo "aqui não vai nada".
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void theScaffoldingOfTheGeneratorIsLeftOut(TestContext context) {
        for (BlueprintBlock block : read(context).blocks()) {
            Optional<Block> real = MinecraftTypeAdapter.toBlock(block.block());

            context.assertTrue(
                    real.isPresent(),
                    "o projeto pede um bloco que o jogo não conhece: " + block.block());

            context.assertTrue(
                    real.get() != Blocks.AIR
                            && real.get() != Blocks.STRUCTURE_BLOCK
                            && real.get() != Blocks.JIGSAW,
                    "andaime do gerador entrou no projeto: " + block.block()
                            + " em " + block.offset());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void structureVoidDoesNotBecomeAHouseMaterial(TestContext context) {
        Blueprint mediumHouse = StructureBlueprintReader.read(
                        context.getWorld(),
                        ResourceId.vanilla("village/plains/houses/plains_medium_house_1"))
                .orElseThrow(() -> new AssertionError("a casa media de planicie nao foi encontrada"));
        ResourceId structureVoid = MinecraftTypeAdapter.toResourceId(Blocks.STRUCTURE_VOID);

        context.assertFalse(
                mediumHouse.materials().containsKey(structureVoid),
                "o andaime structure_void entrou na conta de materiais da casa");

        context.complete();
    }

    /**
     * O marcador do gerador vira o bloco que ele promete — 2026-08-29.
     *
     * <p><b>O buraco no meio do chão, visto em jogo.</b> A frase do
     * autor foi <i>"falta um bloco central no chão"</i>, e ela é exata:
     * a casa de planície tem um piso de nove tábuas, e a <b>do meio</b>
     * é um bloco de encaixe no arquivo do Vanilla.
     *
     * <pre>
     * camada y=0        o piso, visto de cima
     *
     *   cobblestone  cobblestone  cobblestone
     *   cobblestone  &lt;JIGSAW&gt;     cobblestone      &lt;- o buraco
     *   cobblestone  cobblestone  cobblestone
     * </pre>
     *
     * <p>O leitor tratava encaixe como <b>andaime do gerador</b> e o
     * descartava junto com o ar. Mas encaixe não é andaime: ele carrega
     * no próprio arquivo o bloco em que deve se transformar — o
     * {@code final_state} —, e é isso que o Vanilla põe ali quando gera
     * a vila. Do encaixe do meio do piso sai {@code oak_planks}; do que
     * fica na porta, o degrau de entrada.
     *
     * <p>É a ADR-001 de novo: a resposta está no arquivo do jogo, e o
     * mod só precisava lê-la em vez de inventar que não havia nada ali.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void theGeneratorsMarkerBecomesTheBlockItPromises(TestContext context) {
        Blueprint house = read(context);
        Map<ColonyPos, ResourceId> planned = new HashMap<>();

        for (BlueprintBlock block : house.blocks()) {
            planned.put(block.offset(), block.block());
        }

        ColonyPos middleOfTheFloor = new ColonyPos(3, 0, 3);

        context.assertTrue(
                planned.containsKey(middleOfTheFloor),
                "o piso da casa tem um buraco no meio, em " + middleOfTheFloor);

        context.assertTrue(
                planned.get(middleOfTheFloor).equals(ResourceId.vanilla("oak_planks")),
                "o meio do piso saiu como " + planned.get(middleOfTheFloor)
                        + ", e o arquivo promete oak_planks");

        ColonyPos doorstep = new ColonyPos(0, 0, 3);

        context.assertTrue(
                planned.containsKey(doorstep),
                "a casa ficou sem o degrau da entrada, em " + doorstep);

        context.assertTrue(
                planned.get(doorstep).equals(ResourceId.vanilla("oak_stairs")),
                "o degrau da entrada saiu como " + planned.get(doorstep));

        BlueprintBlock step = house.blocks().stream()
                .filter(block -> block.offset().equals(doorstep))
                .findFirst()
                .orElseThrow();

        context.assertTrue(
                step.facing().isPresent(),
                "o degrau da entrada perdeu a orientacao escrita pelo gerador");

        context.complete();
    }

    /**
     * A mobília entra depois da casa pronta — a Regra 32, 2026-08-29.
     *
     * <p><b>Regra do autor, dita depois de ver a casa em jogo:</b>
     * <i>"criar uma regra para adicionar os móveis e cama depois da casa
     * pronta"</i>. E a sessão mostrou as duas razões, as duas no log:
     *
     * <ul>
     *   <li><b>três tochas de parede riscadas</b> com
     *       {@code nothing holds it} — elas vinham antes da parede que as
     *       segura, e um bloco que não tem em que se apoiar é riscado
     *       para sempre;
     *   <li><b>a cama pela metade</b>: a cabeceira foi para dentro de um
     *       pedregulho que ainda nem estava lá quando a decisão foi
     *       tomada.
     * </ul>
     *
     * <p>Os dois são o mesmo defeito de ordem. Mobília precisa da casa
     * inteira em volta para saber onde cabe, e a ordem de baixo para
     * cima não dá isso: ela garante o que está <b>embaixo</b>, e mobília
     * depende do que está <b>ao lado</b>.
     *
     * <p>A afirmação é de ordem e não de contagem, de propósito: quantas
     * peças de mobília a casa tem é coisa que o Vanilla muda, e que
     * nenhuma venha antes de estrutura é o que precisa continuar
     * verdadeiro em casa nenhuma.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void theFurnitureComesAfterTheHouseIsUp(TestContext context) {
        boolean furnitureStarted = false;

        for (BlueprintBlock block : read(context).blocks()) {
            if (block.furniture()) {
                furnitureStarted = true;

                continue;
            }

            context.assertFalse(
                    furnitureStarted,
                    "a casa ainda estava subindo depois da mobília: " + block.block()
                            + " em " + block.offset() + " vem depois de um móvel");
        }

        context.assertTrue(
                furnitureStarted,
                "a casa de planície não tem móvel nenhum, e este teste não mede nada");

        context.complete();
    }

    /** Estrutura que não existe devolve vazio, e não derruba o tick. */
    /**
     * A porta da casa é uma entrada só — o E8 do §17, do lado do leitor.
     *
     * <p>No arquivo do jogo uma porta são <b>duas</b> entradas com o mesmo
     * nome, empilhadas, distinguidas só pela propriedade {@code half}. O
     * projeto guardava as duas, e a obra punha duas metades de baixo uma
     * sobre a outra — além de cobrar duas portas do baú por uma porta.
     *
     * <p>A afirmação é geométrica de propósito, e não uma contagem: "esta
     * casa tem exatamente uma porta" quebraria na próxima variante que o
     * Vanilla mudasse. O que não pode acontecer, em casa nenhuma, é a
     * mesma porta aparecer duas vezes na mesma coluna, em alturas
     * seguidas.
     *
     * <p>Rodado contra a regra desligada em 2026-08-15: sem o descarte em
     * {@code isSecondHalf} este teste falha na casa de planície.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void aDoorIsOneBlockInTheProjectAndNotTwo(TestContext context) {
        Set<String> doorColumns = new HashSet<>();
        int doors = 0;

        for (BlueprintBlock block : read(context).blocks()) {
            Optional<Block> game = MinecraftTypeAdapter.toBlock(block.block());

            if (game.isEmpty() || !(game.get() instanceof DoorBlock)) {
                continue;
            }

            doors++;

            String column = block.offset().x() + "," + block.offset().z();

            context.assertTrue(
                    doorColumns.add(column),
                    "a coluna " + column + " tem duas entradas de porta — "
                            + "a metade de cima não foi descartada na leitura");
        }

        // Uma casa sem porta nenhuma passaria no laço acima sem afirmar
        // nada, e o teste viraria decoração. Se um dia a variante do MVP
        // não tiver porta, é esta linha que avisa — e aí o teste muda de
        // casa, não de afirmação.
        context.assertTrue(doors > 0, "a casa de planície veio sem porta alguma");

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void anUnknownStructureIsEmptyAndNotAnError(TestContext context) {
        Optional<Blueprint> nothing = StructureBlueprintReader.read(
                context.getWorld(), ResourceId.vanilla("village/plains/houses/no_such_house"));

        context.assertTrue(nothing.isEmpty(), "uma estrutura inexistente devolveu projeto");

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void doorsKeepBothCandidateDirectionsWithoutCertifyingInternalDoors(TestContext context) {
        NbtCompound nbt = fixture(Blocks.OAK_DOOR.getDefaultState());
        NbtList entries = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
        entries.add(cell(1, 1, 0, 0));
        entries.add(cell(2, 1, 0, 0));
        entries.add(cell(1, 1, 3, 0));
        Blueprint blueprint = fixtureBlueprint(context, "doors", nbt);
        List<BlueprintAccess> doors = blueprint.accesses().stream()
                .filter(access -> access.kind() == BlueprintAccess.Kind.DOOR).toList();
        context.assertTrue(doors.size() == 4, "duas passagens precisam dos dois sentidos candidatos");
        context.assertTrue(doors.stream().filter(access -> access.opening().size() == 2).count() == 2,
                "porta dupla deve formar uma passagem em cada sentido");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void entranceJigsawSurvivesFilteringButVerticalConnectorDoesNot(TestContext context) {
        NbtCompound nbt = fixture(Blocks.JIGSAW.getDefaultState());
        NbtList palette = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
        NbtCompound verticalState = NbtHelper.fromBlockState(Blocks.JIGSAW.getDefaultState());
        verticalState.getCompound("Properties").putString("orientation", "up_north");
        palette.add(verticalState);
        NbtList entries = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
        NbtCompound horizontal = cell(1, 1, 0, 0);
        NbtCompound data = new NbtCompound();
        data.putString("name", "minecraft:building_entrance");
        data.putString("final_state", "minecraft:air");
        horizontal.put("nbt", data);
        entries.add(horizontal);
        NbtCompound vertical = cell(1, 1, 3, 2);
        vertical.put("nbt", data.copy());
        entries.add(vertical);
        Blueprint blueprint = fixtureBlueprint(context, "jigsaw", nbt);
        context.assertTrue(blueprint.accesses().size() == 1, "somente o conector horizontal é candidato");
        context.assertTrue(blueprint.accesses().getFirst().opening().equals(List.of(new ColonyPos(1, 2, 0))),
                "o filtro de blocos perdeu o conector de entrada");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void aDeclaredStorageOpeningIsCheckedAgainstTheLoadedTemplate(TestContext context) {
        ResourceId id = ResourceId.parse("villagecolony:colony/storage_majest");
        StructureTemplate template = context.getWorld().getStructureTemplateManager()
                .getTemplateOrBlank(MinecraftTypeAdapter.toIdentifier(id));
        NbtCompound original = template.writeNbt(new NbtCompound());
        try {
            Blueprint storage = StructureBlueprintReader.read(context.getWorld(), id).orElseThrow();
            context.assertTrue(storage.accesses().stream().anyMatch(access ->
                    access.kind() == BlueprintAccess.Kind.OPENING), "depósito perdeu o vão inspecionado");
            NbtCompound obstructed = original.copy();
            NbtList palette = obstructed.getList("palette", NbtElement.COMPOUND_TYPE);
            int stone = palette.size();
            palette.add(NbtHelper.fromBlockState(Blocks.STONE.getDefaultState()));
            NbtList entries = obstructed.getList("blocks", NbtElement.COMPOUND_TYPE);
            entries.removeIf(entry -> isAt((NbtCompound) entry, 9, 2, 3));
            entries.add(cell(9, 2, 3, stone));
            template.readNbt(Registries.BLOCK.getReadOnlyWrapper(), obstructed);
            Blueprint blocked = StructureBlueprintReader.read(context.getWorld(), id).orElseThrow();
            context.assertTrue(blocked.accesses().stream().noneMatch(access ->
                    access.kind() == BlueprintAccess.Kind.OPENING), "vão obstruído foi aceito por id do modelo");
        } finally {
            template.readNbt(Registries.BLOCK.getReadOnlyWrapper(), original);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void upperDoorIsNotAnotherAccessAndFenceGateKeepsItsOwnKind(TestContext context) {
        NbtCompound nbt = fixture(Blocks.OAK_DOOR.getDefaultState());
        NbtList palette = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
        NbtCompound upper = NbtHelper.fromBlockState(Blocks.OAK_DOOR.getDefaultState());
        upper.getCompound("Properties").putString("half", "upper");
        palette.add(upper);
        palette.add(NbtHelper.fromBlockState(Blocks.OAK_FENCE_GATE.getDefaultState()));
        NbtList entries = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
        entries.add(cell(1, 1, 0, 0));
        entries.add(cell(1, 2, 0, 2));
        entries.add(cell(4, 1, 3, 3));
        Blueprint blueprint = fixtureBlueprint(context, "gate_and_upper", nbt);
        context.assertTrue(blueprint.accesses().size() == 4, "duas peças de passagem, dois sentidos cada");
        context.assertTrue(blueprint.accesses().stream().filter(access ->
                access.kind() == BlueprintAccess.Kind.GATE).count() == 2, "portão foi confundido com porta");
        context.assertTrue(blueprint.accesses().stream().allMatch(access -> access.opening().getFirst().y() == 1),
                "metade superior virou acesso independente");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void everyStyleKeepsItsLoadedHouseAndDeclaredStorageCandidates(TestContext context) {
        for (String style : List.of("plains", "desert", "savanna", "taiga", "snowy")) {
            ResourceId house = ResourceId.vanilla("village/" + style + "/houses/" + style + "_small_house_1");
            context.assertTrue(!StructureBlueprintReader.read(context.getWorld(), house).orElseThrow().accesses().isEmpty(),
                    "casa sem candidatos: " + style);
            ResourceId storage = ResourceId.parse("villagecolony:colony/" + style + "/storage_majest");
            context.assertTrue(StructureBlueprintReader.read(context.getWorld(), storage).orElseThrow().accesses()
                    .stream().anyMatch(access -> access.kind() == BlueprintAccess.Kind.OPENING),
                    "depósito sem vão conferido: " + style);
        }
        context.complete();
    }

    private static NbtCompound fixture(net.minecraft.block.BlockState state) {
        NbtCompound nbt = new NbtCompound();
        NbtList size = new NbtList();
        for (int value : new int[]{5, 4, 5}) size.add(NbtInt.of(value));
        nbt.put("size", size);
        NbtList palette = new NbtList();
        palette.add(NbtHelper.fromBlockState(state));
        palette.add(NbtHelper.fromBlockState(Blocks.STONE.getDefaultState()));
        nbt.put("palette", palette);
        NbtList blocks = new NbtList();
        blocks.add(cell(0, 0, 0, 1));
        nbt.put("blocks", blocks);
        nbt.put("entities", new NbtList());
        return nbt;
    }

    private static NbtCompound cell(int x, int y, int z, int state) {
        NbtCompound block = new NbtCompound();
        NbtList pos = new NbtList();
        for (int value : new int[]{x, y, z}) pos.add(NbtInt.of(value));
        block.put("pos", pos);
        block.putInt("state", state);
        return block;
    }

    private static Blueprint fixtureBlueprint(TestContext context, String name, NbtCompound nbt) {
        Identifier id = Identifier.of("villagecolony", "test/access_" + name);
        context.getWorld().getStructureTemplateManager().getTemplateOrBlank(id)
                .readNbt(Registries.BLOCK.getReadOnlyWrapper(), nbt);
        return StructureBlueprintReader.read(context.getWorld(), ResourceId.parse(id.toString())).orElseThrow();
    }

    private static boolean isAt(NbtCompound entry, int x, int y, int z) {
        NbtList pos = entry.getList("pos", NbtElement.INT_TYPE);
        return pos.size() == 3 && pos.getInt(0) == x && pos.getInt(1) == y && pos.getInt(2) == z;
    }

    private static Blueprint read(TestContext context) {
        return StructureBlueprintReader
                .read(context.getWorld(), StructureBlueprintReader.PLAINS_SMALL_HOUSE)
                .orElseThrow(() -> new AssertionError(
                        "o jogo não devolveu a casa "
                                + StructureBlueprintReader.PLAINS_SMALL_HOUSE));
    }
}
