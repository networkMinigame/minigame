"""v2 클라이언트 상세도. 실행하면 같은 디렉터리에 detail.svg를 만든다.

python docs/thread-diagram/v2/detail.py
"""

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))  # svg.py 위치
from svg import Diagram

d = Diagram(1500, 1070)


def chip(x, y, w, h, kind, label):
    """한 줄짜리 작은 박스. node()의 굵은 15px 글씨는 좁은 박스에서 넘친다."""
    d.rect(x, y, w, h, kind)
    d.text(x + w / 2, y + h / 2, label, size=12, weight=500)


d.text(40, 46, "클라이언트 메시지 흐름 상세", size=26, weight=700, anchor="start")
d.text(1460, 46, "v2  |  받기 · 보내기 · 끝내기", size=14, weight=400, color="#57606a", anchor="end")

# ① 받기
d.panel(30, 80, 1440, 360, "process", "① 받기: 서버가 보낸 한 줄이 화면에 그려지기까지", "Reader는 화면을 고치지 않는다")
d.node(50, 145, 130, 60, "thread", ["Reader 스레드"])
d.node(50, 315, 130, 60, "edt", ["EDT"])

d.node(200, 135, 180, 80, "thread", ["readLine()", "한 줄 읽기"])
d.node(420, 135, 180, 80, "thread", ["빈 줄?", "그러면 건너뜀"])
d.node(640, 135, 200, 80, "thread", ["decodeServer(line)", "JSON → ServerMsg"])
d.node(880, 135, 200, 80, "thread", ["onMessage.accept()", "LobbySession이 준 콜백"])
d.node(1120, 135, 200, 80, "queue", ["invokeLater", "EDT 이벤트 큐에 넣기"])
chip(620, 226, 240, 30, "close", "JacksonException → 로그만 남기고 다음 줄")
for x1, x2 in ((382, 418), (602, 638), (842, 878), (1082, 1118)):
    d.arrow([(x1, 175), (x2, 175)], "recv")

d.line(190, 276, 1450, 276, color="#d0d7de")
d.text(1450, 266, "여기서 스레드가 바뀐다", size=12, weight=400, color="#57606a", anchor="end")
d.arrow([(1220, 217), (1220, 298)], "recv", "EDT에서 실행", (1220, 250))

d.node(1140, 300, 180, 100, "thread", ["handle(message)", "sealed switch", "9개 case"])
chip(860, 290, 250, 34, "thread", "WELCOME → state.welcome(id, 목록)")
chip(860, 333, 250, 34, "thread", "JOINED → state.save(Player.from(p))")
chip(860, 376, 250, 34, "note", "그 외 7개 → 지금은 무시 (#35~#38)")
for y in (307, 350, 393):
    d.arrow([(1138, y), (1112, y)], "recv")
d.node(620, 310, 200, 80, "loop", ["LobbyState", "접속자 · 내 playerId"])
d.arrow([(858, 307), (840, 307), (840, 335), (822, 335)], "recv")
d.arrow([(858, 350), (822, 350)], "recv")

d.node(380, 310, 200, 80, "thread", ["Timer 16ms", "moveLocalPlayer()"])
d.arrow([(582, 350), (618, 350)], "make")
d.node(200, 310, 140, 80, "thread", ["LobbyPanel", "다시 그림"])
d.arrow([(378, 350), (342, 350)], "make")
d.text(480, 414, "WELCOME 전에는 내 플레이어가 없어서 이동을 건너뜀", size=12, weight=400, color="#57606a")

# ② 보내기
d.panel(30, 460, 1440, 170, "process", "② 보내기: 입장할 때 JOIN 한 번", "EDT는 소켓에 직접 쓰지 않는다")
d.node(50, 515, 130, 60, "edt", ["EDT"])
d.node(200, 510, 220, 70, "thread", ["show() 끝", "client.start(콜백) 다음"])
d.node(460, 510, 220, 70, "thread", ["client.send(join)", "큐에 넣고 바로 돌아옴"])
d.node(720, 510, 170, 70, "queue", ["sendQueue", "LinkedBlockingQueue"])
d.node(930, 510, 240, 70, "thread", ["Writer 스레드: take()", "비어 있으면 잠들어 기다림"])
d.node(1210, 510, 230, 70, "thread", ["encode → write", "newLine · flush"])
for x1, x2 in ((422, 458), (682, 718), (1172, 1208)):
    d.arrow([(x1, 545), (x2, 545)], "send")
d.arrow([(892, 545), (928, 545)], "send")
d.text(60, 606, "add()는 크기 제한이 없는 큐라 막히지 않는다. 기다리는 쪽은 Writer의 take()뿐이다.",
       size=12, weight=400, color="#57606a", anchor="start")

# ③ 끝내기
d.panel(30, 650, 1440, 340, "process", "③ 끝내기: 누가 먼저 부르든 close()는 한 번", "스레드 3개가 같은 close()를 부를 수 있다")
d.node(60, 700, 300, 64, "edt", ["창 닫기", "EDT windowClosed: timer.stop() 다음"])
d.node(60, 780, 300, 64, "thread", ["서버가 끊음", "Reader: readLine() 예외나 null → finally"])
d.node(60, 860, 300, 64, "thread", ["쓰기 실패", "Writer: write 예외 → finally"])
d.node(430, 760, 240, 100, "close", ["close()", "compareAndSet으로 한 번만", "두 번째부터는 그냥 리턴"])
d.arrow([(362, 732), (395, 732), (395, 790), (428, 790)], "close")
d.arrow([(362, 812), (428, 812)], "close")
d.arrow([(362, 892), (395, 892), (395, 835), (428, 835)], "close")

d.node(740, 720, 240, 64, "close", ["socket.close()", "Reader를 깨움"])
d.node(740, 820, 240, 64, "close", ["Writer interrupt()", "take()는 소켓으로 안 깨서"])
d.arrow([(672, 790), (705, 790), (705, 752), (738, 752)], "close")
d.arrow([(672, 830), (705, 830), (705, 852), (738, 852)], "close")

d.node(1050, 720, 290, 64, "thread", ["Reader 끝", "readLine()에서 SocketException"])
d.node(1050, 820, 290, 64, "thread", ["Writer 끝", "take()에서 InterruptedException"])
d.arrow([(982, 752), (1048, 752)], "close")
d.arrow([(982, 852), (1048, 852)], "close")
chip(740, 906, 600, 40, "note", "창이 닫히고 Reader · Writer가 끝나면 남은 스레드가 없어 프로세스 종료")

d.text(60, 962, "연결 실패는 여기로 오지 않는다. new Client()가 예외 → main이 '서버 연결 실패' 대화상자 → 스레드 시작 전이라 그대로 종료",
       size=12, weight=400, color="#57606a", anchor="start")

# 범례
for i, (kind, label) in enumerate([("recv", "수신"), ("send", "전송"), ("make", "같은 스레드 안 호출"), ("close", "종료 경로")]):
    x = 60 + i * 260
    d.arrow([(x, 1030), (x + 60, 1030)], kind)
    d.text(x + 74, 1030, label, size=15, weight=500, anchor="start")
d.text(1460, 1030, "스레드 배치는 overview.svg", size=13, weight=400, color="#57606a", anchor="end")

d.save(HERE / "detail.svg")
