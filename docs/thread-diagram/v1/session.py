"""v1 확대도. python docs/thread-diagram/v1/session.py 로 session.svg를 갱신한다."""

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
from svg import Diagram

d = Diagram(1000, 510)

d.node(40, 24, 160, 40, "title", ["Client"])
d.node(560, 24, 300, 40, "title", ["Server  :12345"])
d.rect(20, 84, 200, 400, "process", r=12)
d.text(120, 272, "Client", weight=600)
d.text(120, 294, "(전체도의 Client A)", size=13, weight=400, color="#57606a")
d.rect(340, 84, 640, 400, "process", r=12)

# 접속별 Session과 스레드
d.out.append('<rect x="270" y="110" width="440" height="340" rx="10" fill="#ffffff" '
             'stroke="#57606a" stroke-width="1.4" stroke-dasharray="6 4"/>')
d.text(525, 134, "Session", weight=600)
d.text(525, 156, "close()는 CAS로 한 번만 실행", size=12, weight=400, color="#57606a")
d.node(290, 190, 200, 56, "thread", ["Reader", "readLine() · decodeClient()"])
d.node(290, 330, 200, 56, "thread", ["Writer", "JSON 한 줄 전송"])
d.node(530, 330, 160, 56, "queue", ["sendQueue", "ServerMsg"])
d.line(340, 96, 340, 472)

# 서버 공용 큐와 요청 처리 루프
d.node(765, 178, 200, 80, "queue", ["receiveQueue", "Received(sendQueue, msg)", "Leave(sendQueue)"])
d.node(765, 317, 200, 82, "loop", ["DispatcherLoop", "JOIN · LEAVE 처리", "SessionManager로 목록 관리"])

d.arrow([(220, 218), (288, 218)], "tcp", "TCP", (254, 218))
d.arrow([(290, 358), (222, 358)], "tcp", "TCP", (256, 358))
d.arrow([(490, 218), (763, 218)], "recv")
d.text(623, 201, "수신 메시지 · 연결 종료", size=12, weight=400, color="#57606a")
d.arrow([(865, 258), (865, 315)], "loop")
d.text(881, 285, "poll()", size=12, weight=400, color="#57606a", anchor="start")
d.arrow([(765, 358), (692, 358)], "send")
d.arrow([(530, 358), (492, 358)], "send")
d.text(510, 312, "take()", size=11, weight=400, color="#57606a")

d.save(HERE / "session.svg")
