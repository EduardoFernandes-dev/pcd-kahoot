# isKahoot (PCD)

Projeto de Programação Concorrente e Distribuída: um clone de Kahoot em JavaFX,
com sessões de "quiz" distribuídas entre clientes e servidor. Foco em
programação concorrente e comunicação entre processos em rede.

## Stack

- Java 17 + JavaFX
- Maven (`pom.xml`)
- Gson (serialização das mensagens de rede)

## Como correr

O projeto funciona em rede: um terminal abre o servidor e cria uma sala; os
participantes entram noutros terminais com o código da sala, equipa e username.

Terminal 1, servidor (cria a sala):

    mvn exec:java -Dexec.mainClass=server.GameServer

Na consola do servidor, criar a sala:

    create ROOM1 2 2      # <código_da_sala> <nº_de_equipas> <jogadores_por_equipa>

Terminal 2, cliente (entra na sala):

    mvn exec:java -Dexec.mainClass=client.ClientMain -Dexec.args="localhost 8080 ROOM1 TEAM_A Alice"

    # argumentos: <IP_do_servidor> <porta> <sala> <equipa> <username>

Modo autónomo (sem servidor), para testar o quiz sozinho:

    mvn clean javafx:run

## Padrões de concorrência

- `CustomBarrier`, `CustomSemaphore` e `ModifiedCountdownLatch`, implementações
  próprias dos mecanismos de sincronização usados para coordenar jogadores,
  questões e pontuações entre os clientes e o servidor.

## Estrutura

- `src/client/`, interface JavaFX e lógica do cliente
- `src/server/`, servidor, gestão de salas e pontuação
- `src/coordination/`, mecanismos de sincronização (barrier, semáforo, countdown latch)
- `src/network/`, mensagens trocadas entre cliente e servidor
- `src/models/`, modelos de jogo (questões, equipas, jogadores, pontuações)
- `lib/` e `resources/`, dependências e recursos
- `pom.xml`, configuração Maven
