"""v1 전체도. 실행하면 같은 디렉터리에 overview.svg를 만든다.

python docs/thread-diagram/v1/overview.py
"""

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))  # svg.py 위치
from svg import Diagram

d = Diagram(1000, 660)

# 제목
d.node(60, 24, 160, 40, "title", ["Client A"])
d.node(560, 24, 300, 40, "title", ["Server  100.x.x.x:12345"])

# 클라이언트 A
d.rect(20, 84, 240, 300, "process", r=12)
d.node(50, 104, 180, 44, "thread", ["Input"])
d.node(50, 172, 180, 44, "queue", ["sendQueue"])
d.node(50, 242, 180, 44, "thread", ["Writer"])
d.node(50, 312, 180, 44, "thread", ["Reader", "출력"])
d.arrow([(140, 148), (140, 170)], "send")
d.arrow([(140, 216), (140, 240)], "send")

# 클라이언트 B
d.rect(20, 440, 240, 160, "process", r=12)
d.text(140, 520, "Client B", weight=600)
d.text(140, 542, "(A와 같은 구조)", size=13, weight=400, color="#57606a")

# 서버
d.rect(380, 84, 600, 556, "process", r=12)
d.node(790, 104, 170, 44, "thread", ["Acceptor"])
d.text(875, 166, "접속마다 Reader·Writer 생성", size=12, weight=400, color="#57606a")
d.node(600, 170, 180, 56, "queue", ["receiveQueue", "(from, msg)"])
d.node(600, 296, 180, 76, "loop", ["GameLoop ×1", "20~30Hz", "세션 목록 소유"])

# 서버 테두리에 걸친 세션 스레드.
# 받는 길과 보내는 길이 교차하지 않게 A는 Reader/Writer, B는 Writer/Reader 순서
# Writer는 자기 sendQueue를 가지므로 노란 박스에 sendQueue를 함께 적는다
for y, kind, label in ((240, "thread", "Reader A"), (310, "queue", "Writer A"),
                       (460, "queue", "Writer B"), (530, "thread", "Reader B")):
    d.rect(320, y, 170, 48, kind)
    if kind == "queue":
        d.text(440, y + 17, label, weight=600)
        d.text(440, y + 35, "+ sendQueue", size=12, weight=400, color="#57606a")
    else:
        d.text(440, y + 24, label, weight=600)
d.line(380, 96, 380, 628)  # 테두리를 박스 위로 다시 그어 "걸친" 모양을 만든다

# TCP
d.arrow([(230, 264), (318, 264)], "tcp", "TCP", (275, 264))
d.arrow([(320, 334), (232, 334)], "tcp", "TCP", (275, 334))
d.arrow([(320, 484), (262, 484)], "tcp", "TCP", (290, 484))
d.arrow([(260, 554), (318, 554)], "tcp", "TCP", (290, 554))

# 받는 길: Reader -> receiveQueue
d.arrow([(490, 264), (540, 264), (540, 198), (598, 198)], "recv")
d.arrow([(490, 554), (940, 554), (940, 198), (782, 198)], "recv")
# 큐 -> GameLoop
d.arrow([(690, 226), (690, 294)], "loop")
# 보내는 길: GameLoop -> sendQueue -> Writer
d.arrow([(600, 334), (492, 334)], "send")
d.arrow([(600, 356), (560, 356), (560, 484), (492, 484)], "send")

d.save(HERE / "overview.svg")
