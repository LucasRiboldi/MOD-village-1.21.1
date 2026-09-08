import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";

const GITHUB =
  "https://github.com/LucasRiboldi/MOD-village-1.21.1";

const DOWNLOAD =
  "https://github.com/LucasRiboldi/MOD-village-1.21.1/releases";

const professions = [
  {
    icon: "🪓",
    name: "Lenhador",
    color: "green",
    text: "Corta árvores, coleta madeira e replanta mudas automaticamente.",
  },
  {
    icon: "⛏️",
    name: "Mineiro",
    color: "gray",
    text: "Escava minas reais, encontra minérios, ilumina túneis e desvia perigos.",
  },
  {
    icon: "🌾",
    name: "Fazendeiro",
    color: "yellow",
    text: "Colhe plantações maduras, replanta e mantém a produção de alimentos.",
  },
  {
    icon: "🐑",
    name: "Pastor",
    color: "white",
    text: "Tosquia ovelhas e mantém a produção de lã da colônia.",
  },
  {
    icon: "🔥",
    name: "Fundidor",
    color: "orange",
    text: "Transforma matérias-primas utilizando as próprias receitas do Minecraft.",
  },
  {
    icon: "🪚",
    name: "Fabricante",
    color: "brown",
    text: "Transforma madeira em tábuas, tochas, vidraças e outros materiais.",
  },
  {
    icon: "🏠",
    name: "Construtor",
    color: "blue",
    text: "Usa os recursos armazenados para levantar novas estruturas da vila.",
  },
];

const features = [
  {
    number: "01",
    title: "Uma vila que trabalha",
    text: "Os aldeões deixam de ser apenas habitantes. Cada profissão possui uma função dentro da economia da colônia.",
  },
  {
    number: "02",
    title: "Recursos de verdade",
    text: "Nada de inventário virtual. Madeira, pedra, comida e outros recursos existem fisicamente nos baús.",
  },
  {
    number: "03",
    title: "Construção automática",
    text: "Quando existem recursos suficientes, o construtor pode transformar estoque em novas estruturas.",
  },
  {
    number: "04",
    title: "Tudo Vanilla",
    text: "O sistema aproveita aldeões, baús, receitas e estruturas do próprio Minecraft.",
  },
];

function App() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 40);
    window.addEventListener("scroll", handleScroll);

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  useEffect(() => {
    const elements = document.querySelectorAll(".reveal");

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("visible");
          }
        });
      },
      { threshold: 0.12 }
    );

    elements.forEach((element) => observer.observe(element));

    return () => observer.disconnect();
  }, []);

  const closeMenu = () => setMenuOpen(false);

  return (
    <div className="site">

      {/* NAVBAR */}
      <header className={`navbar ${scrolled ? "navbar-scrolled" : ""}`}>
        <a href="#top" className="brand" onClick={closeMenu}>
          <span className="brand-mark">VC</span>
          <span>
            <strong>VILLAGE</strong>
            <small>COLONY</small>
          </span>
        </a>

        <button
          className="menu-toggle"
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="Abrir menu"
        >
          <span />
          <span />
          <span />
        </button>

        <nav className={menuOpen ? "nav-open" : ""}>
          <a href="#mod" onClick={closeMenu}>O MOD</a>
          <a href="#professions" onClick={closeMenu}>PROFISSÕES</a>
          <a href="#world" onClick={closeMenu}>MUNDO</a>
          <a href="#installation" onClick={closeMenu}>INSTALAÇÃO</a>

          <a
            href={GITHUB}
            target="_blank"
            rel="noreferrer"
            className="nav-github"
          >
            GITHUB ↗
          </a>
        </nav>
      </header>

      {/* HERO */}
      <main id="top">

        <section className="hero">
          <div className="hero-background">
            <div className="pixel-cloud cloud-one" />
            <div className="pixel-cloud cloud-two" />
            <div className="mountain mountain-back" />
            <div className="mountain mountain-front" />
          </div>

          <div className="hero-content">
            <div className="status-badge">
              <span className="status-dot" />
              ALPHA · MINECRAFT 1.21.1
            </div>

            <p className="eyebrow">FABRIC MOD</p>

            <h1>
              SUA VILA
              <br />
              <span>NÃO ESPERA</span>
            </h1>

            <p className="hero-description">
              Transforme vilas do Minecraft Vanilla em colônias vivas
              que trabalham, produzem e constroem por conta própria.
            </p>

            <div className="hero-actions">
              <a href={DOWNLOAD} className="button button-primary">
                <span>⬇</span>
                BAIXAR MOD
              </a>

              <a
                href="#mod"
                className="button button-secondary"
              >
                EXPLORAR
                <span>↓</span>
              </a>
            </div>

            <div className="hero-meta">
              <span>MINECRAFT 1.21.1</span>
              <i />
              <span>FABRIC</span>
              <i />
              <span>JAVA EDITION</span>
            </div>
          </div>

          <div className="hero-scroll">
            <span>SCROLL PARA EXPLORAR</span>
            <div className="scroll-line" />
          </div>
        </section>

        {/* INTRO */}
        <section id="mod" className="intro section">
          <div className="container">

            <div className="section-label reveal">
              <span>01</span>
              <span>O CONCEITO</span>
            </div>

            <div className="intro-grid">

              <div className="intro-title reveal">
                <p className="eyebrow">UMA NOVA VIDA</p>

                <h2>
                  Você vai embora.
                  <br />
                  <em>A vila continua.</em>
                </h2>
              </div>

              <div className="intro-copy reveal">
                <p>
                  Você encontra uma vila de Minecraft. Contrata seus
                  aldeões. Depois vai explorar o mundo.
                </p>

                <p>
                  Quando retorna, a história continuou sem você.
                  Árvores foram derrubadas. Recursos foram armazenados.
                  Materiais foram produzidos.
                </p>

                <p>
                  E, quando existe recurso suficiente, uma nova construção
                  começa a aparecer.
                </p>

                <a href="#professions" className="text-link">
                  CONHEÇA OS TRABALHADORES <span>→</span>
                </a>
              </div>

            </div>

            {/* LARGE MEDIA AREA */}
            <div className="media-frame media-large reveal">
              <div className="media-placeholder">
                <span className="media-icon">▣</span>
                <strong>SCREENSHOT DA COLÔNIA</strong>
                <small>ADICIONE SUA IMAGEM AQUI</small>
              </div>

              <div className="media-caption">
                <span>VILLAGE COLONY</span>
                <span>01 / 06</span>
              </div>
            </div>

          </div>
        </section>

        {/* FEATURES */}
        <section className="features section">
          <div className="container">

            <div className="section-heading reveal">
              <div>
                <p className="eyebrow">O QUE MUDA</p>
                <h2>
                  O mundo continua
                  <br />
                  <em>mesmo sem você.</em>
                </h2>
              </div>

              <p>
                Village Colony transforma a rotina de uma vila em um
                pequeno sistema econômico vivo.
              </p>
            </div>

            <div className="feature-list">
              {features.map((feature) => (
                <article className="feature reveal" key={feature.number}>
                  <span className="feature-number">{feature.number}</span>

                  <div>
                    <h3>{feature.title}</h3>
                    <p>{feature.text}</p>
                  </div>

                  <span className="feature-arrow">↗</span>
                </article>
              ))}
            </div>

          </div>
        </section>

        {/* PROFESSIONS */}
        <section id="professions" className="professions section">
          <div className="container">

            <div className="section-label reveal">
              <span>02</span>
              <span>OS TRABALHADORES</span>
            </div>

            <div className="section-heading professions-heading reveal">
              <div>
                <p className="eyebrow">SETE PROFISSÕES</p>
                <h2>
                  Cada aldeão.
                  <br />
                  <em>Uma função.</em>
                </h2>
              </div>

              <p>
                A colônia funciona através de trabalhadores especializados,
                cada um com sua própria tarefa e cadeia de produção.
              </p>
            </div>

            <div className="profession-grid">
              {professions.map((profession, index) => (
                <article
                  className={`profession-card ${profession.color} reveal`}
                  key={profession.name}
                  style={{ "--delay": `${index * 60}ms` }}
                >
                  <div className="profession-top">
                    <span className="profession-number">
                      0{index + 1}
                    </span>

                    <span className="profession-icon">
                      {profession.icon}
                    </span>
                  </div>

                  <div className="profession-content">
                    <h3>{profession.name}</h3>
                    <p>{profession.text}</p>
                  </div>

                  <span className="card-arrow">↗</span>
                </article>
              ))}
            </div>

          </div>
        </section>

        {/* CINEMATIC MINING */}
        <section id="world" className="cinematic">
          <div className="cinematic-overlay" />

          <div className="container cinematic-content">

            <div className="section-label light reveal">
              <span>03</span>
              <span>EXPLORAÇÃO</span>
            </div>

            <div className="cinematic-text reveal">
              <p className="eyebrow">O MINEIRO</p>

              <h2>
                Uma mina de
                <br />
                <em>verdade.</em>
              </h2>

              <p>
                O mineiro não apenas remove blocos.
                Ele abre uma mina real, desce em espiral,
                encontra minérios, ilumina o caminho e reage
                a água e lava.
              </p>

              <div className="stat-row">
                <div>
                  <strong>20</strong>
                  <span>BLOCOS</span>
                </div>

                <div>
                  <strong>3</strong>
                  <span>BLOCOS DE ALTURA</span>
                </div>

                <div>
                  <strong>∞</strong>
                  <span>EXPLORAÇÃO</span>
                </div>
              </div>
            </div>

            <div className="cinematic-image reveal">
              <div className="media-placeholder dark">
                <span className="media-icon">⛏</span>
                <strong>SCREENSHOT DA MINA</strong>
                <small>ADICIONE SUA IMAGEM AQUI</small>
              </div>
            </div>

          </div>
        </section>

        {/* RESOURCE CHAIN */}
        <section className="chain section">
          <div className="container">

            <div className="section-label reveal">
              <span>04</span>
              <span>A CADEIA</span>
            </div>

            <div className="chain-heading reveal">
              <p className="eyebrow">NADA É MÁGICO</p>

              <h2>
                Recursos entram.
                <br />
                <em>Construções saem.</em>
              </h2>
            </div>

            <div className="chain-flow">

              <div className="chain-step reveal">
                <span>01</span>
                <strong>COLETAR</strong>
                <small>Madeira · Pedra · Comida</small>
              </div>

              <div className="chain-line" />

              <div className="chain-step reveal">
                <span>02</span>
                <strong>PROCESSAR</strong>
                <small>Tábuas · Vidro · Lingotes</small>
              </div>

              <div className="chain-line" />

              <div className="chain-step reveal">
                <span>03</span>
                <strong>CONSTRUIR</strong>
                <small>Casas · Roças · Expansão</small>
              </div>

            </div>

            <div className="media-frame media-wide reveal">
              <div className="media-placeholder">
                <span className="media-icon">▣</span>
                <strong>SCREENSHOTS DA CADEIA DE PRODUÇÃO</strong>
                <small>ADICIONE 2 OU 3 IMAGENS DO MOD AQUI</small>
              </div>
            </div>

          </div>
        </section>

        {/* VANILLA */}
        <section className="vanilla section">
          <div className="container vanilla-grid">

            <div className="vanilla-copy reveal">
              <p className="eyebrow">VANILLA PRIMEIRO</p>

              <h2>
                O Minecraft
                <br />
                <em>continua sendo Minecraft.</em>
              </h2>

              <p>
                O mod não cria uma economia paralela ou um inventário
                abstrato. Ele utiliza os sistemas que já existem no jogo.
              </p>

              <div className="check-list">
                <span>✓</span> Aldeões Vanilla
                <span>✓</span> Baús Vanilla
                <span>✓</span> Receitas Vanilla
                <span>✓</span> Estruturas Vanilla
              </div>
            </div>

            <div className="media-frame media-square reveal">
              <div className="media-placeholder">
                <span className="media-icon">⬡</span>
                <strong>SCREENSHOT VANILLA</strong>
                <small>IMAGEM DA VILA</small>
              </div>
            </div>

          </div>
        </section>

        {/* INSTALL */}
        <section id="installation" className="installation section">
          <div className="container">

            <div className="section-label reveal">
              <span>05</span>
              <span>COMECE AGORA</span>
            </div>

            <div className="install-heading reveal">
              <p className="eyebrow">INSTALAÇÃO</p>

              <h2>
                Encontre uma vila.
                <br />
                <em>Deixe o resto com eles.</em>
              </h2>
            </div>

            <div className="install-grid">

              <div className="install-card reveal">
                <span>01</span>
                <h3>Fabric Loader</h3>
                <p>
                  Instale o Fabric Loader compatível com Minecraft 1.21.1.
                </p>
              </div>

              <div className="install-card reveal">
                <span>02</span>
                <h3>Fabric API</h3>
                <p>
                  Coloque a Fabric API na pasta <code>mods</code>.
                </p>
              </div>

              <div className="install-card reveal">
                <span>03</span>
                <h3>Village Colony</h3>
                <p>
                  Coloque o arquivo do mod junto dos outros mods.
                </p>
              </div>

              <div className="install-card reveal">
                <span>04</span>
                <h3>Encontre uma vila</h3>
                <p>
                  Entre no mundo, encontre uma vila e observe o sistema
                  começar a funcionar.
                </p>
              </div>

            </div>

            <div className="server-note reveal">
              <span>⚡</span>

              <div>
                <strong>FEITO PARA SERVIDORES</strong>
                <p>
                  Em um servidor dedicado com o mod instalado,
                  jogadores que entram não precisam instalar o mod no cliente.
                </p>
              </div>
            </div>

          </div>
        </section>

        {/* CTA */}
        <section className="final-cta">
          <div className="final-bg" />

          <div className="container final-content reveal">

            <p className="eyebrow">VILLAGE COLONY</p>

            <h2>
              O mundo não
              <br />
              <em>para quando você sai.</em>
            </h2>

            <p>
              Baixe o mod e transforme sua próxima vila
              em uma colônia viva.
            </p>

            <div className="hero-actions">
              <a href={DOWNLOAD} className="button button-primary">
                ⬇ &nbsp; BAIXAR VILLAGE COLONY
              </a>

              <a
                href={GITHUB}
                target="_blank"
                rel="noreferrer"
                className="button button-secondary"
              >
                VER NO GITHUB ↗
              </a>
            </div>

            <div className="final-version">
              MINECRAFT 1.21.1 · FABRIC · MIT LICENSE
            </div>

          </div>
        </section>

      </main>

      {/* FOOTER */}
      <footer>
        <div className="container footer-inner">

          <div className="brand footer-brand">
            <span className="brand-mark">VC</span>

            <span>
              <strong>VILLAGE</strong>
              <small>COLONY</small>
            </span>
          </div>

          <p>
            Um mod open source para transformar vilas
            Vanilla em colônias vivas.
          </p>

          <a
            href={GITHUB}
            target="_blank"
            rel="noreferrer"
          >
            GITHUB ↗
          </a>

          <span className="copyright">
            © {new Date().getFullYear()} Lucas Riboldi
          </span>

        </div>
      </footer>

    </div>
  );
}

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
