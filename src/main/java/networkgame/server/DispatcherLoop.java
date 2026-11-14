package networkgame.server;

import networkgame.protocol.ClientMsg;
import networkgame.protocol.PlayerInfo;
import networkgame.protocol.ServerMsg;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** 한 스레드에서 로비 요청을 처리하고 세션별 응답을 보낸다. */
public final class DispatcherLoop implements Runnable {
    private static final long TICK_MILLIS = 33;

    private sealed interface Event {
        record Received(BlockingQueue<ServerMsg> sendQueue, ClientMsg message) implements Event {}
        record Leave(BlockingQueue<ServerMsg> sendQueue) implements Event {}
    }

    private final BlockingQueue<Event> receiveQueue = new LinkedBlockingQueue<>();
    private final SessionManager sessions = new SessionManager();

    public void receive(BlockingQueue<ServerMsg> sendQueue, ClientMsg message) {
        receiveQueue.add(new Event.Received(sendQueue, message));
    }

    public void leave(BlockingQueue<ServerMsg> sendQueue) {
        receiveQueue.add(new Event.Leave(sendQueue));
    }

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
        while ((event = receiveQueue.poll()) != null) {
            switch (event) {
                case Event.Received(var sendQueue, var message) -> handle(sendQueue, message);
                case Event.Leave(var sendQueue) -> sessions.remove(sendQueue);
            }
        }
    }

    private void handle(BlockingQueue<ServerMsg> sendQueue, ClientMsg message) {
        switch (message) {
            case ClientMsg.Join join -> join(sendQueue, join);
            case ClientMsg.Move ignored -> {}
            case ClientMsg.Ready ignored -> {}
            case ClientMsg.Chat ignored -> {}
            case ClientMsg.Pong ignored -> {}
        }
    }

    private void join(BlockingQueue<ServerMsg> sendQueue, ClientMsg.Join message) {
        PlayerInfo player = sessions.register(sendQueue, message.name(), message.character());
        if (player == null) {
            return;
        }

        sendQueue.add(new ServerMsg.Welcome(player.id(), sessions.players()));
        var joined = new ServerMsg.Joined(player);
        for (var recipient : sessions.sendQueues()) {
            if (recipient != sendQueue) {
                recipient.add(joined);
            }
        }
    }
}
