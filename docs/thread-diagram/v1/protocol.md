# v1 확대도: 주고받는 순서

> [전체도](README.md)의 TCP 화살표와 서버 안 스레드를 시간 순서로 펼친 것.

- 실선 = TCP (네트워크), 점선 = 서버 안 큐 (스레드끼리 전달), 끝이 ✕ = 끊긴 연결
- `Writer A / B`는 세션마다 있는 Writer 스레드(와 그 outbox)를 한 칸으로 합친 것

## 입장 · 채팅 · 핑

```mermaid
sequenceDiagram
    participant A as Client A
    box Server
        participant RA as Reader A
        participant GL as GameLoop
        participant SQ as Writer A / B
    end
    participant B as Client B (id=0)

    A->>RA: JOIN name=Kim
    RA-->>GL: inbox (A, JOIN)
    Note over GL: id=1 부여, 세션 목록에 추가
    GL-->>SQ: A outbox에 WELCOME, B outbox에 JOINED
    SQ->>A: WELCOME id=1, players
    SQ->>B: JOINED id=1 Kim

    A->>RA: CHAT "hi"
    RA-->>GL: inbox (from=1, CHAT)
    GL-->>SQ: 모든 outbox에 CHAT from=1
    SQ->>A: CHAT from=1 "hi"
    SQ->>B: CHAT from=1 "hi"

    loop 3초마다
        GL-->>SQ: A outbox에 PING t
        SQ->>A: PING t
        A->>RA: PONG t
        RA-->>GL: inbox (from=1, PONG)
        Note over GL: rtt = now - t, 마지막 PONG 시각 기록
    end
```

- **입장**: 접속 직후 `JOIN`. GameLoop가 id를 주고 세션 목록에 넣은 뒤, 본인에게 `WELCOME`(내 id, 현재 접속자), 나머지에게 `JOINED`. 목록에 들어가기 전에는 broadcast를 받지 않음
- **거절**: 4명이 찼거나 닉네임이 겹치면 `REJECT` 후 연결 종료
- **채팅**: 보낸 사람(`from`)은 Reader 스레드가 붙임. 내가 친 것도 바로 출력하지 않고 **서버가 돌려준 `CHAT`을 출력**. 그래야 모두 같은 순서로 봄
- **핑**: GameLoop가 3초마다 `PING t`(서버 시각). 돌아온 `PONG t`로 왕복 시간을 계산하고 마지막 PONG 시각을 기록

## 나감

```mermaid
sequenceDiagram
    participant A as Client A
    box Server
        participant RA as Reader A
        participant GL as GameLoop
        participant SQ as Writer A / B
    end
    participant B as Client B (id=0)

    alt 정상 종료 (창 닫기, Ctrl+C)
        A-xRA: 연결 닫힘
        Note over RA: readLine() = null
        RA-->>GL: inbox LEAVE 1
        Note over GL: 세션 목록에서 1 제거
        GL-->>SQ: B outbox에 LEFT, A outbox에 종료 신호
        SQ->>B: LEFT id=1
        Note over SQ: Writer A 종료, 소켓 닫음
    else 응답 없음 (절전, 네트워크 끊김)
        SQ-xA: PING t (PONG 안 옴)
        Note over GL: 10초 동안 PONG 없음 → 1 제거, 소켓 닫음
        GL-->>SQ: B outbox에 LEFT
        SQ->>B: LEFT id=1
        Note over RA: 소켓이 닫혀 예외로 깨어남
        RA-->>GL: inbox LEAVE 1
        Note over GL: 이미 목록에 없음 → 무시
    end
```

- **응답 없음**은 노트북 절전, Tailscale 끊김처럼 연결이 닫혔다는 신호가 안 오는 경우. 이때 `readLine()`은 끝없이 기다리므로 PING으로 판정
- **나감은 두 번 올 수 있다**는 전제로 처리 (GameLoop가 끊은 뒤 Reader A도 LEAVE를 넣음)

## 메시지 목록

| 방향 | 메시지 | 내용 |
|---|---|---|
| 클라 → 서버 | `JOIN` | name |
| | `CHAT` | text |
| | `PONG` | t (받은 PING 값 그대로) |
| 서버 → 클라 | `WELCOME` | id, players (id·name 목록) |
| | `REJECT` | reason |
| | `JOINED` | id, name |
| | `LEFT` | id |
| | `CHAT` | from, text |
| | `PING` | t |
| 서버 내부 (`inbox`만) | `LEAVE` | id. 네트워크로 오가지 않음 |

## 정한 숫자 (기준안)

| 항목 | 값 |
|---|---|
| PING 주기 | 3초 |
| 응답 없음 판정 | 10초 |
| 최대 인원 | 4명 |
| 채팅 길이 | 200자 (`readLine()`은 줄 끝까지 무제한으로 읽으므로 서버에서 자름) |
