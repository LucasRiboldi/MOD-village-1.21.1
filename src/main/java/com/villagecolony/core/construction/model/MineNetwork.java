package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Um nível da mina em rede — ADR-042 (D2, D3, D5, D7, D9).
 *
 * <p>Os nós ficam numa grade de {@link #SPACING} blocos em volta do salão do nível
 * ({@link #hub()}), no plano do piso dele. O salão é o nó (0, 0), pronto desde o
 * começo. Cada nó é ligado ao pai por um corredor de {@link #WIDTH} de largura e
 * {@link #HEIGHT} de altura; quando um nó fica pronto, os vizinhos dele dentro do raio
 * abrem. Cada mineiro segura um nó por vez, e o nível acaba quando não há nó aberto nem
 * em obra — a regra da MineColonies ({@code MinerLevel.closeNextNode}), com a grade
 * inteira no raio em vez dos esquemas sorteados.
 *
 * <p>Puro: não conhece o mundo. Quem pergunta se uma célula está cavada, se há fluido
 * ou se o piso está seguro é o lado Fabric.
 */
public final class MineNetwork {

    /**
     * A distância entre os centros de dois nós vizinhos: túneis a cada 4 blocos, com 3 de
     * rocha entre eles — a mineração em ramal do jogador, que deixa todo bloco da rocha a
     * no máximo 2 de um túnel (playtest 0.3.35: a grade de 7 com corredor 3 × 3 cavava
     * pedra e não minério, e parecia labirinto).
     */
    public static final int SPACING = 4;

    /** Até quantos nós do salão, em cada eixo, o nível se estende: 49 × 49 blocos. */
    public static final int RADIUS = 6;

    /** Largura do túnel: um bloco, por onde o aldeão passa. */
    public static final int WIDTH = 1;

    /** Altura do túnel: dois blocos, a altura do aldeão. */
    public static final int HEIGHT = 2;

    /** Desistências num nó antes de ele ser vedado como inalcançável. */
    public static final int FAILURES_BEFORE_SEALING = 3;

    /** O estado de um nó. */
    public enum State {
        /** Pode ser pego: o pai está pronto. */
        OPEN(0),
        /** Um mineiro está abrindo o corredor dele. */
        DIGGING(1),
        /** Corredor aberto, piso garantido e aberturas vedadas. */
        DONE(2),
        /** Não se abre: fluido, ou ninguém alcançou. */
        SEALED(3);

        /** O número gravado no save; não muda com a ordem das constantes. */
        private final int code;

        State(int code) {
            this.code = code;
        }

        static State ofCode(int code) {
            for (State state : values()) {
                if (state.code == code) {
                    return state;
                }
            }

            return OPEN;
        }
    }

    /** Um nó: a posição na grade, a do pai e o estado. */
    public record Node(int gx, int gz, int parentGx, int parentGz) {

        /** O lado para onde o corredor anda, do pai para o nó: (dx, dz) unitário. */
        public int dx() {
            return Integer.signum(gx - parentGx);
        }

        public int dz() {
            return Integer.signum(gz - parentGz);
        }

        public boolean isHub() {
            return gx == 0 && gz == 0;
        }
    }

    static final class Entry {
        final Node node;
        State state;
        @Nullable UUID claimant;
        int failures;

        Entry(Node node, State state) {
            this.node = node;
            this.state = state;
        }
    }

    private final ColonyPos hub;

    final Map<Long, Entry> nodes = new LinkedHashMap<>();

    private boolean shaftSecured;

    private int radius = RADIUS;

    private MineNetwork(ColonyPos hub) {
        this.hub = Objects.requireNonNull(hub, "hub");
    }

    /** Um nível novo neste salão: o salão pronto e os quatro vizinhos abertos. */
    public static MineNetwork open(ColonyPos hub) {
        MineNetwork network = new MineNetwork(hub);
        Node root = new Node(0, 0, 0, 0);

        network.nodes.put(key(0, 0), new Entry(root, State.DONE));
        network.openChildrenOf(root);

        return network;
    }

    /** O salão do nível: o centro do nó (0, 0), na altura do piso. */
    public ColonyPos hub() {
        return hub;
    }

    /** O raio em nós; cresce no fundo da mina ({@link #widen()}). */
    public int radius() {
        return radius;
    }

    /** Se o poço e o salão deste nível já tiveram o piso garantido e as aberturas vedadas. */
    public boolean shaftSecured() {
        return shaftSecured;
    }

    public void shaftIsSecured() {
        shaftSecured = true;
    }

    /** O centro de um nó, na altura do piso. */
    public ColonyPos centerOf(int gx, int gz) {
        return new ColonyPos(hub.x() + gx * SPACING, hub.y(), hub.z() + gz * SPACING);
    }

    /** O estado de um nó, se ele existe. */
    public Optional<State> stateOf(int gx, int gz) {
        return Optional.ofNullable(nodes.get(key(gx, gz))).map(entry -> entry.state);
    }

    /** O nó que este mineiro segura. */
    public Optional<Node> claimedBy(UUID worker) {
        return nodes.values().stream()
                .filter(entry -> entry.state == State.DIGGING && worker.equals(entry.claimant))
                .map(entry -> entry.node)
                .findFirst();
    }

    /**
     * Pega para este mineiro o nó aberto mais perto dele, ou devolve o que ele já
     * segura. Vazio quando não há nó aberto.
     */
    public Optional<Node> claim(UUID worker, ColonyPos near) {
        Optional<Node> held = claimedBy(worker);

        if (held.isPresent()) {
            return held;
        }

        Entry best = null;
        long bestDistance = Long.MAX_VALUE;

        for (Entry entry : nodes.values()) {
            if (entry.state != State.OPEN) {
                continue;
            }

            ColonyPos center = centerOf(entry.node.gx(), entry.node.gz());
            long dx = (long) center.x() - near.x();
            long dy = (long) center.y() - near.y();
            long dz = (long) center.z() - near.z();
            long distance = dx * dx + dy * dy + dz * dz;

            if (distance < bestDistance) {
                best = entry;
                bestDistance = distance;
            }
        }

        if (best == null) {
            return Optional.empty();
        }

        best.state = State.DIGGING;
        best.claimant = worker;

        return Optional.of(best.node);
    }

    /** O mineiro largou o nó: ele volta a ficar aberto. */
    public void release(UUID worker) {
        for (Entry entry : nodes.values()) {
            if (entry.state == State.DIGGING && worker.equals(entry.claimant)) {
                entry.state = State.OPEN;
                entry.claimant = null;
            }
        }
    }

    /** O corredor do nó está aberto e seguro: pronto, e os vizinhos abrem. */
    public void done(Node node) {
        Entry entry = nodes.get(key(node.gx(), node.gz()));

        if (entry == null) {
            return;
        }

        entry.state = State.DONE;
        entry.claimant = null;
        openChildrenOf(node);
    }

    /** O nó não se abre (fluido, inalcançável): fica vedado e não abre vizinhos. */
    public void seal(Node node) {
        Entry entry = nodes.get(key(node.gx(), node.gz()));

        if (entry != null) {
            entry.state = State.SEALED;
            entry.claimant = null;
        }
    }

    /**
     * Uma desistência neste nó.
     *
     * @return se ele chegou a {@link #FAILURES_BEFORE_SEALING} e foi vedado agora
     */
    public boolean failed(Node node) {
        Entry entry = nodes.get(key(node.gx(), node.gz()));

        if (entry == null || ++entry.failures < FAILURES_BEFORE_SEALING) {
            return false;
        }

        seal(node);

        return true;
    }

    /** Um mineiro trabalhou no nó: as desistências recomeçam. */
    public void worked(Node node) {
        Entry entry = nodes.get(key(node.gx(), node.gz()));

        if (entry != null) {
            entry.failures = 0;
        }
    }

    /** Sem nó aberto nem em obra: o nível acabou. */
    public boolean exhausted() {
        return nodes.values().stream().noneMatch(entry -> entry.state == State.OPEN || entry.state == State.DIGGING);
    }

    /**
     * O fundo da mina: o nível ganha mais um anel de nós em volta (caminhos laterais),
     * e os nós prontos da borda abrem os vizinhos novos — D8.
     */
    public void widen() {
        radius++;

        for (Entry entry : new ArrayList<>(nodes.values())) {
            if (entry.state == State.DONE) {
                openChildrenOf(entry.node);
            }
        }
    }

    /**
     * As células do corredor do nó, do pai até o centro dele: {@link #WIDTH} de largura,
     * {@link #HEIGHT} de altura, na ordem de cavar — perto do pai primeiro, de baixo
     * para cima (o piso de andar abre antes do teto).
     */
    public List<ColonyPos> corridorOf(Node node) {
        if (node.isHub()) {
            return List.of();
        }

        ColonyPos from = centerOf(node.parentGx(), node.parentGz());
        int dx = node.dx();
        int dz = node.dz();
        int half = WIDTH / 2;
        List<ColonyPos> cells = new ArrayList<>(SPACING * WIDTH * HEIGHT);

        for (int step = 1; step <= SPACING; step++) {
            for (int height = 0; height < HEIGHT; height++) {
                for (int side = -half; side <= half; side++) {
                    cells.add(new ColonyPos(
                            from.x() + dx * step + dz * side,
                            from.y() + height,
                            from.z() + dz * step + dx * side));
                }
            }
        }

        return cells;
    }

    /** As células de todos os corredores em obra ou prontos — o que é da mina neste nível. */
    public Set<ColonyPos> cells() {
        Set<ColonyPos> cells = new HashSet<>();

        for (Entry entry : nodes.values()) {
            if (entry.state == State.DONE || entry.state == State.DIGGING) {
                cells.addAll(corridorOf(entry.node));
            }
        }

        return cells;
    }

    /** O nó cujo corredor tem esta célula, se algum. */
    public Optional<Node> nodeHolding(ColonyPos cell) {
        for (Entry entry : nodes.values()) {
            if ((entry.state == State.DIGGING || entry.state == State.OPEN)
                    && corridorOf(entry.node).contains(cell)) {
                return Optional.of(entry.node);
            }
        }

        return Optional.empty();
    }

    /** O nó da grade mais perto desta posição (sem olhar o estado). */
    public int[] gridOf(ColonyPos at) {
        return new int[] {
            Math.round((at.x() - hub.x()) / (float) SPACING),
            Math.round((at.z() - hub.z()) / (float) SPACING)
        };
    }

    /**
     * A rota pelos nós prontos, do nó pronto mais perto de {@code from} até o pai de
     * {@code to} — D5. Os centros dos nós, na ordem; vazia sem rota. Ver
     * {@link MineNetworkRoute}.
     */
    public List<ColonyPos> routeTo(ColonyPos from, Node to) {
        return MineNetworkRoute.route(this, from, to);
    }

    /** Os nós, para gravar: gx, gz, pai gx, pai gz, estado (ordinal) — D9. */
    public int[] toArray() {
        int[] out = new int[3 + nodes.size() * 5];
        out[0] = shaftSecured ? 1 : 0;
        out[1] = radius;
        out[2] = nodes.size();
        int i = 3;

        for (Entry entry : nodes.values()) {
            out[i++] = entry.node.gx();
            out[i++] = entry.node.gz();
            out[i++] = entry.node.parentGx();
            out[i++] = entry.node.parentGz();
            // Em obra volta aberto: quem segurava não volta com o save.
            out[i++] = (entry.state == State.DIGGING ? State.OPEN : entry.state).code;
        }

        return out;
    }

    /** O nível gravado por {@link #toArray()}; um nível novo se o vetor não servir. */
    public static MineNetwork restore(ColonyPos hub, int[] saved) {
        if (saved.length < 3 || saved.length != 3 + saved[2] * 5) {
            return open(hub);
        }

        MineNetwork network = new MineNetwork(hub);
        network.shaftSecured = saved[0] == 1;
        network.radius = Math.max(RADIUS, saved[1]);

        for (int i = 3; i < saved.length; i += 5) {
            State state = State.ofCode(saved[i + 4]);
            Node node = new Node(saved[i], saved[i + 1], saved[i + 2], saved[i + 3]);

            network.nodes.put(key(node.gx(), node.gz()), new Entry(node, state));
        }

        if (!network.nodes.containsKey(key(0, 0))) {
            return open(hub);
        }

        return network;
    }

    /** Quantos nós em cada estado — para o relatório. */
    public Map<State, Integer> census() {
        Map<State, Integer> census = new LinkedHashMap<>();

        for (State state : State.values()) {
            census.put(state, 0);
        }

        for (Entry entry : nodes.values()) {
            census.merge(entry.state, 1, Integer::sum);
        }

        return census;
    }

    private void openChildrenOf(Node node) {
        for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int gx = node.gx() + d[0];
            int gz = node.gz() + d[1];

            if (Math.abs(gx) > radius || Math.abs(gz) > radius || nodes.containsKey(key(gx, gz))) {
                continue;
            }

            nodes.put(key(gx, gz), new Entry(new Node(gx, gz, node.gx(), node.gz()), State.OPEN));
        }
    }

    static long key(int gx, int gz) {
        return ((long) gx << 32) | (gz & 0xFFFFFFFFL);
    }

    static int gxOf(long key) {
        return (int) (key >> 32);
    }

    static int gzOf(long key) {
        return (int) key;
    }
}
