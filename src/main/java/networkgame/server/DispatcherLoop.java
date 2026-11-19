package networkgame.server;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.PlayerInfo;
import networkgame.protocol.ServerMsg;

import networkgame.server.SessionManager.Event;

/** 한 스레드에서 로비 요청을 처리하고 세션별 응답을 보낸다. */
public final class DispatcherLoop implements Runnable {
    private static final long TICK_MILLIS = 33;

    private final PlayerManager playerManager = new PlayerManager();
    private final SessionManager sessionManager;

    /** 서버와 연결 목록을 공유하는 요청 처리 루프를 구성한다. */
    DispatcherLoop(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * 요청을 처리한 뒤 33ms를 기다리는 작업을 인터럽트될 때까지 반복한다.
     * DispatcherLoop 인스턴스당 하나의 스레드에서 실행한다.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                tick();
                Thread.sleep(TICK_MILLIS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 큐에 쌓인 요청을 순서대로 처리한다. DispatcherLoop를 담당하는 스레드만 호출한다. */
    void tick() {
        Event event;
        while ((event = sessionManager.pollEvent()) != null) {
            switch (event) {
                case Event.Received(var sessionId, var message) -> handle(sessionId, message);
                case Event.Leave(var sessionId) -> {
                    playerManager.remove(sessionId);
                    sessionManager.remove(sessionId);
                }
            }
        }
    }

    /** 현재 지원하는 입장 메시지를 처리한다. 다른 메시지는 후속 기능에서 처리한다. */
    private void handle(long sessionId, ClientMsg message) {
        switch (message) {
            case ClientMsg.Join join -> join(sessionId, join);
            case ClientMsg.Move ignored -> {}
            case ClientMsg.Ready ignored -> {}
            case ClientMsg.Chat ignored -> {}
            case ClientMsg.Pong ignored -> {}
        }
    }

    /** 입장에 성공하면 본인에게 전체 목록을 보내고 기존 입장자에게 새 플레이어를 알린다. */
    private void join(long sessionId, ClientMsg.Join message) {
        PlayerInfo player = playerManager.register(sessionId, message.name(), message.character());
        if (player == null) {
            return;
        }

        sessionManager.send(sessionId, new ServerMsg.Welcome(player.id(), playerManager.players()));
        var joined = new ServerMsg.Joined(player);
        for (long recipient : playerManager.sessionIds()) {
            if (recipient != sessionId) {
                sessionManager.send(recipient, joined);
            }
        }
    }
}
