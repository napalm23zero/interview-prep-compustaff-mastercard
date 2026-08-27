# Debian 13 "trixie" = current stable. slim = no docs/locales.
FROM debian:trixie-slim

ENV DEBIAN_FRONTEND=noninteractive
# UTF-8 sem instalar o pacote `locales`: evita mojibake na saída do Maven/npm
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# --- toolchain do desafio ---------------------------------------------------
# headless JDK: sem AWT/Swing, ~200MB mais leve que o jdk completo
RUN apt-get update && apt-get install -y --no-install-recommends \
      openjdk-21-jdk-headless \
      maven \
      nodejs \
      npm \
# --- diagnóstico: sem isto você fica cego quando a porta 8080 travar --------
      procps \
      lsof \
      iproute2 \
# --- exploração do repo: os 5 primeiros minutos do teste são recon ----------
      tree \
      ripgrep \
      file \
      less \
# --- manipulação de dados e arquivos ---------------------------------------
      jq \
      unzip \
      zip \
      make \
      time \
# --- banco: inspecionar o Postgres sem sair do terminal ---------------------
      postgresql-client \
# --- edição no terminal quando o editor do browser não estiver à mão --------
      vim-tiny \
      nano \
# --- base -------------------------------------------------------------------
      git \
      curl \
      ca-certificates \
      bash-completion \
 && rm -rf /var/lib/apt/lists/*

# non-root, UID 1000 para casar com o usuário do host em bind mounts
RUN useradd --create-home --shell /bin/bash --uid 1000 dev

# ~/.m2 e ~/.npm como diretórios próprios do dev: os volumes nomeados do
# devcontainer.json montam aqui e preservam o cache entre rebuilds
RUN mkdir -p /home/dev/.m2 /home/dev/.npm && chown -R dev:dev /home/dev

USER dev
WORKDIR /workspace

# atalhos que economizam digitação sob cronômetro
RUN printf '%s\n' \
      "alias ll='ls -alF'" \
      "alias mvnt='mvn -q test'" \
      "alias mvnr='mvn spring-boot:run'" \
      "alias ports='ss -ltnp'" \
      "export PATH=\$PATH:/workspace/backend" \
      >> /home/dev/.bashrc
