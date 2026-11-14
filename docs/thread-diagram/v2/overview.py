"""v2 클라이언트 전체도. 실행하면 같은 디렉터리에 overview.svg를 만든다.

python docs/thread-diagram/v2/overview.py
"""

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))  # svg.py 위치
from svg import Diagram

d = Diagram(1500, 960)

d.text(40, 46, "클라이언트 스레드 구조", size=26, weight=700, anchor="start")
d.text(1460, 46, "v2  |  #34 GUI 클라이언트 서버 연결", size=14, weight=400, color="#57606a", anchor="end")

# 클라이언트 프로세스
d.rect(30, 80, 1150, 770, "process", r=16)
d.text(54, 104, "CLIENT", size=13, weight=700, color="#57606a", anchor="start")

# main 스레드
d.panel(60, 125, 420, 165, "main", "main 스레드 × 1", "연결까지만 하고 끝남")
d.lines(84, 182, [
    "① LobbyAssetLoader.load()  그림 읽기",
    "② new Client(host, port)  연결 · TCP_NODELAY",
    "③ invokeLater(LobbySession.show())  후 종료",
    "실패하면 대화상자를 띄우고 끝 (스레드 시작 전)",
], size=13, gap=24)

# EDT
d.panel(60, 330, 590, 470, "edt", "EDT × 1  (Swing 화면 스레드)", "로비 상태는 EDT만 바꾼다")
d.rect(90, 380, 530, 120, "thread")
d.text(355, 404, "LobbySession", weight=600)
d.lines(110, 436, [
    "show(): 창 · 타이머 · 방향키 → client.start(콜백) → send(JOIN)",
    "handle(ServerMsg): WELCOME · JOINED를 LobbyState에 반영",
    "windowClosed: timer.stop() → client.close()",
], size=12, gap=20)
d.node(90, 540, 220, 76, "thread", ["Timer", "16ms마다 이동 · 다시 그리기"])
d.node(90, 640, 220, 76, "thread", ["ArrowKeys", "눌린 방향키 모음"])
d.node(350, 540, 270, 76, "loop", ["LobbyState", "접속자 목록 · 내 playerId"])
d.node(350, 640, 270, 76, "thread", ["LobbyPanel", "LobbyState를 읽어 그림"])
d.arrow([(312, 578), (348, 578)], "make")
d.arrow([(200, 638), (200, 618)], "make")
d.arrow([(485, 618), (485, 638)], "make")
d.arrow([(485, 502), (485, 538)], "make")
d.text(345, 756, "Timer · 키 입력 · 서버 메시지 처리가 모두 EDT 한 줄로 돌아서 락이 없다",
       size=12, weight=400, color="#57606a")

# Client: 연결 1개
d.panel(700, 125, 450, 675, "process", "Client × 1  (연결 1개)", "Reader + Writer = 2개 스레드")
d.node(740, 190, 230, 70, "queue", ["sendQueue", "ClientMsg · 크기 제한 없음"])
d.node(740, 310, 230, 90, "thread", ["WriteHandler", "Writer 스레드", "take() → encode → write"])
d.node(740, 520, 230, 100, "thread", ["ReadHandler", "Reader 스레드", "readLine() → decodeServer()"])
d.node(740, 680, 230, 96, "close", ["close()", "여러 번 불려도 한 번만", "소켓 닫기 + Writer interrupt"])
d.node(1030, 290, 80, 360, "socket", ["socket"])

# 서버
d.rect(1230, 330, 230, 200, "process", r=12)
d.text(1345, 380, "SERVER", size=13, weight=700, color="#57606a")
d.text(1345, 418, "서버 :12345", size=16, weight=600)
d.text(1345, 448, "구조는 v1 참고", size=13, weight=400, color="#57606a")

# 생성 · 시작
d.arrow([(482, 205), (698, 205)], "make", "② 연결", (590, 205))
d.arrow([(270, 292), (270, 328)], "make", "③ invokeLater", (330, 310))

# 보내는 길: EDT → sendQueue → Writer → socket → 서버
d.arrow([(622, 410), (690, 410), (690, 225), (738, 225)], "send", "send(JOIN)", (690, 300))
d.arrow([(855, 262), (855, 308)], "send")
d.arrow([(972, 355), (1028, 355)], "send")
d.arrow([(1112, 380), (1228, 380)], "tcp", "TCP", (1170, 380))

# 받는 길: 서버 → socket → Reader → invokeLater → EDT
d.arrow([(1228, 480), (1114, 480)], "tcp", "TCP", (1170, 480))
d.arrow([(1028, 570), (974, 570)], "recv")
d.arrow([(738, 570), (672, 570), (672, 470), (624, 470)], "recv", "invokeLater", (672, 520))

# 종료 경로
d.arrow([(652, 730), (738, 730)], "close", "창 닫기", (695, 730))
d.arrow([(972, 730), (1070, 730), (1070, 652)], "close")

# 책임 구분
d.rect(1200, 570, 280, 230, "note", r=10)
d.text(1220, 598, "책임 구분", size=16, weight=700, anchor="start")
for i, (head, body) in enumerate([
    ("Client", "소켓과 스레드만 안다. Swing은 모름"),
    ("LobbySession", "메시지를 EDT로 넘겨 반영, 창 닫으면 연결 닫기"),
    ("LobbyState", "EDT 전용이라 락 없음"),
    ("main", "연결해서 넘기고 닫지 않음"),
]):
    d.text(1220, 634 + i * 42, head, size=13, weight=700, anchor="start")
    d.text(1220, 654 + i * 42, body, size=11, weight=400, color="#57606a", anchor="start")

d.text(60, 826, "Reader가 화면을 직접 고치지 않는다. 받은 메시지는 invokeLater로 EDT에 넘기고, 보낼 메시지는 EDT가 sendQueue에 넣기만 한다.",
       size=13, weight=400, color="#57606a", anchor="start")

# 범례
for i, (kind, label) in enumerate([("recv", "수신"), ("send", "전송"), ("make", "생성 · 시작 · 호출"), ("close", "종료 경로")]):
    x = 60 + i * 260
    d.arrow([(x, 905), (x + 60, 905)], kind)
    d.text(x + 74, 905, label, size=15, weight=500, anchor="start")
d.text(1460, 905, "서버 내부는 v1 전체도", size=13, weight=400, color="#57606a", anchor="end")

d.save(HERE / "overview.svg")
